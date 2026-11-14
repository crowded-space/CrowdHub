package app.crowdhub.feature.stocks

import app.crowdhub.provider.torn.TornStock
import app.crowdhub.provider.torn.TornUserStock
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class OwnedStockPosition(
    val stock: TornStock,
    val holding: TornUserStock,
    val currentValue: Double,
    val avgBuyPrice: Double?,
    val costBasis: Double?,
    val unrealizedPnl: Double?,
    val unrealizedPnlPct: Double?,
    val portfolioWeightPct: Double,
    val changePct24h: Double?,
    val sparkline: List<StockPricePoint>,
    val hasWatch: Boolean,
    val currentTier: Int,
    val nextTierSharesRemaining: Long?,
    val nextTierCost: Double?,
    val isMaxPassiveTier: Boolean,
    val isDividendReady: Boolean,
    val dividendProgress: Int,
    val dividendFrequency: Int
)

data class PortfolioSummary(
    val positions: List<OwnedStockPosition>,
    val totalValue: Double,
    val totalCostBasis: Double?,
    val hasPartialCostBasis: Boolean,
    val totalUnrealizedPnl: Double?,
    val totalUnrealizedPnlPct: Double?,
    val ownedCount: Int,
    val readyDividendCount: Int,
    val activeBenefitCount: Int
)

object StockPortfolioCalculator {

    fun buildSummary(
        stocks: List<TornStock>,
        holdings: List<TornUserStock>,
        sparklines: Map<Int, List<StockPricePoint>>,
        watchStockIds: Set<Int>
    ): PortfolioSummary {
        val stocksById = stocks.associateBy { it.stockId }
        val validHoldings = holdings.filter { it.totalShares > 0L && stocksById.containsKey(it.stockId) }
        if (validHoldings.isEmpty()) {
            return PortfolioSummary(
                positions = emptyList(),
                totalValue = 0.0,
                totalCostBasis = null,
                hasPartialCostBasis = false,
                totalUnrealizedPnl = null,
                totalUnrealizedPnlPct = null,
                ownedCount = 0,
                readyDividendCount = 0,
                activeBenefitCount = 0
            )
        }

        val totalValue = validHoldings.sumOf { h ->
            val stock = stocksById.getValue(h.stockId)
            h.totalShares.toDouble() * stock.currentPrice
        }

        val positions = validHoldings.map { holding ->
            val stock = stocksById.getValue(holding.stockId)
            buildPosition(
                stock = stock,
                holding = holding,
                totalPortfolioValue = totalValue,
                series = sparklines[stock.stockId].orEmpty(),
                hasWatch = stock.stockId in watchStockIds
            )
        }

        val positionsWithCost = positions.filter { it.costBasis != null }
        val totalCostBasis = if (positionsWithCost.isNotEmpty()) {
            positionsWithCost.sumOf { it.costBasis!! }
        } else {
            null
        }
        val hasPartialCostBasis = positionsWithCost.isNotEmpty() && positionsWithCost.size < positions.size

        val comparableValue = positionsWithCost.sumOf { it.currentValue }
        val totalUnrealizedPnl = totalCostBasis?.let { comparableValue - it }
        val totalUnrealizedPnlPct = if (totalCostBasis != null && totalCostBasis > 0.0 && totalUnrealizedPnl != null) {
            (totalUnrealizedPnl / totalCostBasis) * 100.0
        } else {
            null
        }

        return PortfolioSummary(
            positions = positions,
            totalValue = totalValue,
            totalCostBasis = totalCostBasis,
            hasPartialCostBasis = hasPartialCostBasis,
            totalUnrealizedPnl = totalUnrealizedPnl,
            totalUnrealizedPnlPct = totalUnrealizedPnlPct,
            ownedCount = positions.size,
            readyDividendCount = positions.count { it.isDividendReady },
            activeBenefitCount = positions.count { it.currentTier > 0 }
        )
    }

