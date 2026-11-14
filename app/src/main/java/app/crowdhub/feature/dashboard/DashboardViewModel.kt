package app.crowdhub.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornBars
import app.crowdhub.provider.torn.TornBasicInfo
import app.crowdhub.provider.torn.TornCooldowns
import app.crowdhub.provider.torn.TornMoney
import app.crowdhub.provider.torn.TornNotifications
import app.crowdhub.provider.torn.TornTravel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DashboardData(
    val accountId: String,
    val basic: TornBasicInfo?,
    val bars: TornBars?,
    val cooldowns: TornCooldowns?,
    val travel: TornTravel?,
    val money: TornMoney?,
    val notifications: TornNotifications?,
    val fetchedAtMs: Long,
    val isStale: Boolean,
    val staleReason: String?
)

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Content(val data: DashboardData) : DashboardUiState()
    data class Error(val error: ApiError, val accountId: String?) : DashboardUiState()
}

class DashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val tornApi: TornApi,
    private val alertEngine: AlertEngine,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _activeProfile = MutableStateFlow(ActiveProfileState(null, null, emptyList()))
    val activeProfile: StateFlow<ActiveProfileState> = _activeProfile.asStateFlow()

    private val refreshMutex = Mutex()
    private var lastFetchedAtMs: Long = 0L

    init {
        viewModelScope.launch {
            profileRepository.observeActiveState().collect { state ->
                val prevId = _activeProfile.value.primaryAccount?.id
                _activeProfile.value = state
                val acct = state.primaryAccount
                if (acct != null && (acct.id != prevId || _uiState.value is DashboardUiState.Loading)) {
                    refresh(forceRefresh = false)
                }
            }
        }
    }

    fun refreshIfOlderThan(thresholdMs: Long = 30_000L) {
        val now = clock.nowEpochMs()
        if (now - lastFetchedAtMs >= thresholdMs) {
            refresh(forceRefresh = false)
        }
    }

    fun refresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            refreshMutex.withLock {
                val state = _activeProfile.value
                val acct = state.primaryAccount ?: return@withLock
                if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                    return@withLock
                }

                if (_uiState.value !is DashboardUiState.Content) {
                    _uiState.value = DashboardUiState.Loading
                } else if (forceRefresh) {
                    _isRefreshing.value = true
                }

                val basicDef = async { tornApi.getBasic(acct.id, RequestPriority.USER, forceRefresh) }
                val barsDef = async { tornApi.getBars(acct.id, RequestPriority.USER, forceRefresh) }
                val cdDef = async { tornApi.getCooldowns(acct.id, RequestPriority.USER, forceRefresh) }
                val travelDef = async { tornApi.getTravel(acct.id, RequestPriority.USER, forceRefresh) }
                val moneyDef = async { tornApi.getMoney(acct.id, RequestPriority.USER, forceRefresh) }
                val notifDef = async { tornApi.getNotifications(acct.id, RequestPriority.USER, forceRefresh) }

                val basicRes = basicDef.await()
                val barsRes = barsDef.await()
                val cdRes = cdDef.await()
                val travelRes = travelDef.await()
                val moneyRes = moneyDef.await()
                val notifRes = notifDef.await()

                val allResults = listOf(basicRes, barsRes, cdRes, travelRes, moneyRes, notifRes)
                val firstFailure = allResults.filterIsInstance<ApiResult.Failure<*>>().firstOrNull()

                val basic = basicRes.getOrNull()
                val bars = barsRes.getOrNull()
                val cd = cdRes.getOrNull()
                val travel = travelRes.getOrNull()
                val money = moneyRes.getOrNull()
                val notif = notifRes.getOrNull()

                val hasAnyData = basic != null || bars != null || cd != null || travel != null || money != null
                if (hasAnyData) {
                    val freshTs = allResults.filterIsInstance<ApiResult.Success<*>>()
                        .maxOfOrNull { it.fetchedAtMs }
                        ?: allResults.filterIsInstance<ApiResult.Failure<*>>()
                            .mapNotNull { it.staleFetchedAtMs }
                            .maxOrNull()
                        ?: clock.nowEpochMs()
                    lastFetchedAtMs = freshTs
                    _uiState.value = DashboardUiState.Content(
                        DashboardData(
                            accountId = acct.id,
                            basic = basic,
                            bars = bars,
                            cooldowns = cd,
                            travel = travel,
                            money = money,
                            notifications = notif,
                            fetchedAtMs = freshTs,
                            isStale = firstFailure != null,
                            staleReason = firstFailure?.error?.message
                        )
                    )
                    if (firstFailure == null) {
                        alertEngine.runCycle(AlertTriggerMode.FOREGROUND)
                    }
                } else {
                    _uiState.value = DashboardUiState.Error(
                        error = firstFailure?.error ?: ApiError.Network(),
                        accountId = acct.id
                    )
                }
                _isRefreshing.value = false
            }
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val tornApi: TornApi,
        private val alertEngine: AlertEngine,
        private val clock: Clock
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(profileRepository, tornApi, alertEngine, clock) as T
        }
    }
}
