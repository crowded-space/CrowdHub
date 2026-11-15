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
import app.crowdhub.provider.torn.TornUserStock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StockTab(val label: String) {
    MARKET("Market"),
    PORTFOLIO("Portfolio")
}

enum class StockSort(val label: String) {
    NAME("Name"),
    PRICE_DESC("Price"),
    CHANGE_24H("24h %")
}

enum class PortfolioSort(val label: String) {
    VALUE_DESC("Value"),
    PNL_DESC("P&L $"),
    PNL_PCT_DESC("P&L %"),
    NAME("Name")
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

sealed class PortfolioUiState {
    data object Loading : PortfolioUiState()
    data class Content(
        val summary: PortfolioSummary,
        val visiblePositions: List<OwnedStockPosition>,
        val fetchedAtMs: Long,
        val isStale: Boolean,
        val staleReason: String?
    ) : PortfolioUiState()
    data class Empty(val message: String) : PortfolioUiState()
    data class Error(val error: ApiError, val accountId: String?) : PortfolioUiState()
}

class StocksViewModel(
    private val profileRepository: ProfileRepository,
    private val stockRepository: StockRepository,
    private val alertEngine: AlertEngine,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<StocksUiState>(StocksUiState.Loading)
    val uiState: StateFlow<StocksUiState> = _uiState.asStateFlow()

    private val _portfolioState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val portfolioState: StateFlow<PortfolioUiState> = _portfolioState.asStateFlow()

    private val _selectedTab = MutableStateFlow(StockTab.MARKET)
    val selectedTab: StateFlow<StockTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(StockSort.NAME)
    val sortMode: StateFlow<StockSort> = _sortMode.asStateFlow()

    private val _ownedOnlyFilter = MutableStateFlow(false)
    val ownedOnlyFilter: StateFlow<Boolean> = _ownedOnlyFilter.asStateFlow()

    private val _portfolioSort = MutableStateFlow(PortfolioSort.VALUE_DESC)
    val portfolioSort: StateFlow<PortfolioSort> = _portfolioSort.asStateFlow()

    private val _watches = MutableStateFlow<List<StockWatchEntity>>(emptyList())
    val watches: StateFlow<List<StockWatchEntity>> = _watches.asStateFlow()

    private val _activeProfile = MutableStateFlow(ActiveProfileState(null, null, emptyList()))
    val activeProfile: StateFlow<ActiveProfileState> = _activeProfile.asStateFlow()

    private var rawStocks: List<TornStock> = emptyList()
    private var rawHoldings: List<TornUserStock> = emptyList()
    private var hasLoadedHoldings: Boolean = false
    private var sparklines: Map<Int, List<StockPricePoint>> = emptyMap()
    private var lastFetchedAtMs: Long = 0L
    private var lastIsStale: Boolean = false
    private var lastStaleReason: String? = null
    private var lastPortfolioFetchedAtMs: Long = 0L
    private var lastPortfolioIsStale: Boolean = false
    private var lastPortfolioStaleReason: String? = null
    private var lastPortfolioError: ApiError? = null

    init {
        viewModelScope.launch {
            profileRepository.observeActiveState().collect { state ->
                val prevId = _activeProfile.value.primaryAccount?.id
                val prevProfileId = _activeProfile.value.profile?.id
                _activeProfile.value = state
                val acct = state.primaryAccount
                if (acct != null && (acct.id != prevId || state.profile?.id != prevProfileId)) {
                    rawStocks = emptyList()
                    rawHoldings = emptyList()
                    hasLoadedHoldings = false
                    sparklines = emptyMap()
                    lastPortfolioError = null
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

    fun setSelectedTab(tab: StockTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        rebuildContent()
    }

    fun setSortMode(sort: StockSort) {
        _sortMode.value = sort
        rebuildContent()
    }

    fun setOwnedOnlyFilter(ownedOnly: Boolean) {
        _ownedOnlyFilter.value = ownedOnly
        rebuildContent()
    }

    fun setPortfolioSort(sort: PortfolioSort) {
        _portfolioSort.value = sort
        rebuildContent()
    }

    fun refresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = _activeProfile.value
            val acct = state.primaryAccount ?: return@launch
            val profileId = state.profile?.id ?: "default"
            if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                return@launch
            }
            if (rawStocks.isEmpty()) {
                _uiState.value = StocksUiState.Loading
            }
            if (!hasLoadedHoldings && lastPortfolioError == null) {
                _portfolioState.value = PortfolioUiState.Loading
            }

            val priority = if (forceRefresh) RequestPriority.USER else RequestPriority.WATCH
            val (marketRes, holdingsRes) = coroutineScope {
                val marketDeferred = async {
                    stockRepository.fetchStocksAndRecord(
                        accountId = acct.id,
                        priority = priority,
                        forceRefresh = forceRefresh
                    )
                }
                val holdingsDeferred = async {
                    stockRepository.fetchUserStocks(
                        accountId = acct.id,
                        priority = priority,
                        forceRefresh = forceRefresh
                    )
                }
                marketDeferred.await() to holdingsDeferred.await()
            }
            sparklines = stockRepository.getAllRecent24hSnapshots(profileId)

            when (holdingsRes) {
                is ApiResult.Success -> {
                    rawHoldings = holdingsRes.data
                    hasLoadedHoldings = true
                    lastPortfolioFetchedAtMs = holdingsRes.fetchedAtMs
                    lastPortfolioIsStale = false
                    lastPortfolioStaleReason = null
                    lastPortfolioError = null
                }
                is ApiResult.Failure -> {
                    val staleHoldings = holdingsRes.staleData
                    if (staleHoldings != null) {
                        rawHoldings = staleHoldings
                        hasLoadedHoldings = true
                        lastPortfolioFetchedAtMs = holdingsRes.staleFetchedAtMs ?: clock.nowEpochMs()
                        lastPortfolioIsStale = true
                        lastPortfolioStaleReason = holdingsRes.error.message
                        lastPortfolioError = null
                    } else if (hasLoadedHoldings) {
                        lastPortfolioIsStale = true
                        lastPortfolioStaleReason = holdingsRes.error.message
                        lastPortfolioError = null
                    } else {
                        lastPortfolioError = holdingsRes.error
                    }
                }
            }

            when (marketRes) {
                is ApiResult.Success -> {
                    rawStocks = marketRes.data
                    lastFetchedAtMs = marketRes.fetchedAtMs
                    lastIsStale = false
                    lastStaleReason = null
                    rebuildContent()
                    alertEngine.runCycle(AlertTriggerMode.FOREGROUND)
                }
                is ApiResult.Failure -> {
                    val stale = marketRes.staleData
                    if (!stale.isNullOrEmpty()) {
                        rawStocks = stale
                        lastFetchedAtMs = marketRes.staleFetchedAtMs ?: clock.nowEpochMs()
                        lastIsStale = true
                        lastStaleReason = marketRes.error.message
                        rebuildContent()
                    } else if (rawStocks.isNotEmpty()) {
                        lastIsStale = true
                        lastStaleReason = marketRes.error.message
                        rebuildContent()
                    } else {
                        _uiState.value = StocksUiState.Error(marketRes.error, acct.id)
                        _portfolioState.value = PortfolioUiState.Error(
                            lastPortfolioError ?: marketRes.error,
                            acct.id
                        )
                    }
                }
            }
        }
    }

    fun findStock(stockId: Int): TornStock? = rawStocks.firstOrNull { it.stockId == stockId }

    fun findPosition(stockId: Int): OwnedStockPosition? {
        val stock = findStock(stockId) ?: return null
        val holding = rawHoldings.firstOrNull { it.stockId == stockId && it.totalShares > 0L } ?: return null
        val watchStockIds = _watches.value.filter { it.enabled }.map { it.stockId }.toSet()
        val summary = StockPortfolioCalculator.buildSummary(
            stocks = rawStocks,
            holdings = rawHoldings,
            sparklines = sparklines,
            watchStockIds = watchStockIds
        )
        return summary.positions.firstOrNull { it.stock.stockId == stockId }
            ?: StockPortfolioCalculator.buildPosition(
                stock = stock,
                holding = holding,
                totalPortfolioValue = holding.totalShares.toDouble() * stock.currentPrice,
                series = sparklines[stock.stockId].orEmpty(),
                hasWatch = stock.stockId in watchStockIds
            )
    }

    suspend fun loadHistory(stockId: Int, range: ChartRange): List<StockPricePoint> {
        val profileId = _activeProfile.value.profile?.id ?: "default"
        return stockRepository.getHistory(profileId, stockId, range)
    }

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
        val holdingsById = rawHoldings.associateBy { it.stockId }
        val q = _searchQuery.value.trim().lowercase()
        val ownedOnly = _ownedOnlyFilter.value

        val mapped = rawStocks
            .filter { stock ->
                val matchesQuery = q.isEmpty() ||
                    stock.name.lowercase().contains(q) ||
                    stock.acronym.lowercase().contains(q)
                val matchesOwned = !ownedOnly || (holdingsById[stock.stockId]?.totalShares ?: 0L) > 0L
                matchesQuery && matchesOwned
            }
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
                    hasWatch = stock.stockId in watchStockIds,
                    holding = holdingsById[stock.stockId]?.takeIf { it.totalShares > 0L }
                )
            }

