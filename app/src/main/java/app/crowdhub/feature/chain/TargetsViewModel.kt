package app.crowdhub.feature.chain

import app.crowdhub.connector.ffscouter.FfRepository
import app.crowdhub.connector.ffscouter.FfStats
import app.crowdhub.connector.ffscouter.FfTargetsFilterParams
import app.crowdhub.core.alerts.AlertCandidate
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.db.HospitalCaptureEntity
import app.crowdhub.core.db.StakeoutEntity
import app.crowdhub.core.db.TargetDao
import app.crowdhub.core.db.TargetEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.PlayerNameCache
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.feature.flight.FlightEstimate
import app.crowdhub.feature.flight.FlightRepository
import app.crowdhub.feature.war.StakeoutManager
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornOtherProfile
import app.crowdhub.provider.torn.TornUserListCategory
import app.crowdhub.provider.torn.TornUserListEntry
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

data class PeopleHubUiState(
    val activeTab: PeopleInnerTab = PeopleInnerTab.FRIENDS,
    val chainSubTab: ChainSubTab = ChainSubTab.HOSPITAL,
    val ffEnabled: Boolean = false,
    val friendsItems: List<PeopleUserListItem> = emptyList(),
    val targetsListItems: List<PeopleUserListItem> = emptyList(),
    val enemiesItems: List<PeopleUserListItem> = emptyList(),
    val listErrors: Map<TornUserListCategory, String> = emptyMap(),
    val listFetchedAtMs: Map<TornUserListCategory, Long> = emptyMap(),
    val hospitalChainItems: List<TargetItem> = emptyList(),
    val ffChainItems: List<TargetItem> = emptyList(),
    val additionalItems: List<TargetItem> = emptyList(),
    val stakeoutItems: List<StakeoutHubItem> = emptyList(),
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
    private val scope: CoroutineScope,
    private val stakeoutManager: StakeoutManager? = null,
    private val flightRepository: FlightRepository? = null,
    private val playerNameCache: PlayerNameCache? = null
) {
    private val activeTab = MutableStateFlow(TargetSourceTab.HOSPITAL)
    private val activePeopleTab = MutableStateFlow(PeopleInnerTab.FRIENDS)
    private val chainSubTab = MutableStateFlow(ChainSubTab.HOSPITAL)
    private val peopleSearchQuery = MutableStateFlow("")
    private val peopleListStatusFilter = MutableStateFlow(TargetStatusFilter.ALL)

    private val sortMode = MutableStateFlow(TargetSortMode.LEAVES_SOONEST)
    private val filterCriteria = MutableStateFlow(TargetFilterCriteria())
    private val isRefreshing = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    private val ffScouterTargets = MutableStateFlow<List<TargetItem>>(emptyList())
    private val liveProfiles = MutableStateFlow<Map<Long, TornOtherProfile>>(emptyMap())
    private val ffStatsMap = MutableStateFlow<Map<Long, FfStats>>(emptyMap())
    private val userListsMap = MutableStateFlow<Map<TornUserListCategory, List<TornUserListEntry>>>(emptyMap())
    private val listErrorsMap = MutableStateFlow<Map<TornUserListCategory, String>>(emptyMap())
    private val listFetchedAtMap = MutableStateFlow<Map<TornUserListCategory, Long>>(emptyMap())
    private val stakeoutFlights = MutableStateFlow<Map<Long, FlightEstimate>>(emptyMap())
    private val cachedPlayerNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val lastTargetRefreshMs = ConcurrentHashMap<Long, Long>()

    val selectedTab: StateFlow<TargetSourceTab> = activeTab.asStateFlow()
    val selectedPeopleTab: StateFlow<PeopleInnerTab> = activePeopleTab.asStateFlow()
    val selectedChainSubTab: StateFlow<ChainSubTab> = chainSubTab.asStateFlow()
    val currentPeopleSearch: StateFlow<String> = peopleSearchQuery.asStateFlow()
    val currentPeopleListStatusFilter: StateFlow<TargetStatusFilter> = peopleListStatusFilter.asStateFlow()
    val currentSort: StateFlow<TargetSortMode> = sortMode.asStateFlow()
    val currentFilter: StateFlow<TargetFilterCriteria> = filterCriteria.asStateFlow()

    private val activeProfile: StateFlow<ActiveProfileState> = profileRepository.observeActiveState()
        .stateIn(scope, SharingStarted.Eagerly, ActiveProfileState(null, null, emptyList()))

    init {
        scope.launch {
            var lastKey: Pair<String?, String?> = null to null
            activeProfile.collect { state ->
                val currentKey = state.profile?.id to state.primaryAccount?.id
                if (currentKey != lastKey) {
                    lastKey = currentKey
                    ffScouterTargets.value = emptyList()
                    liveProfiles.value = emptyMap()
                    ffStatsMap.value = emptyMap()
                    userListsMap.value = emptyMap()
                    listErrorsMap.value = emptyMap()
                    listFetchedAtMap.value = emptyMap()
                    stakeoutFlights.value = emptyMap()
                    cachedPlayerNames.value = emptyMap()
                    lastTargetRefreshMs.clear()
                    statusMessage.value = null
                }
            }
        }
        scope.launch {
            stakeoutsFlow.collect { stakes ->
                val profileId = activeProfile.value.profile?.id ?: return@collect
                val cache = playerNameCache ?: return@collect
                val ids = stakes.map { it.playerId }.distinct()
                if (ids.isNotEmpty()) {
                    val cached = cache.getAnyCachedNames(profileId, ids)
                    if (cached.isNotEmpty()) {
                        cachedPlayerNames.value = cachedPlayerNames.value + cached
                    }
                }
            }
        }
    }

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

    private val stakeoutsFlow = activeProfile.flatMapLatest { state ->
        val acct = state.primaryAccount ?: return@flatMapLatest flowOf(emptyList())
        stakeoutManager?.observeStakeouts(acct.id) ?: flowOf(emptyList())
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

    private data class PeopleDataSnapshot(
        val userLists: Map<TornUserListCategory, List<TornUserListEntry>>,
        val listErrors: Map<TornUserListCategory, String>,
        val listFetchedAt: Map<TornUserListCategory, Long>,
        val stakeouts: List<StakeoutEntity>,
        val flights: Map<Long, FlightEstimate>,
        val cachedNames: Map<Long, String>,
        val peopleTab: PeopleInnerTab,
        val chainSub: ChainSubTab,
        val query: String,
        val listStatus: TargetStatusFilter
    )

    private val peopleDataFlow = combine(
        combine(userListsMap, listErrorsMap, listFetchedAtMap) { lists, errs, ts -> Triple(lists, errs, ts) },
        combine(stakeoutsFlow, stakeoutFlights, cachedPlayerNames) { stakes, flts, names -> Triple(stakes, flts, names) },
        combine(activePeopleTab, chainSubTab, peopleSearchQuery, peopleListStatusFilter) { pTab, cSub, q, st ->
            listOf(pTab, cSub, q, st)
        }
    ) { (lists, errs, ts), (stakes, flts, names), uiControls ->
        @Suppress("UNCHECKED_CAST")
        PeopleDataSnapshot(
            userLists = lists,
            listErrors = errs,
            listFetchedAt = ts,
            stakeouts = stakes,
            flights = flts,
            cachedNames = names,
            peopleTab = uiControls[0] as PeopleInnerTab,
            chainSub = uiControls[1] as ChainSubTab,
            query = uiControls[2] as String,
            listStatus = uiControls[3] as TargetStatusFilter
        )
    }

    val uiState: StateFlow<TargetsUiState> = combine(coreFlow, metaFlow) { core, meta ->
        val effectiveTab = if (core.tab == TargetSourceTab.FFSCOUTER && !core.ffEnabled) {
            TargetSourceTab.HOSPITAL
        } else {
            core.tab
        }
        val myMap = core.myList.associateBy { it.playerId }
        val nowMs = clock.nowEpochMs()
        val rawItems = when (effectiveTab) {
            TargetSourceTab.HOSPITAL -> buildHospitalItems(core.captures, meta.profiles, meta.ffMap, myMap, core.ffEnabled)
            TargetSourceTab.FFSCOUTER -> buildFfItems(core.ffTargets, meta.profiles, meta.ffMap, myMap)
            TargetSourceTab.MY_LIST -> buildAdditionalItems(core.myList, meta.profiles, meta.ffMap, core.ffEnabled)
        }
        TargetsUiState(
            activeTab = effectiveTab,
            ffEnabled = core.ffEnabled,
            items = TargetFilters.apply(rawItems, meta.filter, meta.sort, nowMs),
            hospitalCapturedAtMs = core.captures.maxOfOrNull { it.capturedAt },
            isRefreshing = meta.refreshing,
            message = meta.msg
        )
    }.stateIn(scope, SharingStarted.Eagerly, TargetsUiState())

    val peopleUiState: StateFlow<PeopleHubUiState> = combine(coreFlow, metaFlow, peopleDataFlow) { core, meta, pData ->
        val myMap = core.myList.associateBy { it.playerId }
        val stakeoutIds = pData.stakeouts.map { it.playerId }.toSet()
        val nowMs = clock.nowEpochMs()

        fun mapUserList(cat: TornUserListCategory): List<PeopleUserListItem> {
            val raw = pData.userLists[cat].orEmpty()
            val mapped = raw.map { entry ->
                val live = meta.profiles[entry.playerId]
                val ff = if (core.ffEnabled) meta.ffMap[entry.playerId] else null
                PeopleUserListItem(
                    playerId = entry.playerId,
                    name = live?.name ?: entry.name,
                    level = live?.level ?: entry.level,
                    status = live?.status?.state ?: entry.status.state,
                    statusDescription = live?.status?.description ?: entry.status.description,
                    hospitalLeavesAtSec = live?.status?.untilEpochSec ?: entry.status.untilEpochSec,
                    lastActionStatus = live?.lastActionStatus ?: entry.lastActionStatus,
                    lastActionRelative = entry.lastActionRelative,
                    lastActionSec = live?.lastActionTimestampSec ?: entry.lastActionTimestampSec,
                    note = myMap[entry.playerId]?.note ?: entry.note,
                    fairFight = ff?.fairFight,
                    bsEstimate = ff?.bsEstimate,
                    bsHuman = ff?.bsHuman,
                    stakedOut = entry.playerId in stakeoutIds,
                    inAdditional = myMap.containsKey(entry.playerId)
                )
            }
            return TargetFilters.filterPeopleList(mapped, pData.query, pData.listStatus)
        }

        val knownNames = buildMap {
            putAll(pData.cachedNames)
            core.myList.forEach { e -> e.name?.takeIf { !PlayerNameCache.isPlaceholderName(it, e.playerId) }?.let { put(e.playerId, it) } }
            core.captures.forEach { c -> put(c.playerId, c.name) }
            pData.userLists.values.flatten().forEach { u -> put(u.playerId, u.name) }
            meta.profiles.forEach { (id, prof) -> put(id, prof.name) }
        }

        val stakeoutItems = pData.stakeouts.map { stake ->
            val live = meta.profiles[stake.playerId]
            val listEntry = pData.userLists.values.asSequence().flatten().firstOrNull { it.playerId == stake.playerId }
            val ff = if (core.ffEnabled) meta.ffMap[stake.playerId] else null
            StakeoutHubItem(
                stakeout = stake,
                name = live?.name ?: knownNames[stake.playerId] ?: "Unknown #${stake.playerId}",
                level = live?.level ?: listEntry?.level ?: core.captures.firstOrNull { it.playerId == stake.playerId }?.level,
                status = live?.status?.state ?: listEntry?.status?.state ?: StatusKind.UNKNOWN,
                statusDescription = live?.status?.description ?: listEntry?.status?.description ?: "Tap Refresh to check status",
                hospitalLeavesAtSec = live?.status?.untilEpochSec ?: listEntry?.status?.untilEpochSec,
                lastActionStatus = live?.lastActionStatus ?: listEntry?.lastActionStatus,
                lastActionRelative = listEntry?.lastActionRelative,
                lastActionSec = live?.lastActionTimestampSec ?: listEntry?.lastActionTimestampSec,
                flightEstimate = pData.flights[stake.playerId],
                fairFight = ff?.fairFight,
                bsEstimate = ff?.bsEstimate,
                bsHuman = ff?.bsHuman,
                inAdditional = myMap.containsKey(stake.playerId)
            )
        }.filter { item ->
            val q = pData.query.trim().lowercase()
            q.isEmpty() || item.name.lowercase().contains(q) || item.stakeout.playerId.toString().contains(q)
        }

        val hospitalRaw = buildHospitalItems(core.captures, meta.profiles, meta.ffMap, myMap, core.ffEnabled)
            .map { it.copy(watched = it.playerId in stakeoutIds || it.watched) }
        val ffRaw = buildFfItems(core.ffTargets, meta.profiles, meta.ffMap, myMap)
            .map { it.copy(watched = it.playerId in stakeoutIds || it.watched) }
        val additionalRaw = buildAdditionalItems(core.myList, meta.profiles, meta.ffMap, core.ffEnabled)
            .map { item ->
                val resolvedName = if (PlayerNameCache.isPlaceholderName(item.name, item.playerId)) {
                    knownNames[item.playerId] ?: item.name
                } else {
                    item.name
                }
                item.copy(name = resolvedName, watched = item.playerId in stakeoutIds || item.watched)
            }

        val effectiveChainSub = if (pData.chainSub == ChainSubTab.FFSCOUTER && !core.ffEnabled) {
            ChainSubTab.HOSPITAL
        } else {
            pData.chainSub
        }

        PeopleHubUiState(
            activeTab = pData.peopleTab,
            chainSubTab = effectiveChainSub,
            ffEnabled = core.ffEnabled,
            friendsItems = mapUserList(TornUserListCategory.FRIENDS),
            targetsListItems = mapUserList(TornUserListCategory.TARGETS),
            enemiesItems = mapUserList(TornUserListCategory.ENEMIES),
            listErrors = pData.listErrors,
            listFetchedAtMs = pData.listFetchedAt,
            hospitalChainItems = TargetFilters.apply(hospitalRaw, meta.filter, meta.sort, nowMs),
            ffChainItems = TargetFilters.apply(ffRaw, meta.filter, meta.sort, nowMs),
            additionalItems = TargetFilters.apply(additionalRaw, meta.filter, meta.sort, nowMs),
            stakeoutItems = stakeoutItems,
            hospitalCapturedAtMs = core.captures.maxOfOrNull { it.capturedAt },
            isRefreshing = meta.refreshing,
            message = meta.msg
        )
    }.stateIn(scope, SharingStarted.Eagerly, PeopleHubUiState())

    private fun buildHospitalItems(
        captures: List<HospitalCaptureEntity>,
        profiles: Map<Long, TornOtherProfile>,
        ffMap: Map<Long, FfStats>,
        myMap: Map<Long, TargetEntity>,
        ffEnabled: Boolean
    ): List<TargetItem> = captures.map { cap ->
        val live = profiles[cap.playerId]
        val ff = if (ffEnabled) ffMap[cap.playerId] else null
        val estLeave = cap.remainingSecAtCapture?.let { rem -> (cap.capturedAt / 1_000L) + rem }
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

    private fun buildFfItems(
        ffTargets: List<TargetItem>,
        profiles: Map<Long, TornOtherProfile>,
        ffMap: Map<Long, FfStats>,
        myMap: Map<Long, TargetEntity>
    ): List<TargetItem> = ffTargets.map { t ->
        val live = profiles[t.playerId]
        val ff = ffMap[t.playerId]
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

    private fun buildAdditionalItems(
        myList: List<TargetEntity>,
        profiles: Map<Long, TornOtherProfile>,
        ffMap: Map<Long, FfStats>,
        ffEnabled: Boolean
    ): List<TargetItem> = myList.map { entry ->
        val live = profiles[entry.playerId]
        val ff = if (ffEnabled) ffMap[entry.playerId] else null
        TargetItem(
            playerId = entry.playerId,
            name = live?.name ?: entry.name ?: "Unknown #${entry.playerId}",
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

    fun selectTab(tab: TargetSourceTab) {
        activeTab.value = tab
    }

    fun selectPeopleTab(tab: PeopleInnerTab) {
        activePeopleTab.value = tab
        statusMessage.value = null
        when (tab) {
            PeopleInnerTab.FRIENDS -> maybeAutoLoadList(TornUserListCategory.FRIENDS)
            PeopleInnerTab.TARGETS -> maybeAutoLoadList(TornUserListCategory.TARGETS)
            PeopleInnerTab.ENEMIES -> maybeAutoLoadList(TornUserListCategory.ENEMIES)
            PeopleInnerTab.STAKEOUT -> scope.launch { hydrateCachedNamesForStakeouts() }
            else -> {}
        }
    }

    fun selectChainSubTab(subTab: ChainSubTab) {
        chainSubTab.value = subTab
    }

    fun setPeopleSearchQuery(query: String) {
        peopleSearchQuery.value = query
    }

    fun setPeopleListStatusFilter(filter: TargetStatusFilter) {
        peopleListStatusFilter.value = filter
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

    private fun maybeAutoLoadList(category: TornUserListCategory) {
        val lastFetched = listFetchedAtMap.value[category] ?: 0L
        val hasItems = userListsMap.value[category] != null
        val stale = (clock.nowEpochMs() - lastFetched) > 120_000L
        if (!hasItems || stale) {
            refreshUserListCategory(category, forceRefresh = false)
        }
    }

    fun refreshCurrentPeopleTab(forceRefresh: Boolean = true) {
        when (activePeopleTab.value) {
            PeopleInnerTab.FRIENDS -> refreshUserListCategory(TornUserListCategory.FRIENDS, forceRefresh)
            PeopleInnerTab.TARGETS -> refreshUserListCategory(TornUserListCategory.TARGETS, forceRefresh)
            PeopleInnerTab.ENEMIES -> refreshUserListCategory(TornUserListCategory.ENEMIES, forceRefresh)
            PeopleInnerTab.CHAIN -> {
                if (chainSubTab.value == ChainSubTab.FFSCOUTER && peopleUiState.value.ffEnabled) {
                    fetchFfScouterTargets()
                } else {
                    refreshTopN()
                }
            }
            PeopleInnerTab.ADDITIONAL -> refreshMyListTargets()
            PeopleInnerTab.STAKEOUT -> refreshStakeouts()
        }
    }

    fun refreshUserListCategory(category: TornUserListCategory, forceRefresh: Boolean = true) {
        scope.launch {
            val state = activeProfile.value
            val accountId = state.primaryAccount?.id ?: return@launch
            val profileId = state.profile?.id ?: "default"
            isRefreshing.value = true
            statusMessage.value = null
            when (val res = tornApi.getUserList(accountId, category, RequestPriority.USER, forceRefresh)) {
                is ApiResult.Success -> {
                    userListsMap.value = userListsMap.value + (category to res.data)
                    listErrorsMap.value = listErrorsMap.value - category
                    listFetchedAtMap.value = listFetchedAtMap.value + (category to res.fetchedAtMs)
                    val names = res.data.associate { it.playerId to it.name }
                    playerNameCache?.recordNames(profileId, names)
                    if (names.isNotEmpty()) cachedPlayerNames.value = cachedPlayerNames.value + names
                    if (peopleUiState.value.ffEnabled && res.data.isNotEmpty()) {
                        fetchFfStats(accountId, res.data.take(50).map { it.playerId })
                    }
                }
                is ApiResult.Failure -> {
                    val stale = res.staleData
                    if (!stale.isNullOrEmpty()) {
                        userListsMap.value = userListsMap.value + (category to stale)
                        res.staleFetchedAtMs?.let { ts ->
                            listFetchedAtMap.value = listFetchedAtMap.value + (category to ts)
                        }
                    }
                    val friendly = when (res.error) {
                        is ApiError.AccessLevel -> "API key lacks permission to read ${category.label} list (requires Full/Minimal Torn key permissions)."
                        else -> res.error.message
                    }
                    listErrorsMap.value = listErrorsMap.value + (category to friendly)
                }
            }
            isRefreshing.value = false
        }
    }

    private suspend fun hydrateCachedNamesForStakeouts() {
        val profileId = activeProfile.value.profile?.id ?: return
        val cache = playerNameCache ?: return
        val ids = peopleUiState.value.stakeoutItems.map { it.stakeout.playerId }
        if (ids.isEmpty()) return
        val cached = cache.getAnyCachedNames(profileId, ids)
        if (cached.isNotEmpty()) {
            cachedPlayerNames.value = cachedPlayerNames.value + cached
        }
    }

    suspend fun captureHospitalRowsJson(rawJsArrayJson: String?): Int {
        val state = activeProfile.value
        val accountId = state.primaryAccount?.id ?: return 0
        val profileId = state.profile?.id ?: "default"
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

        val capturedNames = entities.associate { it.playerId to it.name }
        playerNameCache?.recordNames(profileId, capturedNames)
        cachedPlayerNames.value = cachedPlayerNames.value + capturedNames

        if (uiState.value.ffEnabled) {
            fetchFfStats(accountId, entities.map { it.playerId })
        }
        return entities.size
    }

    fun refreshTopN(limit: Int = MAX_HOSPITAL_REFRESH) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val items = peopleUiState.value.hospitalChainItems.take(limit.coerceIn(1, MAX_HOSPITAL_REFRESH))
            if (items.isEmpty()) return@launch
            refreshTargetsLive(accountId, items.map { it.playerId })
        }
    }

    fun fetchFfScouterTargets() {
        scope.launch {
            val state = activeProfile.value
            val accountId = state.primaryAccount?.id ?: return@launch
            val profileId = state.profile?.id ?: "default"
            isRefreshing.value = true
            statusMessage.value = null
            when (val res = ffRepository.targets(accountId, FfTargetsFilterParams())) {
                is ApiResult.Success -> {
                    val harvested = mutableMapOf<Long, String>()
                    val mapped = res.data.map { t ->
                        t.name?.takeIf { !PlayerNameCache.isPlaceholderName(it, t.playerId) }?.let { harvested[t.playerId] = it }
                        TargetItem(
                            playerId = t.playerId,
                            name = t.name ?: "Unknown #${t.playerId}",
                            level = t.level,
                            status = StatusKind.UNKNOWN,
                            hospitalLeavesAtSec = null,
                            lastActionSec = t.lastActionSec,
                            fairFight = t.fairFight,
                            bsEstimate = t.bsEstimate,
                            bsHuman = t.bsHuman,
                            note = null,
                            watched = false,
                            source = TargetSourceTab.FFSCOUTER
                        )
                    }
                    playerNameCache?.recordNames(profileId, harvested)
                    if (harvested.isNotEmpty()) cachedPlayerNames.value = cachedPlayerNames.value + harvested
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

    fun refreshStakeouts() {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val ids = peopleUiState.value.stakeoutItems.take(MAX_TRACKED_REFRESH).map { it.stakeout.playerId }
            if (ids.isEmpty()) return@launch
            refreshTargetsLive(accountId, ids, bypassCooldown = true)
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
                    name = liveProfiles.value[playerId]?.name ?: cachedPlayerNames.value[playerId],
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

    fun addManualStakeout(playerIdRaw: String) {
        val playerId = playerIdRaw.trim().toLongOrNull() ?: return
        if (playerId <= 0L) return
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val existing = peopleUiState.value.stakeoutItems.any { it.stakeout.playerId == playerId }
            if (!existing) {
                stakeoutManager?.toggleStar(accountId, playerId, currentlyStarred = false)
            }
            refreshTargetsLive(accountId, listOf(playerId), bypassCooldown = true)
        }
    }

    fun saveToAdditional(playerId: Long, name: String?, note: String?, source: String) {
        scope.launch {
            val state = activeProfile.value
            val accountId = state.primaryAccount?.id ?: return@launch
            val profileId = state.profile?.id ?: "default"
            val cleanName = name?.takeIf { !PlayerNameCache.isPlaceholderName(it, playerId) }
            if (cleanName != null) {
                playerNameCache?.recordName(profileId, playerId, cleanName)
                cachedPlayerNames.value = cachedPlayerNames.value + (playerId to cleanName)
            }
            val isStaked = peopleUiState.value.stakeoutItems.any { it.stakeout.playerId == playerId }
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = accountId,
                    playerId = playerId,
                    name = cleanName,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    source = source,
                    addedAt = clock.nowEpochMs(),
                    watched = isStaked
                )
            )
            statusMessage.value = "Saved ${cleanName ?: "Player $playerId"} to Additional list."
        }
    }

    fun updateAdditionalNote(playerId: Long, note: String) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            val existing = targetDao.getTargets(accountId).firstOrNull { it.playerId == playerId } ?: return@launch
            targetDao.upsertTarget(existing.copy(note = note.trim().takeIf { it.isNotEmpty() }))
        }
    }

    fun saveFromOtherTab(item: TargetItem) {
        saveToAdditional(item.playerId, item.name, item.note, item.source.name)
    }

    fun removeMyTarget(playerId: Long) {
        scope.launch {
            val accountId = activeProfile.value.primaryAccount?.id ?: return@launch
            targetDao.deleteTarget(accountId, playerId)
        }
    }

    fun toggleStakeout(playerId: Long, name: String? = null) {
        scope.launch {
            val state = activeProfile.value
            val accountId = state.primaryAccount?.id ?: return@launch
            val profileId = state.profile?.id ?: "default"
            val currentlyStaked = peopleUiState.value.stakeoutItems.any { it.stakeout.playerId == playerId }
            val cleanName = name?.takeIf { !PlayerNameCache.isPlaceholderName(it, playerId) }
            if (cleanName != null) {
                playerNameCache?.recordName(profileId, playerId, cleanName)
                cachedPlayerNames.value = cachedPlayerNames.value + (playerId to cleanName)
            }
            stakeoutManager?.toggleStar(accountId, playerId, currentlyStaked)
            val myEntry = targetDao.getTargets(accountId).firstOrNull { it.playerId == playerId }
            if (myEntry != null) {
                targetDao.upsertTarget(myEntry.copy(watched = !currentlyStaked, name = cleanName ?: myEntry.name))
            }
        }
    }

    fun updateStakeoutAlerts(stakeout: StakeoutEntity) {
        scope.launch {
            stakeoutManager?.updateStakeoutPrefs(stakeout)
        }
    }

    fun toggleWatchTarget(item: TargetItem) {
        toggleStakeout(item.playerId, item.name)
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

    private suspend fun refreshTargetsLive(
        accountId: String,
        ids: List<Long>,
        bypassCooldown: Boolean = false
    ) {
        val profileId = activeProfile.value.profile?.id ?: "default"
        isRefreshing.value = true
        statusMessage.value = null
        val now = clock.nowEpochMs()
        val updatedMap = liveProfiles.value.toMutableMap()
        val updatedFlights = stakeoutFlights.value.toMutableMap()
        val harvestedNames = mutableMapOf<Long, String>()
        val cappedIds = ids.take(MAX_TRACKED_REFRESH)
        var refreshedCount = 0

        for (id in cappedIds) {
            val prev = lastTargetRefreshMs[id]
            if (!bypassCooldown && prev != null && (now - prev) < MIN_PER_TARGET_INTERVAL_MS) {
                continue
            }
            when (val res = tornApi.getOtherProfile(accountId, id, RequestPriority.USER, forceRefresh = true)) {
                is ApiResult.Success -> {
                    val prof = res.data
                    updatedMap[id] = prof
                    lastTargetRefreshMs[id] = clock.nowEpochMs()
                    refreshedCount++
                    if (!PlayerNameCache.isPlaceholderName(prof.name, id)) {
                        harvestedNames[id] = prof.name
                    }
                    if (prof.status.state == StatusKind.TRAVELING) {
                        val est = flightRepository?.estimateForMember(
                            profileId = profileId,
                            playerId = id,
                            description = prof.status.description,
                            planeTypeApi = prof.status.planeImageType,
                            apiUntilSec = prof.status.untilEpochSec
                        )
                        if (est != null) updatedFlights[id] = est
                    } else {
                        updatedFlights.remove(id)
                    }
                    val myEntry = targetDao.getTargets(accountId).firstOrNull { it.playerId == id }
                    if (myEntry != null && myEntry.watched && prof.status.state == StatusKind.HOSPITAL && prof.status.untilEpochSec != null) {
                        scheduleWatchAlertIfNeeded(
                            accountId,
                            TargetItem(
                                playerId = id,
                                name = prof.name,
                                level = prof.level,
                                status = prof.status.state,
                                hospitalLeavesAtSec = prof.status.untilEpochSec,
                                lastActionSec = prof.lastActionTimestampSec,
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
                is ApiResult.Failure -> {
                    statusMessage.value = res.error.message
                    break
                }
            }
        }

        if (harvestedNames.isNotEmpty()) {
            playerNameCache?.recordNames(profileId, harvestedNames)
            cachedPlayerNames.value = cachedPlayerNames.value + harvestedNames
        }
        liveProfiles.value = updatedMap
        stakeoutFlights.value = updatedFlights
        if (uiState.value.ffEnabled) {
            fetchFfStats(accountId, cappedIds)
        }
        if (statusMessage.value == null) {
            statusMessage.value = "Refreshed $refreshedCount player(s)."
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
                    body = "Target leaves hospital now.",
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
