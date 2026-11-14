package app.crowdhub.feature.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.db.StockWatchEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.provider.torn.TornStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StockSort(val label: String) {
    NAME("Name"),
    PRICE_DESC("Price"),
    CHANGE_24H("24h %")
}

sealed class StocksUiState {
    data object Loading : StocksUiState()
    data class Content(
        val items: List<StockListItem>,
        val fetchedAtMs: Long,
        val isStale: Boolean,
        val staleReason: String?
    ) : StocksUiState()
    data class Empty(val message: String) : StocksUiState()
    data class Error(val error: ApiError, val accountId: String?) : StocksUiState()
}

class StocksViewModel(
    private val profileRepository: ProfileRepository,
    private val stockRepository: StockRepository,
    private val alertEngine: AlertEngine,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<StocksUiState>(StocksUiState.Loading)
    val uiState: StateFlow<StocksUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(StockSort.NAME)
    val sortMode: StateFlow<StockSort> = _sortMode.asStateFlow()

    private val _watches = MutableStateFlow<List<StockWatchEntity>>(emptyList())
    val watches: StateFlow<List<StockWatchEntity>> = _watches.asStateFlow()

    private val _activeProfile = MutableStateFlow(ActiveProfileState(null, null, emptyList()))
    val activeProfile: StateFlow<ActiveProfileState> = _activeProfile.asStateFlow()

    private var rawStocks: List<TornStock> = emptyList()
    private var sparklines: Map<Int, List<StockPricePoint>> = emptyMap()
    private var lastFetchedAtMs: Long = 0L
    private var lastIsStale: Boolean = false
    private var lastStaleReason: String? = null

    init {
        viewModelScope.launch {
            profileRepository.observeActiveState().collect { state ->
                val prevId = _activeProfile.value.primaryAccount?.id
                _activeProfile.value = state
                val acct = state.primaryAccount
                if (acct != null && acct.id != prevId) {
                    launch {
                        stockRepository.observeWatches(acct.id).collect { w ->
                            _watches.value = w
                            rebuildContent()
                        }
                    }
                    refresh(forceRefresh = false)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        rebuildContent()
    }

    fun setSortMode(sort: StockSort) {
        _sortMode.value = sort
        rebuildContent()
    }

    fun refresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = _activeProfile.value
            val acct = state.primaryAccount ?: return@launch
            if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                return@launch
            }
            if (rawStocks.isEmpty()) {
                _uiState.value = StocksUiState.Loading
            }

            val res = stockRepository.fetchStocksAndRecord(
                accountId = acct.id,
                priority = if (forceRefresh) RequestPriority.USER else RequestPriority.WATCH,
                forceRefresh = forceRefresh
            )
            sparklines = stockRepository.getAllRecent24hSnapshots()

            when (res) {
                is ApiResult.Success -> {
                    rawStocks = res.data
                    lastFetchedAtMs = res.fetchedAtMs
                    lastIsStale = false
                    lastStaleReason = null
                    rebuildContent()
                    alertEngine.runCycle(AlertTriggerMode.FOREGROUND)
                }
                is ApiResult.Failure -> {
                    val stale = res.staleData
                    if (!stale.isNullOrEmpty()) {
                        rawStocks = stale
                        lastFetchedAtMs = res.staleFetchedAtMs ?: clock.nowEpochMs()
                        lastIsStale = true
                        lastStaleReason = res.error.message
                        rebuildContent()
                    } else if (rawStocks.isNotEmpty()) {
                        lastIsStale = true
                        lastStaleReason = res.error.message
                        rebuildContent()
                    } else {
                        _uiState.value = StocksUiState.Error(res.error, acct.id)
                    }
                }
            }
        }
    }

    fun findStock(stockId: Int): TornStock? = rawStocks.firstOrNull { it.stockId == stockId }

    suspend fun loadHistory(stockId: Int, range: ChartRange): List<StockPricePoint> =
        stockRepository.getHistory(stockId, range)

    fun saveWatch(
        id: String?,
        stockId: Int,
        aboveTarget: Double?,
        belowTarget: Double?,
        pctMove: Double?,
        pctWindowMin: Int?,
        repeating: Boolean,
        cooldownMin: Int
    ) {
        val acctId = _activeProfile.value.primaryAccount?.id ?: return
        viewModelScope.launch {
            stockRepository.saveWatch(
                id = id,
                accountId = acctId,
                stockId = stockId,
                aboveTarget = aboveTarget,
                belowTarget = belowTarget,
                pctMove = pctMove,
                pctWindowMin = pctWindowMin,
                repeating = repeating,
                cooldownMin = cooldownMin,
                enabled = true
            )
        }
    }

    fun toggleWatch(watch: StockWatchEntity, enabled: Boolean) {
        viewModelScope.launch { stockRepository.toggleWatch(watch, enabled) }
    }

    fun deleteWatch(id: String) {
        viewModelScope.launch { stockRepository.deleteWatch(id) }
    }

    private fun rebuildContent() {
        if (rawStocks.isEmpty()) return
        val watchStockIds = _watches.value.filter { it.enabled }.map { it.stockId }.toSet()
        val q = _searchQuery.value.trim().lowercase()

        val mapped = rawStocks
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.acronym.lowercase().contains(q) }
            .map { stock ->
                val series = sparklines[stock.stockId].orEmpty()
                val pct = if (series.size >= 2 && series.first().price > 0.0) {
                    ((series.last().price - series.first().price) / series.first().price) * 100.0
                } else {
                    null
                }
                StockListItem(
                    stock = stock,
                    changePct24h = pct,
                    sparkline = series,
                    hasWatch = stock.stockId in watchStockIds
                )
            }

        val sorted = when (_sortMode.value) {
            StockSort.NAME -> mapped.sortedBy { it.stock.acronym }
            StockSort.PRICE_DESC -> mapped.sortedByDescending { it.stock.currentPrice }
            StockSort.CHANGE_24H -> mapped.sortedByDescending { it.changePct24h ?: Double.NEGATIVE_INFINITY }
        }

        _uiState.value = if (sorted.isEmpty()) {
            StocksUiState.Empty("No matching stocks found.")
        } else {
            StocksUiState.Content(
                items = sorted,
                fetchedAtMs = lastFetchedAtMs,
                isStale = lastIsStale,
                staleReason = lastStaleReason
            )
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val stockRepository: StockRepository,
        private val alertEngine: AlertEngine,
        private val clock: Clock
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StocksViewModel(profileRepository, stockRepository, alertEngine, clock) as T
        }
    }
}
