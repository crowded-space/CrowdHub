package app.crowdhub.feature.war

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.crowdhub.connector.ffscouter.FfRepository
import app.crowdhub.connector.ffscouter.FfStats
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.db.StakeoutEntity
import app.crowdhub.core.db.TargetDao
import app.crowdhub.core.db.TargetEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.feature.flight.FlightEstimate
import app.crowdhub.feature.flight.FlightRepository
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornFactionMember
import app.crowdhub.provider.torn.TornRankedWar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WarQuickFilter(val label: String) {
    ATTACKABLE("Attackable now"),
    LEAVING_SOON("Leaving hosp <=5m"),
    ABROAD_TRAVELING("Abroad/Traveling"),
    ONLINE("Online"),
    HOSPITAL("Hospital")
}

enum class WarSortMode(val label: String) {
    TACTICAL("Default"),
    LEVEL("Level"),
    FF_DESC("FF desc"),
    BS_ASC("BS asc"),
    NAME("Name")
}

data class WarExtraFilters(
    val minFf: Double? = null,
    val maxFf: Double? = null,
    val minLevel: Int? = null,
    val maxLevel: Int? = null,
    val maxBs: Long? = null,
    val hideOfflineOverHours: Int? = null,
    val leavingSoonMinutes: Int = 5
)

data class WarMemberRowModel(
    val member: TornFactionMember,
    val ffStats: FfStats?,
    val flightEstimate: FlightEstimate?,
    val postLandingGraceEndMs: Long?,
    val stakeout: StakeoutEntity?
)

sealed class WarUiState {
    data object Loading : WarUiState()
    data class NoActiveWar(val message: String, val accountId: String) : WarUiState()
    data class Content(
        val war: TornRankedWar?,
        val factionId: Long,
        val factionName: String,
        val rows: List<WarMemberRowModel>,
        val ffEnabled: Boolean,
        val fetchedAtMs: Long,
        val isStale: Boolean,
        val staleReason: String?
    ) : WarUiState()
    data class Error(val error: ApiError, val accountId: String?) : WarUiState()
}

