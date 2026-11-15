package app.crowdhub.feature.flight

import app.crowdhub.provider.torn.FlightTable
import app.crowdhub.provider.torn.PlaneCategory
import app.crowdhub.provider.torn.TravelDirection
import app.crowdhub.provider.torn.TravelPlace
import kotlin.math.roundToLong

enum class FlightConfidence {
    HIGH,   // from API status.until
    GREEN,  // category known AND book known AND departure observed
    YELLOW, // departure observed but book OR category unknown
    GRAY    // departure not observed, place unknown, or hidden
}

data class FlightEstimate(
    val place: TravelPlace,
    val direction: TravelDirection,
    val category: PlaneCategory,
    val windowStartMs: Long,
    val windowEndMs: Long,
    val confidence: FlightConfidence,
    val isDelayed: Boolean,
    val overdueByMs: Long,
    val usedApiUntil: Boolean,
    val baseMinutesMin: Int? = null,
    val baseMinutesMax: Int? = null,
    val hasBook: Boolean? = null,
    val departWindowStartMs: Long? = null,
    val departWindowEndMs: Long? = null,
    val firstSeenAtMs: Long? = null,
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null
)

class FlightEstimator {
    fun estimate(
        place: TravelPlace,
        direction: TravelDirection,
        category: PlaneCategory,
        hasBook: Boolean?,
        departObservedWindow: Pair<Long, Long>?,
        firstSeenAtMs: Long,
        apiUntilSec: Long?,
        nowMs: Long
    ): FlightEstimate? {
        val allMins = FlightTable.allCategoryMinutes(place)
        val knownBaseMin = if (category != PlaneCategory.UNKNOWN) {
            FlightTable.baseMinutes(place, category)
        } else {
            null
        }
        val baseMin = knownBaseMin ?: allMins?.minOrNull()
        val baseMax = knownBaseMin ?: allMins?.maxOrNull()

        if (apiUntilSec != null && apiUntilSec * 1_000L > nowMs) {
            val untilMs = apiUntilSec * 1_000L
            return FlightEstimate(
                place = place,
                direction = direction,
                category = category,
                windowStartMs = untilMs,
                windowEndMs = untilMs,
                confidence = FlightConfidence.HIGH,
                isDelayed = false,
                overdueByMs = 0L,
                usedApiUntil = true,
                baseMinutesMin = baseMin,
                baseMinutesMax = baseMax,
                hasBook = hasBook,
                departWindowStartMs = departObservedWindow?.first,
                departWindowEndMs = departObservedWindow?.second,
                firstSeenAtMs = firstSeenAtMs
            )
        }

        if (place == TravelPlace.UNKNOWN) {
            return FlightEstimate(
                place = place,
                direction = direction,
                category = category,
                windowStartMs = nowMs,
                windowEndMs = nowMs + 60_000L,
                confidence = FlightConfidence.GRAY,
                isDelayed = false,
                overdueByMs = 0L,
                usedApiUntil = false,
                hasBook = hasBook,
                departWindowStartMs = departObservedWindow?.first,
                departWindowEndMs = departObservedWindow?.second,
                firstSeenAtMs = firstSeenAtMs
            )
        }

        val resolvedAllMins = allMins ?: return null
        val candidateBaseMins = if (category != PlaneCategory.UNKNOWN) {
            listOfNotNull(FlightTable.baseMinutes(place, category))
        } else {
            resolvedAllMins.toList()
        }
        if (candidateBaseMins.isEmpty()) return null

        val minBaseMs = candidateBaseMins.min() * 60_000.0
        val maxBaseMs = candidateBaseMins.max() * 60_000.0

        val minBookFactor = when (hasBook) {
            true -> BOOK_FACTOR
            false -> 1.0
            null -> BOOK_FACTOR
        }
        val maxBookFactor = when (hasBook) {
            true -> BOOK_FACTOR
            false -> 1.0
            null -> 1.0
        }

        val durMinMs = (minBaseMs * minBookFactor * (1.0 - VARIANCE)).roundToLong()
        val durMaxMs = (maxBaseMs * maxBookFactor * (1.0 + VARIANCE)).roundToLong()

        val baseStartMs: Long
        var baseEndMs: Long
        val baseConfidence: FlightConfidence

        if (departObservedWindow != null) {
            baseStartMs = departObservedWindow.first + durMinMs
            baseEndMs = departObservedWindow.second + durMaxMs
            baseConfidence = if (category != PlaneCategory.UNKNOWN && hasBook != null) {
                FlightConfidence.GREEN
            } else {
                FlightConfidence.YELLOW
            }
        } else {
            baseStartMs = nowMs
            baseEndMs = firstSeenAtMs + durMaxMs
            baseConfidence = FlightConfidence.GRAY
        }

        val overdueMs = nowMs - baseEndMs
        val isDelayed = overdueMs > OVERDUE_GRACE_MS
        if (isDelayed) {
            val steps = ((overdueMs - OVERDUE_GRACE_MS) / WIDEN_INTERVAL_MS) + 1L
            baseEndMs += (durMaxMs * 0.10 * steps).roundToLong()
        }

        return FlightEstimate(
            place = place,
            direction = direction,
            category = category,
            windowStartMs = baseStartMs.coerceAtMost(baseEndMs),
            windowEndMs = baseEndMs,
            confidence = baseConfidence,
            isDelayed = isDelayed,
            overdueByMs = overdueMs.coerceAtLeast(0L),
            usedApiUntil = false,
            baseMinutesMin = baseMin,
            baseMinutesMax = baseMax,
            hasBook = hasBook,
            departWindowStartMs = departObservedWindow?.first,
            departWindowEndMs = departObservedWindow?.second,
            firstSeenAtMs = firstSeenAtMs,
            minDurationMs = durMinMs,
            maxDurationMs = durMaxMs
        )
    }