    fun buildPosition(
        stock: TornStock,
        holding: TornUserStock,
        totalPortfolioValue: Double,
        series: List<StockPricePoint>,
        hasWatch: Boolean
    ): OwnedStockPosition {
        val currentValue = holding.totalShares.toDouble() * stock.currentPrice
        val txShares = holding.transactions.sumOf { it.shares }
        val txCost = holding.transactions.sumOf { it.shares.toDouble() * it.boughtPrice }
        val avgBuyPrice = if (txShares > 0L) txCost / txShares.toDouble() else null
        val costBasis = avgBuyPrice?.let { avg ->
            if (txShares == holding.totalShares) txCost else avg * holding.totalShares.toDouble()
        }
        val unrealizedPnl = costBasis?.let { currentValue - it }
        val unrealizedPnlPct = if (costBasis != null && costBasis > 0.0 && unrealizedPnl != null) {
            (unrealizedPnl / costBasis) * 100.0
        } else {
            null
        }
        val weightPct = if (totalPortfolioValue > 0.0) {
            (currentValue / totalPortfolioValue) * 100.0
        } else {
            0.0
        }
        val pct24h = if (series.size >= 2 && series.first().price > 0.0) {
            ((series.last().price - series.first().price) / series.first().price) * 100.0
        } else {
            null
        }

        val req = stock.benefitRequirement?.takeIf { it > 0L }
        val isPassive = stock.bonusPassive == true
        val inferredTier = req?.let { computeTierFromShares(holding.totalShares, it, isPassive) } ?: 0
        val currentTier = (holding.dividend?.increment ?: inferredTier).let { tier ->
            if (isPassive) tier.coerceAtMost(1) else tier.coerceAtLeast(0)
        }
        val isMaxPassive = isPassive && currentTier >= 1

        val nextTierSharesRemaining = if (req != null && !isMaxPassive) {
            val neededCumulative = cumulativeSharesForTier(req, currentTier + 1)
            neededCumulative?.let { (it - holding.totalShares).coerceAtLeast(0L) }
        } else {
            null
        }
        val nextTierCost = nextTierSharesRemaining?.let { it.toDouble() * stock.currentPrice }

        val freq = holding.dividend?.frequency?.takeIf { it > 0 } ?: stock.bonusFrequencyDays ?: 0
        val progress = (holding.dividend?.progress ?: 0).coerceAtLeast(0)

        return OwnedStockPosition(
            stock = stock,
            holding = holding,
            currentValue = currentValue,
            avgBuyPrice = avgBuyPrice,
            costBasis = costBasis,
            unrealizedPnl = unrealizedPnl,
            unrealizedPnlPct = unrealizedPnlPct,
            portfolioWeightPct = weightPct,
            changePct24h = pct24h,
            sparkline = series,
            hasWatch = hasWatch,
            currentTier = currentTier,
            nextTierSharesRemaining = nextTierSharesRemaining,
            nextTierCost = nextTierCost,
            isMaxPassiveTier = isMaxPassive,
            isDividendReady = holding.dividend?.ready == true,
            dividendProgress = progress,
            dividendFrequency = freq
        )
    }

    fun computeTierFromShares(shares: Long, baseRequirement: Long, isPassive: Boolean): Int {
        if (baseRequirement <= 0L || shares < baseRequirement) return 0
        if (isPassive) return 1
        var tier = 0
        while (tier < 30) {
            val nextCumulative = cumulativeSharesForTier(baseRequirement, tier + 1) ?: break
            if (shares >= nextCumulative) {
                tier++
            } else {
                break
            }
        }
        return tier
    }

    fun cumulativeSharesForTier(baseRequirement: Long, tier: Int): Long? {
        if (baseRequirement <= 0L || tier <= 0) return 0L
        if (tier >= 31) return null
        val multiplier = (1L shl tier) - 1L
        if (baseRequirement > Long.MAX_VALUE / multiplier) return null
        return baseRequirement * multiplier
    }
}

object StockFormatters {
    private val wholeFormatter: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale.US)

    fun formatSharesFull(shares: Long): String = wholeFormatter.format(shares)

    fun formatSharesCompact(shares: Long): String {
        val absVal = abs(shares)
        return when {
            absVal >= 1_000_000_000L -> "${"%.2f".format(Locale.US, shares / 1_000_000_000.0)}B"
            absVal >= 1_000_000L -> "${"%.2f".format(Locale.US, shares / 1_000_000.0)}M"
            absVal >= 10_000L -> "${"%.1f".format(Locale.US, shares / 1_000.0)}K"
            else -> wholeFormatter.format(shares)
        }
    }

    fun formatCurrencyWhole(amount: Double): String {
        val rounded = amount.roundToLong()
        val prefix = if (rounded < 0L) "-$" else "$"
        return "$prefix${wholeFormatter.format(abs(rounded))}"
    }

    fun formatCurrencyCompact(amount: Double): String {
        val sign = if (amount < 0.0) "-" else ""
        val absVal = abs(amount)
        return when {
            absVal >= 1_000_000_000_000.0 -> "${sign}$${"%.2f".format(Locale.US, absVal / 1_000_000_000_000.0)}T"
            absVal >= 1_000_000_000.0 -> "${sign}$${"%.2f".format(Locale.US, absVal / 1_000_000_000.0)}B"
            absVal >= 1_000_000.0 -> "${sign}$${"%.2f".format(Locale.US, absVal / 1_000_000.0)}M"
            absVal >= 10_000.0 -> "${sign}$${"%.1f".format(Locale.US, absVal / 1_000.0)}K"
            else -> "${sign}$${wholeFormatter.format(absVal.roundToLong())}"
        }
    }

    fun formatSignedCurrency(amount: Double, compact: Boolean = false): String {
        val sign = if (amount >= 0.0) "+" else "-"
        val body = if (compact) {
            formatCurrencyCompact(abs(amount))
        } else {
            formatCurrencyWhole(abs(amount))
        }
        return "$sign$body"
    }

    fun formatSignedPct(pct: Double): String {
        val sign = if (pct >= 0.0) "+" else ""
        return "$sign${"%.2f".format(Locale.US, pct)}%"
    }
}
