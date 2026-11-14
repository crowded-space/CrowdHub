package app.crowdhub.feature.stocks

import kotlin.math.abs

data class StockPricePoint(
    val tsMs: Long,
    val price: Double
)

data class StockWatchRule(
    val id: String,
    val accountId: String,
    val stockId: Int,
    val aboveTarget: Double?,
    val belowTarget: Double?,
    val pctMove: Double?,
    val pctWindowMin: Int?,
    val repeating: Boolean,
    val cooldownMin: Int,
    val armed: Boolean,
    val lastTriggeredAt: Long?,
    val enabled: Boolean
)

data class StockAlertTrigger(
    val watchId: String,
    val stockId: Int,
    val reason: String,
    val price: Double,
    val triggeredAtMs: Long
)

data class StockEvalOutcome(
    val updatedRules: List<StockWatchRule>,
    val triggers: List<StockAlertTrigger>
)

class StockAlertEvaluator {
    fun evaluate(
        stockId: Int,
        currentPrice: Double,
        history: List<StockPricePoint>,
        rules: List<StockWatchRule>,
        nowMs: Long
    ): StockEvalOutcome {
        if (currentPrice <= 0.0) {
            return StockEvalOutcome(rules, emptyList())
        }

        val updated = mutableListOf<StockWatchRule>()
        val triggers = mutableListOf<StockAlertTrigger>()

        for (rule in rules) {
            if (!rule.enabled || rule.stockId != stockId) {
                updated += rule
                continue
            }

            val aboveHit = rule.aboveTarget != null && currentPrice >= rule.aboveTarget
            val belowHit = rule.belowTarget != null && currentPrice <= rule.belowTarget

            var pctHit = false
            var pctDesc = ""
            if (rule.pctMove != null && rule.pctMove > 0.0 && rule.pctWindowMin != null && rule.pctWindowMin > 0) {
                val windowStartMs = nowMs - rule.pctWindowMin * 60_000L
                val baseline = history
                    .filter { it.tsMs >= windowStartMs && it.price > 0.0 }
                    .minByOrNull { it.tsMs }
                if (baseline != null) {
                    val changePct = ((currentPrice - baseline.price) / baseline.price) * 100.0
                    if (abs(changePct) >= rule.pctMove) {
                        pctHit = true
                        val sign = if (changePct >= 0) "+" else ""
                        pctDesc = "$sign${"%.2f".format(changePct)}% in ${rule.pctWindowMin}m"
                    }
                }
            }

            val hasThresholdRule = rule.aboveTarget != null || rule.belowTarget != null
            val thresholdHit = aboveHit || belowHit

            // Hysteresis: re-arm when price crosses back past thresholds
            val rearmed = if (!rule.armed && hasThresholdRule) {
                val aboveCleared = rule.aboveTarget == null || currentPrice < rule.aboveTarget
                val belowCleared = rule.belowTarget == null || currentPrice > rule.belowTarget
                aboveCleared && belowCleared
            } else if (!rule.armed && !hasThresholdRule && rule.pctMove != null) {
                val cooldownElapsed = rule.lastTriggeredAt == null ||
                    (nowMs - rule.lastTriggeredAt) >= rule.cooldownMin * 60_000L
                cooldownElapsed
            } else {
                rule.armed
            }

            val cooldownElapsed = rule.lastTriggeredAt == null ||
                (nowMs - rule.lastTriggeredAt) >= rule.cooldownMin * 60_000L

            val shouldFire = rearmed && cooldownElapsed && (thresholdHit || pctHit)

            if (shouldFire) {
                val reasons = buildList {
                    if (aboveHit) add("above ${"%.2f".format(rule.aboveTarget)}")
                    if (belowHit) add("below ${"%.2f".format(rule.belowTarget)}")
                    if (pctHit) add(pctDesc)
                }.joinToString(", ")

                triggers += StockAlertTrigger(
                    watchId = rule.id,
                    stockId = stockId,
                    reason = reasons,
                    price = currentPrice,
                    triggeredAtMs = nowMs
                )

                updated += rule.copy(
                    armed = false,
                    lastTriggeredAt = nowMs,
                    enabled = rule.repeating
                )
            } else {
                updated += rule.copy(armed = rearmed)
            }
        }

        return StockEvalOutcome(updated, triggers)
    }
}
