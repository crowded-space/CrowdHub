package app.crowdhub.feature.flight

import app.crowdhub.core.db.ActiveFlightEntity
import app.crowdhub.core.db.FlightObservationEntity
import app.crowdhub.core.db.PlaneTypeMapEntity
import app.crowdhub.core.db.PlayerFlightProfileEntity
import app.crowdhub.core.db.WarAndFlightDao
import app.crowdhub.core.network.Clock
import app.crowdhub.provider.torn.FlightTable
import app.crowdhub.provider.torn.PlaneCategory
import app.crowdhub.provider.torn.TravelDirection
import app.crowdhub.provider.torn.TravelPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class ActiveFlightTrack(
    val playerId: Long,
    val direction: TravelDirection,
    val place: TravelPlace,
    val planeTypeApi: String?,
    val departObservedWindow: Pair<Long, Long>?,
    val firstSeenAtMs: Long
)

class FlightRepository(
    private val dao: WarAndFlightDao,
    private val clock: Clock,
    val estimator: FlightEstimator = FlightEstimator()
) {
    private val mutex = Mutex()
    private val activeFlightsByProfile = ConcurrentHashMap<String, ConcurrentHashMap<Long, ActiveFlightTrack>>()
    private val recentlyLandedByProfile = ConcurrentHashMap<String, ConcurrentHashMap<Long, Long>>()
    private val hydratedProfiles = ConcurrentHashMap.newKeySet<String>()
    private val observedPlaneTypeCounts = ConcurrentHashMap<String, Int>()
    private val unknownPlaces = ConcurrentHashMap.newKeySet<String>()

    private suspend fun ensureHydratedLocked(profileId: String) {
        if (hydratedProfiles.contains(profileId)) return
        val profileMap = activeFlightsByProfile.getOrPut(profileId) { ConcurrentHashMap() }
        val stored = runCatching { dao.getAllActiveFlights(profileId) }.getOrDefault(emptyList())
        for (row in stored) {
            val place = runCatching { TravelPlace.valueOf(row.place) }.getOrDefault(TravelPlace.UNKNOWN)
            val direction = runCatching { TravelDirection.valueOf(row.direction) }.getOrDefault(TravelDirection.UNKNOWN)
            val window = if (row.departPrevPollMs != null && row.departDetectedMs != null) {
                row.departPrevPollMs to row.departDetectedMs
            } else {
                null
            }
            profileMap.putIfAbsent(
                row.playerId,
                ActiveFlightTrack(
                    playerId = row.playerId,
                    direction = direction,
                    place = place,
                    planeTypeApi = row.planeTypeApi,
                    departObservedWindow = window,
                    firstSeenAtMs = row.firstSeenAtMs
                )
            )
        }
        hydratedProfiles.add(profileId)
    }

    suspend fun clearProfileState(profileId: String) {
        mutex.withLock {
            activeFlightsByProfile.remove(profileId)
            recentlyLandedByProfile.remove(profileId)
            hydratedProfiles.remove(profileId)
        }
    }

    private fun ActiveFlightTrack.toEntity(profileId: String) = ActiveFlightEntity(
        profileId = profileId,
        playerId = playerId,
        direction = direction.name,
        place = place.name,
        planeTypeApi = planeTypeApi,
        departPrevPollMs = departObservedWindow?.first,
        departDetectedMs = departObservedWindow?.second,
        firstSeenAtMs = firstSeenAtMs
    )

    fun observePlaneTypeMap(): Flow<List<PlaneTypeMapEntity>> = dao.observePlaneTypeMap()
    fun observeRecentObservations(profileId: String): Flow<List<FlightObservationEntity>> =
        dao.observeRecentObservations(profileId)

    fun getObservedPlaneTypeCounts(): Map<String, Int> = observedPlaneTypeCounts.toMap()
    fun getUnknownPlaces(): Set<String> = unknownPlaces.toSet()

    suspend fun setPlaneTypeMapping(apiValue: String, category: PlaneCategory) {
        val trimmed = apiValue.trim()
        if (trimmed.isEmpty()) return
        dao.upsertPlaneTypeMapping(
            PlaneTypeMapEntity(
                apiValue = trimmed,
                category = category.name,
                source = "USER"
            )
        )
    }

    suspend fun deletePlaneTypeMapping(apiValue: String) {
        dao.deletePlaneTypeMapping(apiValue)
    }

    suspend fun onTravelObserved(
        profileId: String,
        playerId: Long,
        description: String,
        planeTypeApi: String?,
        prevPollAtMs: Long?,
        detectedAtMs: Long,
        departureObserved: Boolean
    ) {
        val parsed = FlightTable.parseDescription(description)
        if (parsed.place == TravelPlace.UNKNOWN && description.isNotBlank()) {
            unknownPlaces.add(description)
        }
        if (!planeTypeApi.isNullOrBlank()) {
            observedPlaneTypeCounts[planeTypeApi] = (observedPlaneTypeCounts[planeTypeApi] ?: 0) + 1
        }
        var updatedTrack: ActiveFlightTrack? = null
        mutex.withLock {
            ensureHydratedLocked(profileId)
            recentlyLandedByProfile[profileId]?.remove(playerId)
            val profileFlights = activeFlightsByProfile.getOrPut(profileId) { ConcurrentHashMap() }
            val existing = profileFlights[playerId]
            val legChanged = existing != null && (
                (parsed.place != TravelPlace.UNKNOWN && existing.place != parsed.place) ||
                    (parsed.direction != TravelDirection.UNKNOWN && existing.direction != parsed.direction)
                )
            if (existing == null || legChanged) {
                val track = ActiveFlightTrack(
                    playerId = playerId,
                    direction = parsed.direction,
                    place = parsed.place,
                    planeTypeApi = planeTypeApi,
                    departObservedWindow = if (departureObserved && prevPollAtMs != null) {
                        prevPollAtMs to detectedAtMs
                    } else {
                        null
                    },
                    firstSeenAtMs = detectedAtMs
                )
                profileFlights[playerId] = track
                updatedTrack = track
            } else {
                val resolvedPlane = if (!planeTypeApi.isNullOrBlank() && existing.planeTypeApi == null) {
                    planeTypeApi
                } else {
                    existing.planeTypeApi
                }
                val resolvedWindow = if (existing.departObservedWindow == null && departureObserved && prevPollAtMs != null) {
                    prevPollAtMs to detectedAtMs
                } else {
                    existing.departObservedWindow
                }
                if (resolvedPlane != existing.planeTypeApi || resolvedWindow != existing.departObservedWindow) {
                    val track = existing.copy(
                        planeTypeApi = resolvedPlane,
                        departObservedWindow = resolvedWindow
                    )
                    profileFlights[playerId] = track
                    updatedTrack = track
                }
            }
        }
        updatedTrack?.let { runCatching { dao.upsertActiveFlight(it.toEntity(profileId)) } }
    }

    suspend fun onLanded(
        profileId: String,
        playerId: Long,
        landedPrevPollAtMs: Long,
        landedDetectedAtMs: Long
    ) {
        val track = mutex.withLock {
            ensureHydratedLocked(profileId)
            recentlyLandedByProfile.getOrPut(profileId) { ConcurrentHashMap() }[playerId] = landedDetectedAtMs
            activeFlightsByProfile[profileId]?.remove(playerId)
        }
        runCatching { dao.deleteActiveFlight(profileId, playerId) }
        if (track == null) return

        val depart = track.departObservedWindow
        val obsMinMs = depart?.let { (landedPrevPollAtMs - it.second).coerceAtLeast(0L) }
        val obsMaxMs = depart?.let { (landedDetectedAtMs - it.first).coerceAtLeast(0L) }

        val map = dao.getPlaneTypeMap().associateBy { it.apiValue }
        val category = track.planeTypeApi?.let { resolveCategory(map, it) } ?: PlaneCategory.UNKNOWN

        var inferredBook: Boolean? = null
        if (obsMinMs != null && obsMaxMs != null && category != PlaneCategory.UNKNOWN) {
            inferredBook = estimator.inferBookFromObservation(track.place, category, obsMinMs, obsMaxMs)
            if (inferredBook != null) {
                dao.upsertFlightProfile(
                    PlayerFlightProfileEntity(
                        profileId = profileId,
                        playerId = playerId,
                        hasBook = inferredBook,
                        lastObservedAt = landedDetectedAtMs
                    )
                )
            }
        }

        dao.insertObservation(
            FlightObservationEntity(
                profileId = profileId,
                playerId = playerId,
                direction = if (track.direction == TravelDirection.RETURN) "RETURN" else "OUT",
                place = track.place.name,
                planeType = track.planeTypeApi,
                prevPollAt = depart?.first,
                detectedAt = depart?.second ?: track.firstSeenAtMs,
                landedPrevPollAt = landedPrevPollAtMs,
                landedDetectedAt = landedDetectedAtMs,
                observedMinMs = obsMinMs,
                observedMaxMs = obsMaxMs,
                inferredBook = inferredBook
            )
        )
        dao.trimObservationsForPlayer(profileId, playerId)
        dao.pruneOldObservations(clock.nowEpochMs() - 30L * 86_400_000L)
    }

    suspend fun estimateForMember(
        profileId: String,
        playerId: Long,
        description: String,
        planeTypeApi: String?,
        apiUntilSec: Long?
    ): FlightEstimate? {
        val nowMs = clock.nowEpochMs()
        val track = mutex.withLock {
            ensureHydratedLocked(profileId)
            activeFlightsByProfile[profileId]?.get(playerId)
        }
        val parsed = FlightTable.parseDescription(description)
        val place = track?.place?.takeIf { it != TravelPlace.UNKNOWN } ?: parsed.place
        val direction = track?.direction?.takeIf { it != TravelDirection.UNKNOWN } ?: parsed.direction
        val resolvedPlaneType = planeTypeApi ?: track?.planeTypeApi

        val map = dao.getPlaneTypeMap().associateBy { it.apiValue }
        val category = resolvedPlaneType?.let { resolveCategory(map, it) } ?: PlaneCategory.UNKNOWN

        val hasBook = validBookStatus(dao.getFlightProfile(profileId, playerId), nowMs)

        return estimator.estimate(
            place = place,
            direction = direction,
            category = category,
            hasBook = hasBook,
            departObservedWindow = track?.departObservedWindow,
            firstSeenAtMs = track?.firstSeenAtMs ?: nowMs,
            apiUntilSec = apiUntilSec,
            nowMs = nowMs
        )
    }

    fun getPostLandingGraceEndsAtMs(profileId: String, playerId: Long): Long? {
        val landedAt = recentlyLandedByProfile[profileId]?.get(playerId) ?: return null
        val endsAt = landedAt + 15_000L
        return if (endsAt > clock.nowEpochMs()) endsAt else null
    }

    suspend fun getObservationsForPlayer(profileId: String, playerId: Long): List<FlightObservationEntity> =
        dao.getObservationsForPlayer(profileId, playerId)

    private fun resolveCategory(
        userMap: Map<String, PlaneTypeMapEntity>,
        apiValue: String
    ): PlaneCategory {
        val normKey = apiValue.trim()
        val fromDb = (userMap[normKey] ?: userMap[normKey.lowercase()])
            ?.let { PlaneCategory.fromRaw(it.category) }
            ?.takeIf { it != PlaneCategory.UNKNOWN }
        return fromDb ?: FlightTable.defaultCategoryForPlaneType(normKey)
    }

    private fun validBookStatus(profile: PlayerFlightProfileEntity?, nowMs: Long): Boolean? {
        if (profile == null || profile.hasBook == null || profile.lastObservedAt == null) return null
        val age = nowMs - profile.lastObservedAt
        val maxAge = if (profile.hasBook) {
            FlightEstimator.BOOK_TRUE_VALIDITY_MS
        } else {
            FlightEstimator.BOOK_FALSE_VALIDITY_MS
        }
        return if (age <= maxAge) profile.hasBook else null
    }
}
