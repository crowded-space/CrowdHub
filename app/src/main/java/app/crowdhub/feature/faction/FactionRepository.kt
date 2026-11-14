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
    private val clock: Clock
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
            runCatching {
                stakeoutManager.processFactionMembers(accountId, factionId, res.data)
            }
        }
        return res
    }

    suspend fun enrichMembers(
        members: List<TornFactionMember>,
        starredIds: Set<Long>
    ): List<FactionMemberItem> {
        return members.map { member ->
            val est = if (member.status.state == StatusKind.TRAVELING) {
                flightRepository.estimateForMember(
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
                postLandingGraceEndMs = flightRepository.getPostLandingGraceEndsAtMs(member.playerId),
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
    ): ApiResult<List<TornFactionAttack>> = tornApi.getFactionAttacks(accountId, priority, forceRefresh)

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
        ownPlayerId: Long?,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): FactionSectionState<FactionOcData> {
        val factionRes = tornApi.getFactionCrimes(accountId, priority, forceRefresh)
        val selfRes = tornApi.getUserOrganizedCrime(accountId, priority, forceRefresh)

        val selfCrimeDirect = (selfRes as? ApiResult.Success)?.data
            ?: (selfRes as? ApiResult.Failure)?.staleData

        return when (factionRes) {
            is ApiResult.Success -> {
                val crimes = factionRes.data
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
                        selfCrime = matchedSelf ?: selfCrimeDirect,
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
                    val matchedSelf = ownPlayerId?.let { pid ->
                        staleCrimes.firstOrNull { c -> c.slots.any { it.userId == pid } }
                    }
                    FactionSectionState(
                        isLoading = false,
                        data = FactionOcData(
                            factionCrimes = staleCrimes,
                            selfCrime = matchedSelf ?: selfCrimeDirect,
                            factionCrimesRestricted = restricted
                        ),
                        fetchedAtMs = factionRes.staleFetchedAtMs ?: clock.nowEpochMs(),
                        isStale = true,
                        accessRestricted = restricted,
                        error = factionRes.error
                    )
                } else if (selfRes is ApiResult.Success || selfCrimeDirect != null) {
                    val fetchedAt = (selfRes as? ApiResult.Success)?.fetchedAtMs
                        ?: (selfRes as? ApiResult.Failure)?.staleFetchedAtMs
                        ?: clock.nowEpochMs()
                    FactionSectionState(
                        isLoading = false,
                        data = FactionOcData(
                            factionCrimes = emptyList(),
                            selfCrime = selfCrimeDirect,
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
}
