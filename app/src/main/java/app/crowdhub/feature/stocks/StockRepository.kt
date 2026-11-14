package app.crowdhub.feature.stocks

import app.crowdhub.core.alerts.AlertCandidate
import app.crowdhub.core.alerts.AlertContext
import app.crowdhub.core.alerts.AlertSource
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.db.StockDao
import app.crowdhub.core.db.StockSnapshotEntity
import app.crowdhub.core.db.StockWatchEntity
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornStock
import app.crowdhub.provider.torn.TornUserStock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class ChartRange(val label: String, val durationMs: Long) {
    H1("1h", 3_600_000L),
    H6("6h", 6 * 3_600_000L),
    H24("24h", 24 * 3_600_000L),
    D7("7d", 7 * 86_400_000L),
    D30("30d", 30 * 86_400_000L)
}

data class StockListItem(
    val stock: TornStock,
    val changePct24h: Double?,
    val sparkline: List<StockPricePoint>,
    val hasWatch: Boolean,
    val holding: TornUserStock? = null
)

class StockRepository(
    private val stockDao: StockDao,
    private val tornApi: TornApi,
    private val settingsStore: SettingsStore,
    private val clock: Clock,
    private val evaluator: StockAlertEvaluator = StockAlertEvaluator()
) {
    fun observeWatches(accountId: String): Flow<List<StockWatchEntity>> =
        stockDao.observeWatches(accountId)

    fun observeWatchesForStock(accountId: String, stockId: Int): Flow<List<StockWatchEntity>> =
        stockDao.observeWatchesForStock(accountId, stockId)

    suspend fun fetchStocksAndRecord(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<List<TornStock>> {
        val result = tornApi.getStocks(accountId, priority, forceRefresh)
        if (result is ApiResult.Success) {
            recordSnapshots(result.data, result.fetchedAtMs)
        }
        return result
    }

    suspend fun fetchUserStocks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean
    ): ApiResult<List<TornUserStock>> = tornApi.getUserStocks(accountId, priority, forceRefresh)

    suspend fun getHistory(stockId: Int, range: ChartRange): List<StockPricePoint> {
        val since = clock.nowEpochMs() - range.durationMs
        val rows = stockDao.getSnapshotsSince(stockId, since)
            .map { StockPricePoint(it.ts, it.price) }
        return downsampleToLimit(rows, MAX_CHART_POINTS)
    }

    suspend fun getAllRecent24hSnapshots(): Map<Int, List<StockPricePoint>> {
        val since = clock.nowEpochMs() - ChartRange.H24.durationMs
        return stockDao.getAllSnapshotsSince(since)
            .groupBy { it.stockId }
            .mapValues { (_, list) ->
                downsampleToLimit(list.map { StockPricePoint(it.ts, it.price) }, 40)
            }
    }

    suspend fun saveWatch(
        id: String?,
        accountId: String,
        stockId: Int,
        aboveTarget: Double?,
        belowTarget: Double?,
        pctMove: Double?,
        pctWindowMin: Int?,
        repeating: Boolean,
        cooldownMin: Int,
        enabled: Boolean
    ) {
        stockDao.upsertWatch(
            StockWatchEntity(
                id = id ?: UUID.randomUUID().toString(),
                accountId = accountId,
                stockId = stockId,
                aboveTarget = aboveTarget,
                belowTarget = belowTarget,
                pctMove = pctMove,
                pctWindowMin = pctWindowMin,
                repeating = repeating,
                cooldownMin = cooldownMin.coerceAtLeast(1),
                armed = true,
                lastTriggeredAt = null,
                enabled = enabled
            )
        )
    }

    suspend fun toggleWatch(watch: StockWatchEntity, enabled: Boolean) {
        stockDao.upsertWatch(watch.copy(enabled = enabled, armed = if (enabled) true else watch.armed))
    }

    suspend fun deleteWatch(id: String) {
        stockDao.deleteWatch(id)
    }

    suspend fun clearAllHistory() {
        stockDao.clearAllSnapshots()
    }

    suspend fun runRetentionCleanup() {
        val now = clock.nowEpochMs()
        val thirtyDaysAgo = now - 30L * 86_400_000L
        stockDao.deleteSnapshotsBefore(thirtyDaysAgo)

        val fortyEightHoursAgo = now - 48L * 3_600_000L
        val oldRows = stockDao.getSnapshotsBetween(thirtyDaysAgo, fortyEightHoursAgo)
        if (oldRows.isEmpty()) return

        val bucketMs = 15L * 60_000L
        val toDelete = mutableListOf<Long>()
        oldRows.groupBy { it.stockId to (it.ts / bucketMs) }.forEach { (_, group) ->
            if (group.size > 1) {
                // Keep the first row in each 15-min bucket, delete extras
                group.drop(1).forEach { toDelete += it.id }
            }
        }
        if (toDelete.isNotEmpty()) {
            toDelete.chunked(500).forEach { chunk ->
                stockDao.deleteSnapshotsByIds(chunk)
            }
        }
    }

    fun alertSource(): AlertSource = object : AlertSource {
        override val id: String = "torn.stocks"

        override suspend fun collect(ctx: AlertContext): List<AlertCandidate> {
            val prefs = settingsStore.current()
            val watches = if (prefs.alertsStocksEnabled) {
                stockDao.getEnabledWatches(ctx.accountId)
            } else {
                emptyList()
            }
            val shouldSyncMarket = prefs.backgroundStockSyncEnabled || watches.isNotEmpty()
            if (!shouldSyncMarket) return emptyList()

            val priority = when (ctx.mode) {
                AlertTriggerMode.FOREGROUND -> RequestPriority.USER
                AlertTriggerMode.WATCH -> RequestPriority.WATCH
                AlertTriggerMode.BACKGROUND -> RequestPriority.BACKGROUND
            }

            val res = fetchStocksAndRecord(ctx.accountId, priority, forceRefresh = false)
            val stocks = (res as? ApiResult.Success)?.data ?: return emptyList()
            if (!prefs.alertsStocksEnabled || watches.isEmpty()) return emptyList()
            val stocksById = stocks.associateBy { it.stockId }

            val now = ctx.nowEpochMs
            val maxWindowMin = watches.mapNotNull { it.pctWindowMin }.maxOrNull() ?: 60
            val historySince = now - maxWindowMin * 60_000L
            val allHistory = stockDao.getAllSnapshotsSince(historySince)
                .groupBy { it.stockId }
                .mapValues { (_, rows) -> rows.map { StockPricePoint(it.ts, it.price) } }

            val candidates = mutableListOf<AlertCandidate>()
            val updatedEntities = mutableListOf<StockWatchEntity>()

            watches.groupBy { it.stockId }.forEach { (stockId, stockWatches) ->
                val stock = stocksById[stockId] ?: return@forEach
                val rules = stockWatches.map { it.toRule() }
                val outcome = evaluator.evaluate(
                    stockId = stockId,
                    currentPrice = stock.currentPrice,
                    history = allHistory[stockId].orEmpty(),
                    rules = rules,
                    nowMs = now
                )
                outcome.updatedRules.forEach { updatedRule ->
                    val orig = stockWatches.first { it.id == updatedRule.id }
                    updatedEntities += orig.copy(
                        armed = updatedRule.armed,
                        lastTriggeredAt = updatedRule.lastTriggeredAt,
                        enabled = updatedRule.enabled
                    )
                }
                outcome.triggers.forEach { trig ->
                    candidates += AlertCandidate(
                        dedupeKey = "torn:stock:${trig.watchId}:${trig.triggeredAtMs / 60_000L}",
                        accountId = ctx.accountId,
                        channelId = NotificationChannels.STOCKS,
                        title = "${stock.acronym} (${stock.name})",
                        body = "Price $${"%.2f".format(trig.price)} (${trig.reason})",
                        fireAtEpochMs = null,
                        deepLink = "stock/$stockId"
                    )
                }
            }

            if (updatedEntities.isNotEmpty()) {
                stockDao.updateWatches(updatedEntities)
            }
            return candidates
        }
    }

    private suspend fun recordSnapshots(stocks: List<TornStock>, nowMs: Long) {
        val toInsert = mutableListOf<StockSnapshotEntity>()
        for (stock in stocks) {
            if (stock.currentPrice <= 0.0) continue
            val latestTs = stockDao.getLatestTs(stock.stockId)
            if (latestTs == null || (nowMs - latestTs) >= MIN_SNAPSHOT_INTERVAL_MS) {
                toInsert += StockSnapshotEntity(
                    stockId = stock.stockId,
                    ts = nowMs,
                    price = stock.currentPrice
                )
            }
        }
        if (toInsert.isNotEmpty()) {
            stockDao.insertSnapshots(toInsert)
        }
    }

    private fun downsampleToLimit(points: List<StockPricePoint>, limit: Int): List<StockPricePoint> {
        if (points.size <= limit) return points
        val step = points.size.toDouble() / limit.toDouble()
        val out = ArrayList<StockPricePoint>(limit)
        var i = 0.0
        while (i < points.size && out.size < limit) {
            out += points[i.toInt()]
            i += step
        }
        if (out.lastOrNull() != points.last()) {
            out[out.lastIndex] = points.last()
        }
        return out
    }

    private fun StockWatchEntity.toRule() = StockWatchRule(
        id = id,
        accountId = accountId,
        stockId = stockId,
        aboveTarget = aboveTarget,
        belowTarget = belowTarget,
        pctMove = pctMove,
        pctWindowMin = pctWindowMin,
        repeating = repeating,
        cooldownMin = cooldownMin,
        armed = armed,
        lastTriggeredAt = lastTriggeredAt,
        enabled = enabled
    )

    companion object {
        private const val MIN_SNAPSHOT_INTERVAL_MS = 60_000L
        private const val MAX_CHART_POINTS = 300
    }
}