        val sorted = when (_sortMode.value) {
            StockSort.NAME -> mapped.sortedBy { it.stock.acronym }
            StockSort.PRICE_DESC -> mapped.sortedByDescending { it.stock.currentPrice }
            StockSort.CHANGE_24H -> mapped.sortedByDescending { it.changePct24h ?: Double.NEGATIVE_INFINITY }
        }

        _uiState.value = if (sorted.isEmpty()) {
            val msg = if (ownedOnly && q.isEmpty()) "No owned stocks found." else "No matching stocks found."
            StocksUiState.Empty(msg)
        } else {
            StocksUiState.Content(
                items = sorted,
                fetchedAtMs = lastFetchedAtMs,
                isStale = lastIsStale,
                staleReason = lastStaleReason
            )
        }

        rebuildPortfolioState(watchStockIds, q)
    }

    private fun rebuildPortfolioState(watchStockIds: Set<Int>, query: String) {
        val acctId = _activeProfile.value.primaryAccount?.id
        val err = lastPortfolioError
        if (!hasLoadedHoldings && err != null) {
            _portfolioState.value = PortfolioUiState.Error(err, acctId)
            return
        }
        if (!hasLoadedHoldings) {
            _portfolioState.value = PortfolioUiState.Loading
            return
        }

        val summary = StockPortfolioCalculator.buildSummary(
            stocks = rawStocks,
            holdings = rawHoldings,
            sparklines = sparklines,
            watchStockIds = watchStockIds
        )

        if (summary.positions.isEmpty()) {
            _portfolioState.value = PortfolioUiState.Empty("You do not own any Torn stocks yet.")
            return
        }

        val filtered = summary.positions.filter { pos ->
            query.isEmpty() ||
                pos.stock.name.lowercase().contains(query) ||
                pos.stock.acronym.lowercase().contains(query)
        }
        val sortedPositions = when (_portfolioSort.value) {
            PortfolioSort.VALUE_DESC -> filtered.sortedByDescending { it.currentValue }
            PortfolioSort.PNL_DESC -> filtered.sortedByDescending { it.unrealizedPnl ?: Double.NEGATIVE_INFINITY }
            PortfolioSort.PNL_PCT_DESC -> filtered.sortedByDescending { it.unrealizedPnlPct ?: Double.NEGATIVE_INFINITY }
            PortfolioSort.NAME -> filtered.sortedBy { it.stock.acronym }
        }

        _portfolioState.value = PortfolioUiState.Content(
            summary = summary,
            visiblePositions = sortedPositions,
            fetchedAtMs = lastPortfolioFetchedAtMs.takeIf { it > 0L } ?: lastFetchedAtMs,
            isStale = lastPortfolioIsStale,
            staleReason = lastPortfolioStaleReason
        )
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
