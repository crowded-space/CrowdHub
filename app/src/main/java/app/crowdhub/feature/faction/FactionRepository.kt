package app.crowdhub.feature.faction

import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.feature.flight.FlightRepository
import app.crowdhub.feature.war.StakeoutManager
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornArmoryItem
import app.crowdhub.provider.torn.TornFactionAttack
import app.crowdhub.provider.torn.TornFactionBalance
import app.crowdhub.provider.torn.TornFactionBasic
import app.crowdhub.provider.torn.TornFactionChain
import app.crowdhub.provider.torn.TornFactionMember
import app.crowdhub.provider.torn.TornOrganizedCrime

class FactionRepository(
    private val tornApi: TornApi,
    private val flightRepository: FlightRepository,
    private val stakeoutManager: StakeoutManager,
    private val clock: Clock,
    private val playerNameCache: app.crowdhub.core.profile.PlayerNameCache? = null,
    private val resolveProfileId: suspend (String) -> String = { "default" }
) {
    suspend fun resolveOwnFactionId(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<Long?> = tornApi.getOwnFactionId(accountId, priority, forceRefresh)

    suspend fun fetchBasic(
        accountId: String,
        factionId: Long?,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<TornFactionBasic> = tornApi.getFactionBasic(accountId, factionId, priority, forceRefresh)

    suspend fun fetchMembersAndTrackFlights(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<List<TornFactionMember>> {
        val res = tornApi.getFactionMembers(accountId, factionId, priority, forceRefresh)
        if (res is ApiResult.Success) {
            val profileId = resolveProfileId(accountId)
            playerNameCache?.recordNames(profileId, res.data.associate { it.playerId to it.name })
            runCatching {
                stakeoutManager.processFactionMembers(accountId, factionId, res.data)
            }
        }
        return res
    }

    suspend fun enrichMembers(
        profileId: String,
        members: List<TornFactionMember>,
        starredIds: Set<Long>
    ): List<FactionMemberItem> {
        return members.map { member ->
            val est = if (member.status.state == StatusKind.TRAVELING) {
                flightRepository.estimateForMember(
                    profileId = profileId,
                    playerId = member.playerId,
                    description = member.status.description,
                    planeTypeApi = member.status.planeImageType,
                    apiUntilSec = member.status.untilEpochSec
                )
            } else {
                null
            }
            FactionMemberItem(
                member = member,
                flightEstimate = est,
                postLandingGraceEndMs = flightRepository.getPostLandingGraceEndsAtMs(profileId, member.playerId),
                isStarred = member.playerId in starredIds
            )
        }
    }

    suspend fun fetchChain(
        accountId: String,
        factionId: Long?,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<TornFactionChain> = tornApi.getFactionChain(accountId, factionId, priority, forceRefresh)

    suspend fun fetchAttacks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<List<TornFactionAttack>> {
        val res = tornApi.getFactionAttacks(accountId, priority, forceRefresh)
        if (res is ApiResult.Success) {
            val profileId = resolveProfileId(accountId)
            val discovered = mutableMapOf<Long, String>()
            for (atk in res.data) {
                if (atk.attackerId != null && atk.attackerName.isNotBlank() && !atk.attackerName.equals("Someone", true)) {
                    discovered[atk.attackerId] = atk.attackerName
                }
                if (atk.defenderId != null && atk.defenderName.isNotBlank() && !atk.defenderName.equals("Someone", true)) {
                    discovered[atk.defenderId] = atk.defenderName
                }
            }
            if (discovered.isNotEmpty()) {
                playerNameCache?.recordNames(profileId, discovered)
            }
        }
        return res
    }

    suspend fun fetchArmory(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<List<TornArmoryItem>> = tornApi.getFactionArmory(accountId, priority, forceRefresh)

    suspend fun fetchBalance(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<TornFactionBalance> = tornApi.getFactionBalance(accountId, priority, forceRefresh)

    suspend fun fetchOrganizedCrimes(
        accountId: String,
        factionId: Long?,
        ownPlayerId: Long?,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): FactionSectionState<FactionOcData> {
        val factionRes = tornApi.getFactionCrimes(accountId, priority, forceRefresh)
        val selfRes = tornApi.getUserOrganizedCrime(accountId, priority, forceRefresh)

        val selfCrimeDirect = (selfRes as? ApiResult.Success)?.data
            ?: (selfRes as? ApiResult.Failure)?.staleData

        val profileId = resolveProfileId(accountId)

        return when (factionRes) {
            is ApiResult.Success -> {
                val resolvedCrimes = resolveOcPlayerNames(
                    profileId = profileId,
                    accountId = accountId,
                    factionId = factionId,
                    crimes = factionRes.data,
                    extraCrime = selfCrimeDirect,
                    priority = priority
                )
                val crimes = resolvedCrimes.first
                val resolvedSelfDirect = resolvedCrimes.second
                val matchedSelf = if (ownPlayerId != null) {
                    crimes.firstOrNull { crime ->
                        crime.slots.any { it.userId == ownPlayerId }
                    }
                } else {
                    null
                }
                FactionSectionState(
                    isLoading = false,
                    data = FactionOcData(
                        factionCrimes = crimes,
                        selfCrime = matchedSelf ?: resolvedSelfDirect,
                        factionCrimesRestricted = false
                    ),
                    fetchedAtMs = factionRes.fetchedAtMs,
                    isStale = factionRes.isStale
                )
            }
            is ApiResult.Failure -> {
                val restricted = factionRes.error is ApiError.AccessLevel
                val staleCrimes = factionRes.staleData
                if (!staleCrimes.isNullOrEmpty()) {
                    val (resolvedStale, resolvedSelf) = resolveOcPlayerNames(
                        profileId = profileId,
                        accountId = accountId,
                        factionId = factionId,
                        crimes = staleCrimes,
                        extraCrime = selfCrimeDirect,
                        priority = priority,
                        allowNetworkLookup = false
                    )
                    val matchedSelf = ownPlayerId?.let { pid ->
                        resolvedStale.firstOrNull { c -> c.slots.any { it.userId == pid } }
                    }
                    FactionSectionState(
                        isLoading = false,
                        data = FactionOcData(
                            factionCrimes = resolvedStale,
                            selfCrime = matchedSelf ?: resolvedSelf,
                            factionCrimesRestricted = restricted
                        ),
                        fetchedAtMs = factionRes.staleFetchedAtMs ?: clock.nowEpochMs(),
                        isStale = true,
                        accessRestricted = restricted,
                        error = factionRes.error
                    )
                } else if (selfRes is ApiResult.Success || selfCrimeDirect != null) {
                    val (_, resolvedSelf) = resolveOcPlayerNames(
                        profileId = profileId,
                        accountId = accountId,
                        factionId = factionId,
                        crimes = emptyList(),
                        extraCrime = selfCrimeDirect,
                        priority = priority,
                        allowNetworkLookup = selfRes is ApiResult.Success
                    )
                    val fetchedAt = (selfRes as? ApiResult.Success)?.fetchedAtMs
                        ?: (selfRes as? ApiResult.Failure)?.staleFetchedAtMs
                        ?: clock.nowEpochMs()
                    FactionSectionState(
                        isLoading = false,
                        data = FactionOcData(
                            factionCrimes = emptyList(),
                            selfCrime = resolvedSelf,
                            factionCrimesRestricted = restricted
                        ),
                        fetchedAtMs = fetchedAt,
                        isStale = selfRes is ApiResult.Failure,
                        accessRestricted = restricted,
                        error = if (restricted) null else factionRes.error
                    )
                } else {
                    FactionSectionState(
                        isLoading = false,
                        data = null,
                        accessRestricted = restricted,
                        error = factionRes.error
                    )
                }
            }
        }
    }

    private suspend fun resolveOcPlayerNames(
        profileId: String,
        accountId: String,
        factionId: Long?,
        crimes: List<TornOrganizedCrime>,
        extraCrime: TornOrganizedCrime?,
        priority: RequestPriority,
        allowNetworkLookup: Boolean = true
    ): Pair<List<TornOrganizedCrime>, TornOrganizedCrime?> {
        val allCrimes = if (extraCrime != null) crimes + extraCrime else crimes
        if (allCrimes.isEmpty()) return crimes to extraCrime

        val nameMap = mutableMapOf<Long, String>()
        val missingIds = linkedSetOf<Long>()

        for (crime in allCrimes) {
            for (slot in crime.slots) {
                val uid = slot.userId ?: continue
                val existing = slot.userName?.takeIf { !app.crowdhub.core.profile.PlayerNameCache.isPlaceholderName(it, uid) }
                if (existing != null) {
                    nameMap[uid] = existing
                } else {
                    missingIds.add(uid)
                }
            }
        }

        if (nameMap.isNotEmpty()) {
            playerNameCache?.recordNames(profileId, nameMap)
        }

        if (missingIds.isNotEmpty()) {
            val fromCache = playerNameCache?.getFreshNames(profileId, missingIds.toList()).orEmpty()
            nameMap.putAll(fromCache)
            missingIds.removeAll(fromCache.keys)
        }

        // Primary resolution source: /faction/{id}/members (returns member ID + name)
        if (missingIds.isNotEmpty() && factionId != null && factionId > 0L) {
            val membersRes = tornApi.getFactionMembers(
                accountId = accountId,
                factionId = factionId,
                priority = priority,
                forceRefresh = false
            )
            val members = (membersRes as? ApiResult.Success)?.data
                ?: (membersRes as? ApiResult.Failure)?.staleData
            if (!members.isNullOrEmpty()) {
                val memberNames = members.associate { it.playerId to it.name }
                playerNameCache?.recordNames(profileId, memberNames)
                for (id in missingIds.toList()) {
                    memberNames[id]?.let {
                        nameMap[id] = it
                        missingIds.remove(id)
                    }
                }
            }
        }

        // Stale cache fallback before hitting user/{id}/profile
        if (missingIds.isNotEmpty()) {
            val staleCached = playerNameCache?.getAnyCachedNames(profileId, missingIds.toList()).orEmpty()
            for ((id, name) in staleCached) {
                nameMap.putIfAbsent(id, name)
                if (!allowNetworkLookup) {
                    missingIds.remove(id)
                }
            }
        }

        // Throttled fallback for remaining unknown IDs (e.g., ex-members in historical OCs)
        if (allowNetworkLookup && missingIds.isNotEmpty()) {
            val newlyResolved = mutableMapOf<Long, String>()
            for (uid in missingIds.take(MAX_PROFILE_NAME_LOOKUPS)) {
                val res = tornApi.getOtherProfile(
                    accountId = accountId,
                    targetPlayerId = uid,
                    priority = priority,
                    forceRefresh = false,
                    ttlMs = app.crowdhub.core.profile.PlayerNameCache.TTL_MS
                )
                val profileName = (res as? ApiResult.Success)?.data?.name
                    ?.takeIf { !app.crowdhub.core.profile.PlayerNameCache.isPlaceholderName(it, uid) }
                if (profileName != null) {
                    nameMap[uid] = profileName
                    newlyResolved[uid] = profileName
                }
            }
            if (newlyResolved.isNotEmpty()) {
                playerNameCache?.recordNames(profileId, newlyResolved)
            }
        }

        fun applyNames(crime: TornOrganizedCrime): TornOrganizedCrime {
            var changed = false
            val updatedSlots = crime.slots.map { slot ->
                val uid = slot.userId
                val resolved = if (uid != null && (slot.userName.isNullOrBlank() || app.crowdhub.core.profile.PlayerNameCache.isPlaceholderName(slot.userName, uid))) {
                    nameMap[uid]
                } else {
                    null
                }
                if (resolved != null && resolved != slot.userName) {
                    changed = true
                    slot.copy(userName = resolved)
                } else {
                    slot
                }
            }
            return if (changed) crime.copy(slots = updatedSlots) else crime
        }

        val updatedCrimes = crimes.map(::applyNames)
        val updatedExtra = extraCrime?.let(::applyNames)
        return updatedCrimes to updatedExtra
    }

    companion object {
        private const val MAX_PROFILE_NAME_LOOKUPS = 10
    }
}
