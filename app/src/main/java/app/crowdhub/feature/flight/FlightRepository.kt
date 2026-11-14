package app.crowdhub.feature.flight

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
    private val activeFlights = ConcurrentHashMap<Long, ActiveFlightTrack>()
    private val recentlyLanded = ConcurrentHashMap<Long, Long>()
    private val observedPlaneTypeCounts = ConcurrentHashMap<String, Int>()
    private val unknownPlaces = ConcurrentHashMap.newKeySet<String>()

    fun observePlaneTypeMap(): Flow<List<PlaneTypeMapEntity>> = dao.observePlaneTypeMap()
    fun observeRecentObservations(): Flow<List<FlightObservationEntity>> = dao.observeRecentObservations()

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
        mutex.withLock {
            recentlyLanded.remove(playerId)
            val existing = activeFlights[playerId]
            if (existing == null) {
                activeFlights[playerId] = ActiveFlightTrack(
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
            } else if (!planeTypeApi.isNullOrBlank() && existing.planeTypeApi == null) {
                activeFlights[playerId] = existing.copy(planeTypeApi = planeTypeApi)
            }
        }
    }

    suspend fun onLanded(
        playerId: Long,
        landedPrevPollAtMs: Long,
        landedDetectedAtMs: Long
    ) {
        val track = mutex.withLock {
            recentlyLanded[playerId] = landedDetectedAtMs
            activeFlights.remove(playerId)
        } ?: return

        val depart = track.departObservedWindow
        val obsMinMs = depart?.let { (landedPrevPollAtMs - it.second).coerceAtLeast(0L) }
        val obsMaxMs = depart?.let { (landedDetectedAtMs - it.first).coerceAtLeast(0L) }

        val map = dao.getPlaneTypeMap().associateBy { it.apiValue }
        val category = track.planeTypeApi?.let { map[it]?.category }?.let { PlaneCategory.fromRaw(it) }
            ?: PlaneCategory.UNKNOWN

        var inferredBook: Boolean? = null
        if (obsMinMs != null && obsMaxMs != null && category != PlaneCategory.UNKNOWN) {
            inferredBook = estimator.inferBookFromObservation(track.place, category, obsMinMs, obsMaxMs)
            if (inferredBook != null) {
                dao.upsertFlightProfile(
                    PlayerFlightProfileEntity(
                        playerId = playerId,
                        hasBook = inferredBook,
                        lastObservedAt = landedDetectedAtMs
                    )
                )
            }
        }

        dao.insertObservation(
            FlightObservationEntity(
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
        dao.trimObservationsForPlayer(playerId)
        dao.pruneOldObservations(clock.nowEpochMs() - 30L * 86_400_000L)
    }

    suspend fun estimateForMember(
        playerId: Long,
        description: String,
        planeTypeApi: String?,
        apiUntilSec: Long?
    ): FlightEstimate? {
        val nowMs = clock.nowEpochMs()
        val track = activeFlights[playerId]
        val parsed = FlightTable.parseDescription(description)
        val place = track?.place ?: parsed.place
        val direction = track?.direction ?: parsed.direction
        val resolvedPlaneType = planeTypeApi ?: track?.planeTypeApi

        val map = dao.getPlaneTypeMap().associateBy { it.apiValue }
        val category = resolvedPlaneType?.let { map[it]?.category }?.let { PlaneCategory.fromRaw(it) }
            ?: PlaneCategory.UNKNOWN

        val hasBook = validBookStatus(dao.getFlightProfile(playerId), nowMs)

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

    fun getPostLandingGraceEndsAtMs(playerId: Long): Long? {
        val landedAt = recentlyLanded[playerId] ?: return null
        val endsAt = landedAt + 15_000L
        return if (endsAt > clock.nowEpochMs()) endsAt else null
    }

    suspend fun getObservationsForPlayer(playerId: Long): List<FlightObservationEntity> =
        dao.getObservationsForPlayer(playerId)

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
