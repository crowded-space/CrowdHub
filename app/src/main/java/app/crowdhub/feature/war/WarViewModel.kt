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

sealed class CurrentWarStatus {
    data class Active(val war: TornRankedWar) : CurrentWarStatus()
    data class Upcoming(val war: TornRankedWar) : CurrentWarStatus()
    data class ManualStakeout(val factionId: Long, val factionName: String) : CurrentWarStatus()
    data class NotInWar(val ownFactionId: Long?, val reason: String) : CurrentWarStatus()
}

data class HistoryFactionMembersState(
    val isLoading: Boolean = false,
    val rows: List<WarMemberRowModel> = emptyList(),
    val totalMemberCount: Int = 0,
    val errorMessage: String? = null,
    val fetchedAtMs: Long? = null,
    val isStale: Boolean = false
)

data class HistoryFactionGroup(
    val enemyFactionId: Long,
    val enemyFactionName: String,
    val wars: List<TornRankedWar>,
    val isExpanded: Boolean,
    val membersState: HistoryFactionMembersState
) {
    val wins: Int get() = wars.count { it.winnerFactionId != null && it.winnerFactionId == it.ownFactionId }
    val losses: Int get() = wars.count { it.winnerFactionId != null && it.winnerFactionId != it.ownFactionId }
    val latestWar: TornRankedWar get() = wars.first()
}

data class OpponentWarHistory(
    val enemyFactionId: Long,
    val enemyFactionName: String,
    val wars: List<TornRankedWar>
)

sealed class WarUiState {
    data object Loading : WarUiState()
    data class Content(
        val status: CurrentWarStatus,
        val activeRows: List<WarMemberRowModel>,
        val totalActiveMemberCount: Int,
        val historyGroups: List<HistoryFactionGroup>,
        val ffEnabled: Boolean,
        val fetchedAtMs: Long,
        val isStale: Boolean,
        val staleReason: String?
    ) : WarUiState()
    data class Error(val error: ApiError, val accountId: String?) : WarUiState()
}

internal fun resolveCurrentWarStatus(
    wars: List<TornRankedWar>,
    ownFactionId: Long?,
    manualFactionId: Long?,
    manualFactionName: String? = null
): CurrentWarStatus {
    if (manualFactionId != null && manualFactionId > 0L) {
        val matchedName = manualFactionName?.takeIf { it.isNotBlank() }
            ?: wars.firstOrNull { it.enemyFactionId == manualFactionId }?.enemyFactionName
            ?: "Faction #$manualFactionId"
        return CurrentWarStatus.ManualStakeout(manualFactionId, matchedName)
    }

    val activeWar = wars.firstOrNull { it.isActive && !it.isScheduled }
    if (activeWar != null) {
        return CurrentWarStatus.Active(activeWar)
    }

    val upcomingWar = wars.firstOrNull { it.isActive && it.isScheduled }
    if (upcomingWar != null) {
        return CurrentWarStatus.Upcoming(upcomingWar)
    }

    val reason = if (ownFactionId == null) {
        "You are not currently in a faction."
    } else {
        "Your faction is not currently in an active ranked war."
    }
    return CurrentWarStatus.NotInWar(ownFactionId = ownFactionId, reason = reason)
}