    fun inferBookFromObservation(
        place: TravelPlace,
        category: PlaneCategory,
        observedMinMs: Long,
        observedMaxMs: Long
    ): Boolean? {
        if (place == TravelPlace.UNKNOWN || category == PlaneCategory.UNKNOWN) return null
        val baseMs = (FlightTable.baseMinutes(place, category) ?: return null) * 60_000.0

        val noBookMin = (baseMs * 1.0 * (1.0 - VARIANCE)).roundToLong()
        val noBookMax = (baseMs * 1.0 * (1.0 + VARIANCE)).roundToLong()
        val bookMin = (baseMs * BOOK_FACTOR * (1.0 - VARIANCE)).roundToLong()
        val bookMax = (baseMs * BOOK_FACTOR * (1.0 + VARIANCE)).roundToLong()

        val fitsNoBook = observedMaxMs >= noBookMin && observedMinMs <= noBookMax
        val fitsBook = observedMaxMs >= bookMin && observedMinMs <= bookMax

        return when {
            fitsBook && !fitsNoBook -> true
            fitsNoBook && !fitsBook -> false
            else -> null
        }
    }

    fun suggestCategoryFromObservation(
        place: TravelPlace,
        hasBook: Boolean?,
        observedMinMs: Long,
        observedMaxMs: Long
    ): PlaneCategory? {
        if (place == TravelPlace.UNKNOWN) return null
        val bookMults = when (hasBook) {
            true -> listOf(BOOK_FACTOR)
            false -> listOf(1.0)
            null -> listOf(BOOK_FACTOR, 1.0)
        }
        val matches = listOf(
            PlaneCategory.STANDARD,
            PlaneCategory.AIRSTRIP,
            PlaneCategory.WLT,
            PlaneCategory.BUSINESS
        ).filter { cat ->
            val base = (FlightTable.baseMinutes(place, cat) ?: return@filter false) * 60_000.0
            bookMults.any { bf ->
                val minD = (base * bf * (1.0 - VARIANCE)).roundToLong()
                val maxD = (base * bf * (1.0 + VARIANCE)).roundToLong()
                observedMaxMs >= minD && observedMinMs <= maxD
            }
        }
        return if (matches.size == 1) matches.first() else null
    }

    companion object {
        const val BOOK_FACTOR = 0.75
        const val VARIANCE = 0.03
        const val OVERDUE_GRACE_MS = 60_000L
        const val WIDEN_INTERVAL_MS = 300_000L
        const val BOOK_TRUE_VALIDITY_MS = 31L * 86_400_000L
        const val BOOK_FALSE_VALIDITY_MS = 7L * 86_400_000L
    }
}
