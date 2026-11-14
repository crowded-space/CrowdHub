package app.crowdhub.feature.stocks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockAlertEvaluatorTest {
    private val evaluator = StockAlertEvaluator()

    @Test
    fun `triggers above and below targets and respects one-shot vs hysteresis`() {
        val oneShot = StockWatchRule(
            id = "w1",
            accountId = "a1",
            stockId = 1,
            aboveTarget = 100.0,
            belowTarget = null,
            pctMove = null,
            pctWindowMin = null,
            repeating = false,
            cooldownMin = 60,
            armed = true,
            lastTriggeredAt = null,
            enabled = true
        )
        val repeating = StockWatchRule(
            id = "w2",
            accountId = "a1",
            stockId = 1,
            aboveTarget = null,
            belowTarget = 90.0,
            pctMove = null,
            pctWindowMin = null,
            repeating = true,
            cooldownMin = 10,
            armed = true,
            lastTriggeredAt = null,
            enabled = true
        )

        val out1 = evaluator.evaluate(1, 105.0, emptyList(), listOf(oneShot, repeating), nowMs = 1_000_000L)
        assertEquals(1, out1.triggers.size)
        assertEquals("w1", out1.triggers[0].watchId)
        assertFalse(out1.updatedRules[0].enabled)

        // Price drops to 88 -> w2 fires and disarms
        val out2 = evaluator.evaluate(1, 88.0, emptyList(), out1.updatedRules, nowMs = 1_100_000L)
        assertEquals(1, out2.triggers.size)
        assertEquals("w2", out2.triggers[0].watchId)
        assertFalse(out2.updatedRules[1].armed)
        assertTrue(out2.updatedRules[1].enabled)

        // Stays below 90 after cooldown -> stays disarmed (no spam)
        val out3 = evaluator.evaluate(1, 87.0, emptyList(), out2.updatedRules, nowMs = 2_000_000L)
        assertTrue(out3.triggers.isEmpty())

        // Crosses back above 90 -> re-arms, then drops again -> triggers
        val out4 = evaluator.evaluate(1, 95.0, emptyList(), out3.updatedRules, nowMs = 2_100_000L)
        assertTrue(out4.updatedRules[1].armed)
        val out5 = evaluator.evaluate(1, 89.0, emptyList(), out4.updatedRules, nowMs = 2_200_000L)
        assertEquals(1, out5.triggers.size)
    }

    @Test
    fun `detects percent move within lookback window`() {
        val rule = StockWatchRule(
            id = "wpct",
            accountId = "a1",
            stockId = 2,
            aboveTarget = null,
            belowTarget = null,
            pctMove = 5.0,
            pctWindowMin = 30,
            repeating = true,
            cooldownMin = 15,
            armed = true,
            lastTriggeredAt = null,
            enabled = true
        )
        val now = 2_000_000L
        val history = listOf(
            StockPricePoint(now - 20 * 60_000L, 100.0),
            StockPricePoint(now - 10 * 60_000L, 102.0)
        )
        val out = evaluator.evaluate(2, 106.0, history, listOf(rule), nowMs = now)
        assertEquals(1, out.triggers.size)
    }
}
