package app.crowdhub.feature.chain

import app.crowdhub.connector.ffscouter.FfRepository
import app.crowdhub.connector.ffscouter.FfStats
import app.crowdhub.connector.ffscouter.FfTargetsFilterParams
import app.crowdhub.core.alerts.AlertCandidate
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.db.HospitalCaptureEntity
import app.crowdhub.core.db.TargetDao
import app.crowdhub.core.db.TargetEntity
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornOtherProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class TargetsUiState(
    val activeTab: TargetSourceTab = TargetSourceTab.HOSPITAL,
    val ffEnabled: Boolean = false,
    val items: List<TargetItem> = emptyList(),
    val hospitalCapturedAtMs: Long? = null,
    val isRefreshing: Boolean = false,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TargetsViewModel(
    private val profileRepository: ProfileRepository,
    private val targetDao: TargetDao,
    private val tornApi: TornApi,
    private val ffRepository: FfRepository,
    private val alertEngine: AlertEngine,
    private val clock: Clock,
    private val scope: CoroutineScope
) {
    private val activeTab = MutableStateFlow(TargetSourceTab.HOSPITAL)
    private val sortMode = MutableStateFlow(TargetSortMode.LEAVES_SOONEST)
    private val filterCriteria = MutableStateFlow(TargetFilterCriteria())
    private val isRefreshing = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    private val ffScouterTargets = MutableStateFlow<List<TargetItem>>(emptyList())
    private val liveProfiles = MutableStateFlow<Map<Long, TornOtherProfile>>(emptyMap())
    private val ffStatsMap = MutableStateFlow<Map<Long, FfStats>>(emptyMap())
    private val lastTargetRefreshMs = ConcurrentHashMap<Long, Long>()

    val selectedTab: StateFlow<TargetSourceTab> = activeTab.asStateFlow()
    val currentSort: StateFlow<TargetSortMode> = sortMode.asStateFlow()
    val currentFilter: StateFlow<TargetFilterCriteria> = filterCriteria.asStateFlow()

    private val activeProfile: StateFlow<ActiveProfileState> = profileRepository.observeActiveState()
        .stateIn(scope, SharingStarted.Eagerly, ActiveProfileState(null, null, emptyList()))

    private val ffEnabledFlow = activeProfile.flatMapLatest { state ->
        val acct = state.primaryAccount ?: return@flatMapLatest flowOf(false)
        ffRepository.observeEnabled(acct.id)
    }

    private val myListFlow = activeProfile.flatMapLatest { state ->
        val acct = state.primaryAccount ?: return@flatMapLatest flowOf(emptyList())
        targetDao.observeTargets(acct.id)
    }

    private val hospitalFlow = activeProfile.flatMapLatest { state ->
        val acct = state.primaryAccount ?: return@flatMapLatest flowOf(emptyList())
        targetDao.observeHospitalCaptures(acct.id)
    }

    private data class CoreSnapshot(
        val tab: TargetSourceTab,
        val ffEnabled: Boolean,
        val captures: List<HospitalCaptureEntity>,
        val myList: List<TargetEntity>,
        val ffTargets: List<TargetItem>
    )

    private val coreFlow = combine(
        activeTab,
        ffEnabledFlow,
        hospitalFlow,
        myListFlow,
        ffScouterTargets
    ) { tab, ffEnabled, captures, myList, ffTargets ->
        CoreSnapshot(tab, ffEnabled, captures, myList, ffTargets)
    }

    private data class MetaSnapshot(
        val profiles: Map<Long, TornOtherProfile>,
        val ffMap: Map<Long, FfStats>,
        val sort: TargetSortMode,
        val filter: TargetFilterCriteria,
        val refreshing: Boolean,
        val msg: String?
    )

    private val metaFlow = combine(
        liveProfiles,
        ffStatsMap,
        sortMode,
        filterCriteria,
        combine(isRefreshing, statusMessage) { r, m -> r to m }
    ) { profiles, ffMap, sort, filter, (refreshing, msg) ->
        MetaSnapshot(profiles, ffMap, sort, filter, refreshing, msg)
    }

    val uiState: StateFlow<TargetsUiState> = combine(coreFlow, metaFlow) { core, meta ->
        val effectiveTab = if (core.tab == TargetSourceTab.FFSCOUTER && !core.ffEnabled) {
            TargetSourceTab.HOSPITAL
        } else {
            core.tab
        }

        val myMap = core.myList.associateBy { it.playerId }
        val rawItems: List<TargetItem> = when (effectiveTab) {
            TargetSourceTab.HOSPITAL -> core.captures.map { cap ->
                val live = meta.profiles[cap.playerId]
                val ff = if (core.ffEnabled) meta.ffMap[cap.playerId] else null
                val estLeave = cap.remainingSecAtCapture?.let { rem ->
                    (cap.capturedAt / 1_000L) + rem
                }
                TargetItem(
                    playerId = cap.playerId,
                    name = live?.name ?: cap.name,
                    level = live?.level ?: cap.level,
                    status = live?.status?.state ?: StatusKind.HOSPITAL,
                    hospitalLeavesAtSec = live?.status?.untilEpochSec ?: estLeave,
                    lastActionSec = live?.lastActionTimestampSec,
                    fairFight = ff?.fairFight,
                    bsEstimate = ff?.bsEstimate,
                    bsHuman = ff?.bsHuman,
                    note = myMap[cap.playerId]?.note,
                    watched = myMap[cap.playerId]?.watched == true,
                    source = TargetSourceTab.HOSPITAL,
                    capturedAtMs = cap.capturedAt
                )
            }
            TargetSourceTab.FFSCOUTER -> core.ffTargets.map { t ->
                val live = meta.profiles[t.playerId]
                val ff = meta.ffMap[t.playerId]
                TargetItem(
                    playerId = t.playerId,
                    name = live?.name ?: t.name,
                    level = live?.level ?: t.level,
                    status = live?.status?.state ?: t.status,
                    hospitalLeavesAtSec = live?.status?.untilEpochSec ?: t.hospitalLeavesAtSec,
                    lastActionSec = live?.lastActionTimestampSec ?: t.lastActionSec,
                    fairFight = ff?.fairFight ?: t.fairFight,
                    bsEstimate = ff?.bsEstimate ?: t.bsEstimate,
                    bsHuman = ff?.bsHuman ?: t.bsHuman,
                    note = myMap[t.playerId]?.note,
                    watched = myMap[t.playerId]?.watched == true,
                    source = TargetSourceTab.FFSCOUTER
                )
            }
            TargetSourceTab.MY_LIST -> core.myList.map { entry ->
                val live = meta.profiles[entry.playerId]
                val ff = if (core.ffEnabled) meta.ffMap[entry.playerId] else null
                TargetItem(
                    playerId = entry.playerId,
                    name = live?.name ?: entry.name ?: "Player ${entry.playerId}",
                    level = live?.level,
                    status = live?.status?.state ?: StatusKind.UNKNOWN,
                    hospitalLeavesAtSec = live?.status?.untilEpochSec,
                    lastActionSec = live?.lastActionTimestampSec,
                    fairFight = ff?.fairFight,
                    bsEstimate = ff?.bsEstimate,
                    bsHuman = ff?.bsHuman,
                    note = entry.note,
                    watched = entry.watched,
                    source = TargetSourceTab.MY_LIST
                )
            }
        }

        val sortedFiltered = TargetFilters.apply(
            items = rawItems,
            criteria = meta.filter,
            sortMode = meta.sort,
            nowMs = clock.nowEpochMs()
        )

        TargetsUiState(
            activeTab = effectiveTab,
            ffEnabled = core.ffEnabled,
            items = sortedFiltered,
            hospitalCapturedAtMs = core.captures.maxOfOrNull { it.capturedAt },
            isRefreshing = meta.refreshing,
            message = meta.msg
        )
    }.stateIn(scope, SharingStarted.Eagerly, TargetsUiState())

    fun selectTab(tab: TargetSourceTab) {
        activeTab.value = tab
    }

    fun setSortMode(mode: TargetSortMode) {
        sortMode.value = mode
    }

    fun setFilterCriteria(criteria: TargetFilterCriteria) {
        filterCriteria.value = criteria
    }

    fun clearMessage() {
        statusMessage.value = null
    }

    suspend fun captureHospitalRowsJson(rawJsArrayJson: String?): Int {
        val accountId = activeProfile.value.primaryAccount?.id ?: return 0
        val parsed = HospitalPageParser.parseCapturedJson(rawJsArrayJson)
        if (parsed.isEmpty()) return 0

        val now = clock.nowEpochMs()
        val entities = parsed.map { row ->
            HospitalCaptureEntity(
                accountId = accountId,
                playerId = row.playerId,
                name = row.name,
                level = row.level,
                remainingSecAtCapture = row.remainingSec,
                capturedAt = now
            )
        }
        targetDao.clearHospitalCapturesForAccount(accountId)
        targetDao.upsertHospitalCaptures(entities)
        liveProfiles.value = liveProfiles.value - entities.map { it.playerId }.toSet()

        if (uiState.value.ffEnabled) {
            fetchFfStats(accountId, entities.map { it.playerId })
        }
        return entities.size
    }

    fun refreshTopN(limit: Int = MAX_HOSPITAL_REFRESH) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val items = uiState.value.items.take(limit.coerceIn(1, MAX_HOSPITAL_REFRESH))
            if (items.isEmpty()) return@launch
            refreshTargetsLive(accountId, items.map { it.playerId })
        }
    }

    fun fetchFfScouterTargets() {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            isRefreshing.value = true
            statusMessage.value = null
            when (val res = ffRepository.targets(accountId, FfTargetsFilterParams())) {
                is ApiResult.Success -> {
                    val mapped = res.data.map { t ->
                        TargetItem(
                            playerId = t.playerId,
                            name = t.name ?: "Player ${t.playerId}",
                            level = t.level,
                            status = StatusKind.UNKNOWN,
                            hospitalLeavesAtSec = null,
                            lastActionSec = null,
                            fairFight = t.fairFight,
                            bsEstimate = t.bsEstimate,
                            bsHuman = t.bsHuman,
                            note = null,
                            watched = false,
                            source = TargetSourceTab.FFSCOUTER
                        )
                    }
                    ffScouterTargets.value = mapped
                    if (mapped.isEmpty()) {
                        statusMessage.value = "FFScouter returned 0 targets."
                    }
                }
                is ApiResult.Failure -> {
                    statusMessage.value = res.error.message
                }
            }
            isRefreshing.value = false
        }
    }

    fun refreshMyListTargets() {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val ids = targetDao.getTargets(accountId).take(MAX_TRACKED_REFRESH).map { it.playerId }
            if (ids.isEmpty()) return@launch
            refreshTargetsLive(accountId, ids)
        }
    }

    fun addManualTarget(playerIdRaw: String, note: String) {
        val playerId = playerIdRaw.trim().toLongOrNull() ?: return
        if (playerId <= 0L) return
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = accountId,
                    playerId = playerId,
                    name = liveProfiles.value[playerId]?.name,
                    note = note.trim().takeIf { it.isNotEmpty() },
                    source = "MANUAL",
                    addedAt = clock.nowEpochMs(),
                    watched = false
                )
            )
            if (uiState.value.ffEnabled) {
                fetchFfStats(accountId, listOf(playerId))
            }
        }
    }

    fun saveFromOtherTab(item: TargetItem) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = accountId,
                    playerId = item.playerId,
                    name = item.name,
                    note = item.note,
                    source = item.source.name,
                    addedAt = clock.nowEpochMs(),
                    watched = item.watched
                )
            )
            statusMessage.value = "Saved ${item.name} to My List."
        }
    }

    fun removeMyTarget(playerId: Long) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            targetDao.deleteTarget(accountId, playerId)
        }
    }

    fun toggleWatchTarget(item: TargetItem) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val newWatched = !item.watched
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = accountId,
                    playerId = item.playerId,
                    name = item.name,
                    note = item.note,
                    source = item.source.name,
                    addedAt = clock.nowEpochMs(),
                    watched = newWatched
                )
            )
            scheduleWatchAlertIfNeeded(accountId, item.copy(watched = newWatched))
        }
    }

    private suspend fun refreshTargetsLive(accountId: String, ids: List<Long>) {
        isRefreshing.value = true
        statusMessage.value = null
        val now = clock.nowEpochMs()
        val updatedMap = liveProfiles.value.toMutableMap()
        val cappedIds = ids.take(MAX_TRACKED_REFRESH)
        var refreshedCount = 0

        for (id in cappedIds) {
            val prev = lastTargetRefreshMs[id]
            if (prev != null && (now - prev) < MIN_PER_TARGET_INTERVAL_MS) {
                continue
            }
            when (val res = tornApi.getOtherProfile(accountId, id, RequestPriority.USER, forceRefresh = true)) {
                is ApiResult.Success -> {
                    updatedMap[id] = res.data
                    lastTargetRefreshMs[id] = clock.nowEpochMs()
                    refreshedCount++
                    val myEntry = targetDao.getTargets(accountId).firstOrNull { it.playerId == id }
                    if (myEntry != null && myEntry.watched) {
                        val until = res.data.status.untilEpochSec
                        if (res.data.status.state == StatusKind.HOSPITAL && until != null) {
                            scheduleWatchAlertIfNeeded(
                                accountId,
                                TargetItem(
                                    playerId = id,
                                    name = res.data.name,
                                    level = res.data.level,
                                    status = res.data.status.state,
                                    hospitalLeavesAtSec = until,
                                    lastActionSec = res.data.lastActionTimestampSec,
                                    fairFight = null,
                                    bsEstimate = null,
                                    bsHuman = null,
                                    note = myEntry.note,
                                    watched = true,
                                    source = TargetSourceTab.MY_LIST
                                )
                            )
                        }
                    }
                }
                is ApiResult.Failure -> {
                    statusMessage.value = res.error.message
                    break
                }
            }
        }

        liveProfiles.value = updatedMap
        if (uiState.value.ffEnabled) {
            fetchFfStats(accountId, cappedIds)
        }
        if (statusMessage.value == null) {
            statusMessage.value = "Refreshed $refreshedCount target(s)."
        }
        isRefreshing.value = false
    }

    private suspend fun fetchFfStats(accountId: String, ids: List<Long>) {
        if (ids.isEmpty()) return
        val res = ffRepository.stats(accountId, ids)
        if (res is ApiResult.Success) {
            ffStatsMap.value = ffStatsMap.value + res.data
        }
    }

    private suspend fun scheduleWatchAlertIfNeeded(accountId: String, item: TargetItem) {
        val prefix = "chain:${item.playerId}:out:"
        if (!item.watched || item.hospitalLeavesAtSec == null) {
            alertEngine.reconcileCandidates(accountId, setOf(prefix), emptyList())
            return
        }
        val fireAtMs = item.hospitalLeavesAtSec * 1_000L
        if (fireAtMs <= clock.nowEpochMs()) return
        val bucket = item.hospitalLeavesAtSec / 60L
        alertEngine.reconcileCandidates(
            accountId = accountId,
            managedPrefixes = setOf(prefix),
            candidates = listOf(
                AlertCandidate(
                    dedupeKey = "$prefix$bucket",
                    accountId = accountId,
                    channelId = NotificationChannels.WAR,
                    title = "${item.name} leaving hospital",
                    body = "Chain target leaves hospital now.",
                    fireAtEpochMs = fireAtMs,
                    deepLink = "targets"
                )
            )
        )
    }

    companion object {
        const val MAX_HOSPITAL_REFRESH = 20
        const val MAX_TRACKED_REFRESH = 25
        const val MIN_PER_TARGET_INTERVAL_MS = 60_000L
    }
}
