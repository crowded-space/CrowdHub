package app.crowdhub.feature.faction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.crowdhub.core.db.TargetDao
import app.crowdhub.core.db.TargetEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.feature.war.StakeoutManager
import app.crowdhub.provider.torn.ArmoryCategory
import app.crowdhub.provider.torn.TornFactionMember
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FactionViewModel(
    private val profileRepository: ProfileRepository,
    private val factionRepository: FactionRepository,
    private val stakeoutManager: StakeoutManager,
    private val targetDao: TargetDao,
    private val settingsStore: SettingsStore,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(FactionUiState())
    val uiState: StateFlow<FactionUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(FactionInnerTab.MEMBERS)
    val selectedTab: StateFlow<FactionInnerTab> = _selectedTab.asStateFlow()

    private var activeProfile = ActiveProfileState(null, null, emptyList())
    private var starredIds: Set<Long> = emptySet()
    private var rawMembers: List<TornFactionMember> = emptyList()

    init {
        viewModelScope.launch {
            profileRepository.observeActiveState().collect { state ->
                val prevId = activeProfile.primaryAccount?.id
                val prevProfileId = activeProfile.profile?.id
                activeProfile = state
                val acct = state.primaryAccount
                if (acct != null && (acct.id != prevId || state.profile?.id != prevProfileId)) {
                    resetAccountState(acct.id, acct.externalId?.toLongOrNull())
                    val pid = state.profile?.id ?: "default"
                    launch {
                        stakeoutManager.observeStakeouts(acct.id).collect { list ->
                            starredIds = list.map { it.playerId }.toSet()
                            if (rawMembers.isNotEmpty()) {
                                val enriched = factionRepository.enrichMembers(pid, rawMembers, starredIds)
                                _uiState.update { st ->
                                    st.copy(members = st.members.copy(data = enriched))
                                }
                            }
                        }
                    }
                    refreshForTab(_selectedTab.value, forceRefresh = false)
                }
            }
        }
    }

    private fun resetAccountState(accountId: String, playerId: Long?) {
        rawMembers = emptyList()
        _uiState.value = FactionUiState(
            accountId = accountId,
            ownPlayerId = playerId
        )
    }

    fun selectTab(tab: FactionInnerTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        if (tab != FactionInnerTab.WAR) {
            refreshForTab(tab, forceRefresh = false)
        }
    }

    fun setMemberSearch(query: String) {
        _uiState.update { it.copy(memberSearch = query) }
    }

    fun setMemberFilter(filter: FactionMemberFilter) {
        _uiState.update { it.copy(memberFilter = filter) }
    }

    fun setMemberSort(sort: FactionMemberSort) {
        _uiState.update { it.copy(memberSort = sort) }
    }

    fun setAttackFilter(filter: FactionAttackFilter) {
        _uiState.update { it.copy(attackFilter = filter) }
    }

    fun setArmoryCategory(category: ArmoryCategory) {
        _uiState.update { it.copy(armoryCategory = category) }
    }

    fun setArmorySearch(query: String) {
        _uiState.update { it.copy(armorySearch = query) }
    }

    fun setOcFilter(filter: FactionOcFilter) {
        _uiState.update { it.copy(ocFilter = filter) }
    }

    fun toggleStar(playerId: Long) {
        val acctId = activeProfile.primaryAccount?.id ?: return
        viewModelScope.launch {
            stakeoutManager.toggleStar(acctId, playerId, playerId in starredIds)
        }
    }

    fun addToTargets(playerId: Long, name: String, note: String) {
        val acctId = activeProfile.primaryAccount?.id ?: return
        viewModelScope.launch {
            targetDao.upsertTarget(
                TargetEntity(
                    accountId = acctId,
                    playerId = playerId,
                    name = name,
                    note = note,
                    source = "FACTION",
                    addedAt = clock.nowEpochMs(),
                    watched = false
                )
            )
        }
    }

    fun refreshCurrentTab(forceRefresh: Boolean) {
        refreshForTab(_selectedTab.value, forceRefresh)
    }

    fun refreshForTab(tab: FactionInnerTab, forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = activeProfile
            val acct = state.primaryAccount ?: return@launch
            if (state.accountStatus == AccountStatus.INVALID || state.accountStatus == AccountStatus.PAUSED) {
                return@launch
            }
            val prefs = settingsStore.current()
            val priority = if (prefs.watchModeActive) RequestPriority.WATCH else RequestPriority.USER

            val factionId = ensureOwnFactionId(acct.id, priority, forceRefresh)
            if (factionId == null) {
                if (_uiState.value.notInFaction) {
                    _uiState.update {
                        it.copy(
                            basic = FactionSectionState(isLoading = false),
                            members = FactionSectionState(isLoading = false),
                            chain = FactionSectionState(isLoading = false),
                            attacks = FactionSectionState(isLoading = false),
                            armory = FactionSectionState(isLoading = false),
                            balance = FactionSectionState(isLoading = false),
                            oc = FactionSectionState(isLoading = false)
                        )
                    }
                }
                return@launch
            }

            // Always refresh header summary (basic + chain) lightly alongside active tab
            val basicDeferred = async { loadBasic(acct.id, factionId, priority, forceRefresh) }
            val chainDeferred = async { loadChain(acct.id, factionId, priority, forceRefresh) }

            when (tab) {
                FactionInnerTab.MEMBERS -> loadMembers(acct.id, factionId, priority, forceRefresh)
                FactionInnerTab.WAR -> Unit
                FactionInnerTab.CHAIN -> loadAttacks(acct.id, priority, forceRefresh)
                FactionInnerTab.ARMORY -> {
                    val armoryDef = async { loadArmory(acct.id, priority, forceRefresh) }
                    val balanceDef = async { loadBalance(acct.id, priority, forceRefresh) }
                    armoryDef.await()
                    balanceDef.await()
                }
                FactionInnerTab.OC -> loadOrganizedCrimes(acct.id, factionId, priority, forceRefresh)
            }

            basicDeferred.await()
            chainDeferred.await()
        }
    }

    private suspend fun ensureOwnFactionId(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): Long? {
        val cachedId = _uiState.value.ownFactionId
        if (!forceRefresh && cachedId != null && cachedId > 0L) return cachedId

        return when (val res = factionRepository.resolveOwnFactionId(accountId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                val fid = res.data?.takeIf { it > 0L }
                _uiState.update {
                    it.copy(
                        ownFactionId = fid,
                        notInFaction = fid == null
                    )
                }
                fid
            }
            is ApiResult.Failure -> {
                val staleId = res.staleData?.takeIf { it > 0L } ?: cachedId
                if (staleId != null) {
                    _uiState.update { it.copy(ownFactionId = staleId, notInFaction = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            basic = it.basic.copy(isLoading = false, error = res.error),
                            members = it.members.copy(isLoading = false, error = res.error)
                        )
                    }
                }
                staleId
            }
        }
    }

    private suspend fun loadBasic(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        if (_uiState.value.basic.data == null) {
            _uiState.update { it.copy(basic = it.basic.copy(isLoading = true)) }
        }
        when (val res = factionRepository.fetchBasic(accountId, factionId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        basic = FactionSectionState(
                            isLoading = false,
                            data = res.data,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                _uiState.update { st ->
                    st.copy(
                        basic = mapFailureSection(
                            current = st.basic,
                            error = res.error,
                            staleData = res.staleData,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadMembers(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        if (_uiState.value.members.data == null) {
            _uiState.update { it.copy(members = it.members.copy(isLoading = true, error = null)) }
        }
        when (val res = factionRepository.fetchMembersAndTrackFlights(accountId, factionId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                rawMembers = res.data
                val pid = activeProfile.profile?.id ?: "default"
                val enriched = factionRepository.enrichMembers(pid, res.data, starredIds)
                _uiState.update {
                    it.copy(
                        members = FactionSectionState(
                            isLoading = false,
                            data = enriched,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                val staleRaw = res.staleData
                val pid = activeProfile.profile?.id ?: "default"
                val staleEnriched = if (!staleRaw.isNullOrEmpty()) {
                    rawMembers = staleRaw
                    factionRepository.enrichMembers(pid, staleRaw, starredIds)
                } else null
                _uiState.update { st ->
                    st.copy(
                        members = mapFailureSection(
                            current = st.members,
                            error = res.error,
                            staleData = staleEnriched,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadChain(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        when (val res = factionRepository.fetchChain(accountId, factionId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        chain = FactionSectionState(
                            isLoading = false,
                            data = res.data,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                _uiState.update { st ->
                    st.copy(
                        chain = mapFailureSection(
                            current = st.chain,
                            error = res.error,
                            staleData = res.staleData,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadAttacks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        if (_uiState.value.attacks.data == null) {
            _uiState.update { it.copy(attacks = it.attacks.copy(isLoading = true, error = null)) }
        }
        when (val res = factionRepository.fetchAttacks(accountId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        attacks = FactionSectionState(
                            isLoading = false,
                            data = res.data,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                _uiState.update { st ->
                    st.copy(
                        attacks = mapFailureSection(
                            current = st.attacks,
                            error = res.error,
                            staleData = res.staleData,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadArmory(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        if (_uiState.value.armory.data == null) {
            _uiState.update { it.copy(armory = it.armory.copy(isLoading = true, error = null)) }
        }
        when (val res = factionRepository.fetchArmory(accountId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        armory = FactionSectionState(
                            isLoading = false,
                            data = res.data,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                _uiState.update { st ->
                    st.copy(
                        armory = mapFailureSection(
                            current = st.armory,
                            error = res.error,
                            staleData = res.staleData,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadBalance(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        when (val res = factionRepository.fetchBalance(accountId, priority, forceRefresh)) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        balance = FactionSectionState(
                            isLoading = false,
                            data = res.data,
                            fetchedAtMs = res.fetchedAtMs,
                            isStale = res.isStale
                        )
                    )
                }
            }
            is ApiResult.Failure -> {
                _uiState.update { st ->
                    st.copy(
                        balance = mapFailureSection(
                            current = st.balance,
                            error = res.error,
                            staleData = res.staleData,
                            staleAtMs = res.staleFetchedAtMs
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadOrganizedCrimes(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean
    ) {
        if (_uiState.value.oc.data == null) {
            _uiState.update { it.copy(oc = it.oc.copy(isLoading = true, error = null)) }
        }
        val section = factionRepository.fetchOrganizedCrimes(
            accountId = accountId,
            factionId = factionId,
            ownPlayerId = _uiState.value.ownPlayerId,
            priority = priority,
            forceRefresh = forceRefresh
        )
        _uiState.update { it.copy(oc = section) }
    }

    private fun <T> mapFailureSection(
        current: FactionSectionState<T>,
        error: ApiError,
        staleData: T?,
        staleAtMs: Long?
    ): FactionSectionState<T> {
        val restricted = error is ApiError.AccessLevel
        val resolvedData = staleData ?: current.data
        val resolvedFetchedAt = staleAtMs ?: current.fetchedAtMs.takeIf { it > 0L } ?: clock.nowEpochMs()
        return FactionSectionState(
            isLoading = false,
            data = resolvedData,
            fetchedAtMs = resolvedFetchedAt,
            isStale = resolvedData != null,
            accessRestricted = restricted,
            error = error
        )
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val factionRepository: FactionRepository,
        private val stakeoutManager: StakeoutManager,
        private val targetDao: TargetDao,
        private val settingsStore: SettingsStore,
        private val clock: Clock
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FactionViewModel(
                profileRepository = profileRepository,
                factionRepository = factionRepository,
                stakeoutManager = stakeoutManager,
                targetDao = targetDao,
                settingsStore = settingsStore,
                clock = clock
            ) as T
        }
    }
}
