package app.crowdhub.feature.stocks

import app.crowdhub.provider.torn.TornStock
import app.crowdhub.provider.torn.TornStockDividend
import app.crowdhub.provider.torn.TornStockTransaction
import app.crowdhub.provider.torn.TornUserStock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockPortfolioCalculatorTest {

    private fun stock(
        id: Int,
        acronym: String,
        price: Double,
        req: Long?,
        passive: Boolean = false,
        freq: Int = 7
    ) = TornStock(
        stockId = id,
        name = "Company $acronym",
        acronym = acronym,
        currentPrice = price,
        marketCap = null,
        totalShares = null,
        availableShares = null,
        forecast = null,
        demand = null,
        benefitDescription = "Reward $acronym",
        benefitRequirement = req,
        investors = null,
        bonusPassive = passive,
        bonusFrequencyDays = freq
    )

    @Test
    fun `calculates total value, cost basis, unrealized PnL, weights, and next tier math`() {
        val tsb = stock(id = 1, acronym = "TSB", price = 1200.0, req = 3_000_000L, passive = false, freq = 31)
        val tci = stock(id = 2, acronym = "TCI", price = 1000.0, req = 1_500_000L, passive = true, freq = 7)

        val h1 = TornUserStock(
            stockId = 1,
            totalShares = 3_000_000L,
            dividend = TornStockDividend(ready = true, increment = 1, progress = 31, frequency = 31),
            transactions = listOf(
                TornStockTransaction(1L, 1_000_000L, 1000.0, 100L),
                TornStockTransaction(2L, 2_000_000L, 1100.0, 200L)
            )
        )
        val h2 = TornUserStock(
            stockId = 2,
            totalShares = 1_500_000L,
            dividend = TornStockDividend(ready = false, increment = 1, progress = 7, frequency = 7),
            transactions = listOf(
                TornStockTransaction(3L, 1_500_000L, 1100.0, 300L)
            )
        )

        val summary = StockPortfolioCalculator.buildSummary(
            stocks = listOf(tsb, tci),
            holdings = listOf(h1, h2),
            sparklines = emptyMap(),
            watchStockIds = setOf(1)
        )

        // TSB value: 3M * 1200 = 3.6B; cost: 1M*1000 + 2M*1100 = 3.2B; PnL: +400M
        // TCI value: 1.5M * 1000 = 1.5B; cost: 1.5M*1100 = 1.65B; PnL: -150M
        // Total value = 5.1B; Total cost = 4.85B; Total PnL = +250M
        assertEquals(2, summary.ownedCount)
        assertEquals(1, summary.readyDividendCount)
        assertEquals(2, summary.activeBenefitCount)
        assertEquals(5_100_000_000.0, summary.totalValue, 1.0)
        assertEquals(4_850_000_000.0, summary.totalCostBasis!!, 1.0)
        assertEquals(250_000_000.0, summary.totalUnrealizedPnl!!, 1.0)
        assertEquals((250_000_000.0 / 4_850_000_000.0) * 100.0, summary.totalUnrealizedPnlPct!!, 0.001)
        assertFalse(summary.hasPartialCostBasis)

        val posTsb = summary.positions.first { it.stock.stockId == 1 }
        assertTrue(posTsb.hasWatch)
        assertTrue(posTsb.isDividendReady)
        assertEquals(1, posTsb.currentTier)
        // Tier 2 cumulative for req 3M = 3M * (2^2 - 1) = 9M -> 6M remaining
        assertEquals(6_000_000L, posTsb.nextTierSharesRemaining)
        assertEquals(7_200_000_000.0, posTsb.nextTierCost!!, 1.0)
        assertEquals((3_600_000_000.0 / 5_100_000_000.0) * 100.0, posTsb.portfolioWeightPct, 0.001)

        val posTci = summary.positions.first { it.stock.stockId == 2 }
        assertTrue(posTci.isMaxPassiveTier)
        assertNull(posTci.nextTierSharesRemaining)
    }

    @Test
    fun `handles cumulative tier doubling thresholds accurately`() {
        assertEquals(0, StockPortfolioCalculator.computeTierFromShares(999_999L, 1_000_000L, isPassive = false))
        assertEquals(1, StockPortfolioCalculator.computeTierFromShares(1_000_000L, 1_000_000L, isPassive = false))
        assertEquals(1, StockPortfolioCalculator.computeTierFromShares(2_999_999L, 1_000_000L, isPassive = false))
        assertEquals(2, StockPortfolioCalculator.computeTierFromShares(3_000_000L, 1_000_000L, isPassive = false))
        assertEquals(3, StockPortfolioCalculator.computeTierFromShares(7_000_000L, 1_000_000L, isPassive = false))
        assertEquals(1, StockPortfolioCalculator.computeTierFromShares(7_000_000L, 1_000_000L, isPassive = true))
    }
}