class WarViewModel(
    private val profileRepository: ProfileRepository,
    private val tornApi: TornApi,
    private val ffRepository: FfRepository,
    private val flightRepository: FlightRepository,
    private val stakeoutManager: StakeoutManager,
    private val alertEngine: AlertEngine,
    private val targetDao: TargetDao,
    private val settingsStore: SettingsStore,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<WarUiState>(WarUiState.Loading)
    val uiState: StateFlow<WarUiState> = _uiState.asStateFlow()

    private val _quickFilters = MutableStateFlow<Set<WarQuickFilter>>(emptySet())
    val quickFilters: StateFlow<Set<WarQuickFilter>> = _quickFilters.asStateFlow()

    private val _sortMode = MutableStateFlow(WarSortMode.TACTICAL)
    val sortMode: StateFlow<WarSortMode> = _sortMode.asStateFlow()

    private val _extraFilters = MutableStateFlow(WarExtraFilters())
    val extraFilters: StateFlow<WarExtraFilters> = _extraFilters.asStateFlow()

    private val _activeProfile = MutableStateFlow(ActiveProfileState(null, null, emptyList()))
    val activeProfile: StateFlow<ActiveProfileState> = _activeProfile.asStateFlow()

    private var rawMembers: List<TornFactionMember> = emptyList()
    private var currentWar: TornRankedWar? = null
    private var currentFactionId: Long? = null
    private var currentFactionName: String = ""
    private var ffMap: Map<Long, FfStats> = emptyMap()
    private var ffEnabled: Boolean = false
    private var stakeoutsMap: Map<Long, StakeoutEntity> = emptyMap()
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
                        stakeoutManager.observeStakeouts(acct.id).collect { list ->
                            stakeoutsMap = list.associateBy { it.playerId }
                            rebuildContent()
                        }
                    }
                    launch {
                        ffRepository.observeEnabled(acct.id).collect { en ->
                            ffEnabled = en
                            rebuildContent()
                        }
                    }
                    refresh(forceRefresh = false)
                }
            }
        }
    }

    fun setManualFactionId(factionIdStr: String) {
        viewModelScope.launch {
            settingsStore.setManualWarFactionId(factionIdStr.trim())
            refresh(forceRefresh = true)
        }
    }

    fun toggleQuickFilter(filter: WarQuickFilter) {
        val current = _quickFilters.value.toMutableSet()
        if (filter in current) current.remove(filter) else current.add(filter)
        _quickFilters.value = current
        viewModelScope.launch { rebuildContent() }
    }

    fun setSortMode(mode: WarSortMode) {
        _sortMode.value = mode
        viewModelScope.launch { rebuildContent() }
    }

    fun setExtraFilters(filters: WarExtraFilters) {
        _extraFilters.value = filters
        viewModelScope.launch { rebuildContent() }
    }

    fun toggleStar(playerId: Long) {
        val acctId = _activeProfile.value.primaryAccount?.id ?: return
        viewModelScope.launch {
            stakeoutManager.toggleStar(acctId, playerId, stakeoutsMap.containsKey(playerId))
        }
    }

    fun updateStakeout(stakeout: StakeoutEntity) {
        viewModelScope.launch { stakeoutManager.updateStakeoutPrefs(stakeout) }
    }

    fun addToTargets(member: TornFactionMember) {
        val acctId = _activeProfile.value.primaryAccount?.id ?: return
        viewModelScope.launch {
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = acctId,
                    playerId = member.playerId,
                    name = member.name,
                    note = "From War (${currentFactionName.ifEmpty { "Enemy" }})",
                    source = "WAR",
                    addedAt = clock.nowEpochMs(),
                    watched = false
                )
            )
        }
    }

    fun refresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = _activeProfile.value
            val acct = state.primaryAccount ?: return@launch
            if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                return@launch
            }
            if (rawMembers.isEmpty()) {
                _uiState.value = WarUiState.Loading
            }

            val prefs: AppPreferences = settingsStore.current()
            val priority = if (prefs.watchModeActive) RequestPriority.WATCH else RequestPriority.USER

            val manualId = prefs.manualWarFactionId.trim().toLongOrNull()
            var war: TornRankedWar? = null
            var targetFactionId: Long? = manualId?.takeIf { it > 0L }
            var targetFactionName = targetFactionId?.let { "Faction #$it" } ?: ""

            if (targetFactionId == null) {
                val ownFactionRes = tornApi.getOwnFactionId(acct.id, priority, forceRefresh)
                val ownFactionId = ownFactionRes.getOrNull()
                if (ownFactionId == null) {
                    val err = (ownFactionRes as? ApiResult.Failure)?.error
                        ?: ApiError.Unknown(null, "You are not in a faction")
                    if (rawMembers.isEmpty()) {
                        _uiState.value = WarUiState.NoActiveWar(err.message, acct.id)
                        return@launch
                    }
                } else {
                    val warsRes = tornApi.getRankedWars(acct.id, ownFactionId, priority, forceRefresh)
                    val wars = warsRes.getOrNull().orEmpty()
                    war = wars.firstOrNull { it.isActive } ?: wars.firstOrNull()
                    if (war != null) {
                        targetFactionId = war.enemyFactionId
                        targetFactionName = war.enemyFactionName
                    } else if (rawMembers.isEmpty()) {
                        _uiState.value = WarUiState.NoActiveWar("No active ranked war found", acct.id)
                        return@launch
                    }
                }
            }

            val fid = targetFactionId ?: return@launch
            val membersRes = tornApi.getFactionMembers(acct.id, fid, priority, forceRefresh)
            when (membersRes) {
                is ApiResult.Success -> {
                    currentWar = war
                    currentFactionId = fid
                    currentFactionName = targetFactionName
                    rawMembers = membersRes.data
                    lastFetchedAtMs = membersRes.fetchedAtMs
                    lastIsStale = false
                    lastStaleReason = null

                    val candidates = stakeoutManager.processFactionMembers(acct.id, fid, rawMembers)
                    if (candidates.isNotEmpty()) {
                        alertEngine.reconcileCandidates(
                            accountId = acct.id,
                            managedPrefixes = setOf("war:"),
                            candidates = candidates,
                            nowMs = clock.nowEpochMs()
                        )
                    }

                    ffEnabled = ffRepository.isConsentedAndEnabled(acct.id)
                    if (ffEnabled && rawMembers.isNotEmpty()) {
                        val ffRes = ffRepository.stats(acct.id, rawMembers.map { it.playerId }, forceRefresh = false)
                        ffMap = ffRes.getOrNull().orEmpty()
                    }
                    rebuildContent()
                }
                is ApiResult.Failure -> {
                    val stale = membersRes.staleData
                    if (!stale.isNullOrEmpty()) {
                        rawMembers = stale
                        currentFactionId = fid
                        currentFactionName = targetFactionName
                        lastFetchedAtMs = membersRes.staleFetchedAtMs ?: clock.nowEpochMs()
                        lastIsStale = true
                        lastStaleReason = membersRes.error.message
                        rebuildContent()
                    } else if (rawMembers.isNotEmpty()) {
                        lastIsStale = true
                        lastStaleReason = membersRes.error.message
                        rebuildContent()
                    } else {
                        _uiState.value = WarUiState.Error(membersRes.error, acct.id)
                    }
                }
            }
        }
    }

    private suspend fun rebuildContent() {
        val fid = currentFactionId ?: return
        if (rawMembers.isEmpty() && _uiState.value is WarUiState.NoActiveWar) return

        val nowMs = clock.nowEpochMs()
        val nowSec = nowMs / 1_000L
        val qf = _quickFilters.value
        val ef = _extraFilters.value

        val enriched = rawMembers.map { member ->
            val flightEst = if (member.status.state == StatusKind.TRAVELING) {
                flightRepository.estimateForMember(
                    playerId = member.playerId,
                    description = member.status.description,
                    planeTypeApi = member.status.planeImageType,
                    apiUntilSec = member.status.untilEpochSec
                )
            } else null

            WarMemberRowModel(
                member = member,
                ffStats = if (ffEnabled) ffMap[member.playerId] else null,
                flightEstimate = flightEst,
                postLandingGraceEndMs = flightRepository.getPostLandingGraceEndsAtMs(member.playerId),
                stakeout = stakeoutsMap[member.playerId]
            )
        }

        val filtered = enriched.filter { row ->
            val m = row.member
            val quickMatch = if (qf.isEmpty()) true else {
                qf.any { f ->
                    when (f) {
                        WarQuickFilter.ATTACKABLE -> m.status.state == StatusKind.OKAY
                        WarQuickFilter.LEAVING_SOON -> {
                            m.status.state == StatusKind.HOSPITAL &&
                                m.status.untilEpochSec != null &&
                                (m.status.untilEpochSec - nowSec) in 0..(ef.leavingSoonMinutes * 60L)
                        }
                        WarQuickFilter.ABROAD_TRAVELING ->
                            m.status.state == StatusKind.ABROAD || m.status.state == StatusKind.TRAVELING
                        WarQuickFilter.ONLINE -> m.lastActionStatus.equals("Online", ignoreCase = true)
                        WarQuickFilter.HOSPITAL -> m.status.state == StatusKind.HOSPITAL
                    }
                }
            }

            val ff = row.ffStats?.fairFight
            val bs = row.ffStats?.bsEstimate
            val lvl = m.level
            val lastActSec = m.lastActionTimestampSec

            val ffOk = (!ffEnabled) || (
                (ef.minFf == null || (ff != null && ff >= ef.minFf)) &&
                    (ef.maxFf == null || (ff != null && ff <= ef.maxFf))
                )
            val bsOk = (!ffEnabled) || ef.maxBs == null || (bs != null && bs <= ef.maxBs)
            val lvlOk = (ef.minLevel == null || (lvl != null && lvl >= ef.minLevel)) &&
                (ef.maxLevel == null || (lvl != null && lvl <= ef.maxLevel))
            val offlineOk = ef.hideOfflineOverHours == null || lastActSec == null ||
                (nowSec - lastActSec) <= ef.hideOfflineOverHours * 3600L

            quickMatch && ffOk && bsOk && lvlOk && offlineOk
        }

        val sorted = when (_sortMode.value) {
            WarSortMode.TACTICAL -> filtered.sortedWith(
                compareBy<WarMemberRowModel>(
                    { tacticalPriority(it.member.status.state) },
                    {
                        when (it.member.status.state) {
                            StatusKind.HOSPITAL -> it.member.status.untilEpochSec ?: Long.MAX_VALUE
                            StatusKind.TRAVELING -> it.flightEstimate?.windowStartMs ?: Long.MAX_VALUE
                            else -> 0L
                        }
                    },
                    { it.member.name }
                )
            )
            WarSortMode.LEVEL -> filtered.sortedByDescending { it.member.level ?: 0 }
            WarSortMode.FF_DESC -> filtered.sortedByDescending { it.ffStats?.fairFight ?: Double.NEGATIVE_INFINITY }
            WarSortMode.BS_ASC -> filtered.sortedBy { it.ffStats?.bsEstimate ?: Long.MAX_VALUE }
            WarSortMode.NAME -> filtered.sortedBy { it.member.name.lowercase() }
        }

        _uiState.value = WarUiState.Content(
            war = currentWar,
            factionId = fid,
            factionName = currentFactionName,
            rows = sorted,
            ffEnabled = ffEnabled,
            fetchedAtMs = lastFetchedAtMs,
            isStale = lastIsStale,
            staleReason = lastStaleReason
        )
    }

    private fun tacticalPriority(state: StatusKind): Int = when (state) {
        StatusKind.OKAY -> 0
        StatusKind.HOSPITAL -> 1
        StatusKind.TRAVELING -> 2
        StatusKind.ABROAD -> 3
        else -> 4
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val tornApi: TornApi,
        private val ffRepository: FfRepository,
        private val flightRepository: FlightRepository,
        private val stakeoutManager: StakeoutManager,
        private val alertEngine: AlertEngine,
        private val targetDao: TargetDao,
        private val settingsStore: SettingsStore,
        private val clock: Clock
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WarViewModel(
                profileRepository,
                tornApi,
                ffRepository,
                flightRepository,
                stakeoutManager,
                alertEngine,
                targetDao,
                settingsStore,
                clock
            ) as T
        }
    }
}