internal fun groupEndedWarsByOpponent(wars: List<TornRankedWar>): List<OpponentWarHistory> {
    val endedSorted = wars
        .filter { !it.isActive }
        .sortedByDescending { it.endEpochSec ?: it.startEpochSec ?: it.warId }

    if (endedSorted.isEmpty()) return emptyList()

    val grouped = LinkedHashMap<Long, MutableList<TornRankedWar>>()
    for (war in endedSorted) {
        grouped.getOrPut(war.enemyFactionId) { mutableListOf() }.add(war)
    }

    return grouped.map { (enemyId, enemyWars) ->
        OpponentWarHistory(
            enemyFactionId = enemyId,
            enemyFactionName = enemyWars.first().enemyFactionName,
            wars = enemyWars
        )
    }
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

    private var cachedStatus: CurrentWarStatus? = null
    private var activeRawMembers: List<TornFactionMember> = emptyList()
    private var cachedWars: List<TornRankedWar> = emptyList()
    private var ownFactionIdCache: Long? = null

    private val expandedHistoryFactionIds = mutableSetOf<Long>()
    private val historyRawMembersByFaction = mutableMapOf<Long, List<TornFactionMember>>()
    private val historyLoadingFactions = mutableSetOf<Long>()
    private val historyErrorsByFaction = mutableMapOf<Long, String?>()
    private val historyFetchedAtByFaction = mutableMapOf<Long, Long>()
    private val historyStaleByFaction = mutableSetOf<Long>()

    private val ffMap = mutableMapOf<Long, FfStats>()
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
                    resetAccountState()
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

    private fun resetAccountState() {
        cachedStatus = null
        activeRawMembers = emptyList()
        cachedWars = emptyList()
        ownFactionIdCache = null
        expandedHistoryFactionIds.clear()
        historyRawMembersByFaction.clear()
        historyLoadingFactions.clear()
        historyErrorsByFaction.clear()
        historyFetchedAtByFaction.clear()
        historyStaleByFaction.clear()
        ffMap.clear()
        lastFetchedAtMs = 0L
        lastIsStale = false
        lastStaleReason = null
        _uiState.value = WarUiState.Loading
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

    fun clearAllFilters() {
        _quickFilters.value = emptySet()
        _extraFilters.value = WarExtraFilters()
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

    fun addToTargets(member: TornFactionMember, sourceFactionName: String? = null) {
        val acctId = _activeProfile.value.primaryAccount?.id ?: return
        val label = sourceFactionName?.takeIf { it.isNotBlank() }
            ?: when (val st = cachedStatus) {
                is CurrentWarStatus.Active -> st.war.enemyFactionName
                is CurrentWarStatus.Upcoming -> st.war.enemyFactionName
                is CurrentWarStatus.ManualStakeout -> st.factionName
                else -> "Enemy"
            }
        viewModelScope.launch {
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = acctId,
                    playerId = member.playerId,
                    name = member.name,
                    note = "From War ($label)",
                    source = "WAR",
                    addedAt = clock.nowEpochMs(),
                    watched = false
                )
            )
        }
    }

    fun toggleHistoryFaction(enemyFactionId: Long) {
        if (enemyFactionId in expandedHistoryFactionIds) {
            expandedHistoryFactionIds.remove(enemyFactionId)
            viewModelScope.launch { rebuildContent() }
            return
        }
        expandedHistoryFactionIds.add(enemyFactionId)
        val alreadyLoaded = historyRawMembersByFaction.containsKey(enemyFactionId)
        if (!alreadyLoaded) {
            refreshHistoryFaction(enemyFactionId, forceRefresh = false)
        } else {
            viewModelScope.launch { rebuildContent() }
        }
    }

    fun refreshHistoryFaction(enemyFactionId: Long, forceRefresh: Boolean = true) {
        val acct = _activeProfile.value.primaryAccount ?: return
        viewModelScope.launch {
            historyLoadingFactions.add(enemyFactionId)
            historyErrorsByFaction.remove(enemyFactionId)
            rebuildContent()

            val res = tornApi.getFactionMembers(acct.id, enemyFactionId, RequestPriority.USER, forceRefresh)
            historyLoadingFactions.remove(enemyFactionId)

            when (res) {
                is ApiResult.Success -> {
                    historyRawMembersByFaction[enemyFactionId] = res.data
                    historyFetchedAtByFaction[enemyFactionId] = res.fetchedAtMs
                    historyStaleByFaction.remove(enemyFactionId)
                    historyErrorsByFaction.remove(enemyFactionId)
                    fetchMissingFfStats(acct.id, res.data)
                }
                is ApiResult.Failure -> {
                    val stale = res.staleData
                    if (!stale.isNullOrEmpty()) {
                        historyRawMembersByFaction[enemyFactionId] = stale
                        historyFetchedAtByFaction[enemyFactionId] = res.staleFetchedAtMs ?: clock.nowEpochMs()
                        historyStaleByFaction.add(enemyFactionId)
                        historyErrorsByFaction.remove(enemyFactionId)
                    } else if (historyRawMembersByFaction[enemyFactionId].isNullOrEmpty()) {
                        historyErrorsByFaction[enemyFactionId] = res.error.message
                    } else {
                        historyStaleByFaction.add(enemyFactionId)
                    }
                }
            }
            rebuildContent()
        }
    }

    fun refresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = _activeProfile.value
            val acct = state.primaryAccount ?: return@launch
            if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                return@launch
            }
            if (cachedStatus == null) {
                _uiState.value = WarUiState.Loading
            }

            val prefs: AppPreferences = settingsStore.current()
            val priority = if (prefs.watchModeActive) RequestPriority.WATCH else RequestPriority.USER
            val manualId = prefs.manualWarFactionId.trim().toLongOrNull()?.takeIf { it > 0L }

            val ownFactionRes = tornApi.getOwnFactionId(acct.id, priority, forceRefresh)
            var staleNotice: String? = null
            var anyStale = false
            var latestFetchMs = clock.nowEpochMs()

            when (ownFactionRes) {
                is ApiResult.Success -> {
                    ownFactionIdCache = ownFactionRes.data
                    latestFetchMs = ownFactionRes.fetchedAtMs
                }
                is ApiResult.Failure -> {
                    if (ownFactionRes.staleData != null) {
                        ownFactionIdCache = ownFactionRes.staleData
                        anyStale = true
                        staleNotice = ownFactionRes.error.message
                    } else if (manualId == null && cachedStatus == null) {
                        _uiState.value = WarUiState.Error(ownFactionRes.error, acct.id)
                        return@launch
                    } else {
                        anyStale = true
                        staleNotice = ownFactionRes.error.message
                    }
                }
            }

            val ownFactionId = ownFactionIdCache
            if (ownFactionId != null) {
                val warsRes = tornApi.getRankedWars(acct.id, ownFactionId, priority, forceRefresh)
                when (warsRes) {
                    is ApiResult.Success -> {
                        cachedWars = warsRes.data
                        latestFetchMs = warsRes.fetchedAtMs
                    }
                    is ApiResult.Failure -> {
                        val staleWars = warsRes.staleData
                        if (staleWars != null) {
                            cachedWars = staleWars
                            anyStale = true
                            staleNotice = warsRes.error.message
                        } else if (manualId == null && cachedStatus == null) {
                            _uiState.value = WarUiState.Error(warsRes.error, acct.id)
                            return@launch
                        } else {
                            anyStale = true
                            staleNotice = warsRes.error.message
                        }
                    }
                }
            } else {
                cachedWars = emptyList()
            }

            val resolvedStatus = resolveCurrentWarStatus(
                wars = cachedWars,
                ownFactionId = ownFactionId,
                manualFactionId = manualId
            )

            val activeTargetFactionId = when (resolvedStatus) {
                is CurrentWarStatus.Active -> resolvedStatus.war.enemyFactionId
                is CurrentWarStatus.Upcoming -> resolvedStatus.war.enemyFactionId
                is CurrentWarStatus.ManualStakeout -> resolvedStatus.factionId
                is CurrentWarStatus.NotInWar -> null
            }

            if (activeTargetFactionId == null) {
                cachedStatus = resolvedStatus
                activeRawMembers = emptyList()
                lastFetchedAtMs = latestFetchMs
                lastIsStale = anyStale
                lastStaleReason = staleNotice
                ffEnabled = ffRepository.isConsentedAndEnabled(acct.id)
                rebuildContent()
                return@launch
            }

            when (val membersRes = tornApi.getFactionMembers(acct.id, activeTargetFactionId, priority, forceRefresh)) {
                is ApiResult.Success -> {
                    cachedStatus = resolvedStatus
                    activeRawMembers = membersRes.data
                    lastFetchedAtMs = membersRes.fetchedAtMs
                    lastIsStale = anyStale
                    lastStaleReason = staleNotice

                    val candidates = stakeoutManager.processFactionMembers(acct.id, activeTargetFactionId, activeRawMembers)
                    if (candidates.isNotEmpty()) {
                        alertEngine.reconcileCandidates(
                            accountId = acct.id,
                            managedPrefixes = setOf("war:"),
                            candidates = candidates,
                            nowMs = clock.nowEpochMs()
                        )
                    }

                    ffEnabled = ffRepository.isConsentedAndEnabled(acct.id)
                    fetchMissingFfStats(acct.id, activeRawMembers)
                    rebuildContent()
                }
                is ApiResult.Failure -> {
                    val staleMembers = membersRes.staleData
                    cachedStatus = resolvedStatus
                    if (!staleMembers.isNullOrEmpty()) {
                        activeRawMembers = staleMembers
                        lastFetchedAtMs = membersRes.staleFetchedAtMs ?: clock.nowEpochMs()
                        lastIsStale = true
                        lastStaleReason = membersRes.error.message
                        rebuildContent()
                    } else if (activeRawMembers.isNotEmpty() || cachedWars.isNotEmpty()) {
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

    private suspend fun fetchMissingFfStats(accountId: String, members: List<TornFactionMember>) {
        if (!ffEnabled || members.isEmpty()) return
        val ids = members.map { it.playerId }
        val res = ffRepository.stats(accountId, ids, forceRefresh = false)
        res.getOrNull()?.let { ffMap.putAll(it) }
    }

    private suspend fun rebuildContent() {
        val status = cachedStatus ?: return
        val nowMs = clock.nowEpochMs()
        val nowSec = nowMs / 1_000L

        val activeRows = filterAndSortMembers(activeRawMembers, nowSec)
        val historyGroups = groupEndedWarsByOpponent(cachedWars).map { summary ->
            val fid = summary.enemyFactionId
            val isExpanded = fid in expandedHistoryFactionIds
            val raw = historyRawMembersByFaction[fid].orEmpty()
            val rows = if (isExpanded && raw.isNotEmpty()) {
                filterAndSortMembers(raw, nowSec)
            } else {
                emptyList()
            }
            HistoryFactionGroup(
                enemyFactionId = fid,
                enemyFactionName = summary.enemyFactionName,
                wars = summary.wars,
                isExpanded = isExpanded,
                membersState = HistoryFactionMembersState(
                    isLoading = fid in historyLoadingFactions,
                    rows = rows,
                    totalMemberCount = raw.size,
                    errorMessage = historyErrorsByFaction[fid],
                    fetchedAtMs = historyFetchedAtByFaction[fid],
                    isStale = fid in historyStaleByFaction
                )
            )
        }

        _uiState.value = WarUiState.Content(
            status = status,
            activeRows = activeRows,
            totalActiveMemberCount = activeRawMembers.size,
            historyGroups = historyGroups,
            ffEnabled = ffEnabled,
            fetchedAtMs = lastFetchedAtMs,
            isStale = lastIsStale,
            staleReason = lastStaleReason
        )
    }

    private suspend fun filterAndSortMembers(
        members: List<TornFactionMember>,
        nowSec: Long
    ): List<WarMemberRowModel> {
        if (members.isEmpty()) return emptyList()
        val qf = _quickFilters.value
        val ef = _extraFilters.value

        val enriched = members.map { member ->
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

        return when (_sortMode.value) {
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
