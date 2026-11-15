package app.crowdhub.feature.flight

import app.crowdhub.provider.torn.FlightTable
import app.crowdhub.provider.torn.PlaneCategory
import app.crowdhub.provider.torn.TravelDirection
import app.crowdhub.provider.torn.TravelPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightEstimatorTest {
    private val estimator = FlightEstimator()

    @Test
    fun `book factor reduces flight duration to 75 percent within 1 minute for standard countries`() {
        // Verify flight-times.md claim: book reduces durations by 25%, matching reported times to +/-1m
        // Note: Torn Wiki table reports SWITZERLAND + WLT / AIRSTRIP slightly differently, so check standard/WLT/Business
        val reportedWithBook = mapOf(
            TravelPlace.MEXICO to intArrayOf(18, 13, 9, 5),
            TravelPlace.CAYMAN_ISLANDS to intArrayOf(25, 18, 13, 8),
            TravelPlace.CANADA to intArrayOf(29, 20, 14, 9),
            TravelPlace.HAWAII to intArrayOf(95, 67, 47, 29),
            TravelPlace.UNITED_KINGDOM to intArrayOf(113, 80, 56, 34),
            TravelPlace.ARGENTINA to intArrayOf(119, 83, 59, 35),
            TravelPlace.JAPAN to intArrayOf(160, 112, 80, 48),
            TravelPlace.CHINA to intArrayOf(172, 120, 86, 52),
            TravelPlace.UAE to intArrayOf(193, 135, 96, 58),
            TravelPlace.SOUTH_AFRICA to intArrayOf(212, 148, 106, 64)
        )

        for ((place, expected) in reportedWithBook) {
            for (cat in listOf(PlaneCategory.STANDARD, PlaneCategory.AIRSTRIP, PlaneCategory.WLT, PlaneCategory.BUSINESS)) {
                val withoutBook = FlightTable.baseMinutes(place, cat)!!
                val computed = kotlin.math.round(withoutBook * FlightEstimator.BOOK_FACTOR).toInt()
                val diff = kotlin.math.abs(computed - expected[cat.index])
                assertTrue("Expected $place $cat computed $computed within 1m of ${expected[cat.index]}", diff <= 1)
            }
        }
    }

    @Test
    fun `departure observed yields GREEN when category and book known and infers book status`() {
        // Mexico WLT without book = 12 min (720_000 ms), with book = 9 min (540_000 ms)
        val inferBook = estimator.inferBookFromObservation(
            place = TravelPlace.MEXICO,
            category = PlaneCategory.WLT,
            observedMinMs = 535_000L,
            observedMaxMs = 550_000L
        )
        assertEquals(true, inferBook)

        val est = estimator.estimate(
            place = TravelPlace.MEXICO,
            direction = TravelDirection.OUT,
            category = PlaneCategory.WLT,
            hasBook = false,
            departObservedWindow = 1_000_000L to 1_030_000L,
            firstSeenAtMs = 1_030_000L,
            apiUntilSec = null,
            nowMs = 1_100_000L
        )
        assertNotNull(est)
        assertEquals(FlightConfidence.GREEN, est!!.confidence)
    }

    @Test
    fun `api until produces HIGH confidence exact timestamp`() {
        val est = estimator.estimate(
            place = TravelPlace.JAPAN,
            direction = TravelDirection.RETURN,
            category = PlaneCategory.UNKNOWN,
            hasBook = null,
            departObservedWindow = null,
            firstSeenAtMs = 1_000_000L,
            apiUntilSec = 5_000L,
            nowMs = 1_000_000L
        )
        assertNotNull(est)
        assertEquals(FlightConfidence.HIGH, est!!.confidence)
        assertEquals(5_000_000L, est.windowStartMs)
        assertEquals(5_000_000L, est.windowEndMs)
        assertEquals(64, est.baseMinutesMin)
        assertEquals(213, est.baseMinutesMax)
    }

    @Test
    fun `known category and departure transition populate exact base duration and poll tolerance`() {
        val est = estimator.estimate(
            place = TravelPlace.SWITZERLAND,
            direction = TravelDirection.OUT,
            category = PlaneCategory.AIRSTRIP,
            hasBook = false,
            departObservedWindow = 10_000_000L to 10_030_000L,
            firstSeenAtMs = 10_030_000L,
            apiUntilSec = null,
            nowMs = 10_100_000L
        )
        assertNotNull(est)
        assertEquals(FlightConfidence.GREEN, est!!.confidence)
        assertEquals(116, est.baseMinutesMin)
        assertEquals(116, est.baseMinutesMax)
        assertEquals(false, est.hasBook)
        assertEquals(10_000_000L, est.departWindowStartMs)
        assertEquals(10_030_000L, est.departWindowEndMs)
        assertTrue(est.windowEndMs > est.windowStartMs)
    }
}
