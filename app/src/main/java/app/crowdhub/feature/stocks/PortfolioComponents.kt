package app.crowdhub.feature.stocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import app.crowdhub.provider.torn.TornStock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALLOCATION_PALETTE = listOf(
    CrowColors.Sheen,
    CrowColors.Ok,
    CrowColors.Info,
    CrowColors.AccentD,
    CrowColors.Abroad
)

@Composable
fun PortfolioCompactStrip(
    summary: PortfolioSummary,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary.ownedCount <= 0) return
    val pnl = summary.totalUnrealizedPnl
    val pnlPct = summary.totalUnrealizedPnlPct
    val pnlColor = when {
        pnl == null -> CrowColors.TextMuted
        pnl >= 0.0 -> CrowColors.Ok
        else -> CrowColors.Danger
    }

    CrowCard(
        dimmed = dimmed,
        borderColor = CrowColors.Sheen.copy(alpha = 0.45f),
        containerColor = CrowColors.SurfaceHigh,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("portfolio_compact_strip")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CrowColors.Sheen.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = CrowColors.SheenSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Portfolio",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrowColors.TextMuted
                        )
                        Text(
                            text = "${summary.ownedCount} owned",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.SheenSoft
                        )
                        if (summary.readyDividendCount > 0) {
                            MiniBadge(
                                text = "${summary.readyDividendCount} ready",
                                color = CrowColors.AccentD
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = StockFormatters.formatCurrencyWhole(summary.totalValue),
                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.Text
                        )
                        if (pnl != null) {
                            val pctPart = pnlPct?.let { " (${StockFormatters.formatSignedPct(it)})" }.orEmpty()
                            Text(
                                text = "${StockFormatters.formatSignedCurrency(pnl, compact = true)}$pctPart",
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                                fontWeight = FontWeight.SemiBold,
                                color = pnlColor
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Open portfolio",
                tint = CrowColors.TextMuted
            )
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    dimmed: Boolean,
    modifier: Modifier = Modifier
) {
    val pnl = summary.totalUnrealizedPnl
    val pnlPct = summary.totalUnrealizedPnlPct
    val pnlColor = when {
        pnl == null -> CrowColors.TextMuted
        pnl >= 0.0 -> CrowColors.Ok
        else -> CrowColors.Danger
    }

    CrowCard(
        dimmed = dimmed,
        borderColor = CrowColors.Sheen.copy(alpha = 0.45f),
        containerColor = CrowColors.SurfaceHigh,
        modifier = modifier.testTag("portfolio_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Portfolio Value",
                    style = MaterialTheme.typography.labelMedium,
                    color = CrowColors.TextMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = StockFormatters.formatCurrencyWhole(summary.totalValue),
                    style = MaterialTheme.typography.headlineSmall.merge(TabularNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
            }
            if (summary.readyDividendCount > 0) {
                MiniBadge(
                    text = "${summary.readyDividendCount} Dividend Ready",
                    color = CrowColors.AccentD
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Unrealized P&L",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted
                )
                if (pnl != null) {
                    val pctText = pnlPct?.let { " (${StockFormatters.formatSignedPct(it)})" }.orEmpty()
                    Text(
                        text = "${StockFormatters.formatSignedCurrency(pnl, compact = false)}$pctText",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.SemiBold,
                        color = pnlColor
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrowColors.TextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (summary.hasPartialCostBasis) "Cost Basis (partial)" else "Cost Basis",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted
                )
                Text(
                    text = summary.totalCostBasis?.let { StockFormatters.formatCurrencyWhole(it) } ?: "—",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = CrowColors.Text
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBadge(
                text = "${summary.ownedCount} Stocks",
                color = CrowColors.SheenSoft
            )
            MiniBadge(
                text = "${summary.activeBenefitCount} Active Benefits",
                color = CrowColors.Info
            )
        }

        val sortedByWeight = remember(summary.positions) {
            summary.positions.sortedByDescending { it.currentValue }
        }
        if (sortedByWeight.isNotEmpty() && summary.totalValue > 0.0) {
            Spacer(Modifier.height(12.dp))
            val topFour = sortedByWeight.take(4)
            val otherWeight = sortedByWeight.drop(4).sumOf { it.portfolioWeightPct }
            val segments = buildList {
                topFour.forEachIndexed { idx, pos ->
                    add(Triple(pos.stock.acronym, pos.portfolioWeightPct, ALLOCATION_PALETTE[idx]))
                }
                if (otherWeight > 0.1) {
                    add(Triple("Other", otherWeight, CrowColors.TextMuted))
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                var x = 0f
                val gap = if (segments.size > 1) 2.dp.toPx() else 0f
                val totalGap = gap * (segments.size - 1)
                val usableWidth = (size.width - totalGap).coerceAtLeast(1f)
                segments.forEachIndexed { idx, (_, pct, color) ->
                    val frac = (pct / 100.0).toFloat().coerceIn(0f, 1f)
                    val segWidth = if (idx == segments.lastIndex) {
                        (size.width - x).coerceAtLeast(2f)
                    } else {
                        (usableWidth * frac).coerceAtLeast(2f)
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, 0f),
                        size = Size(segWidth, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    x += segWidth + gap
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                segments.forEach { (label, pct, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = "$label ${"%.0f".format(Locale.US, pct)}%",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            color = CrowColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OwnedStockCard(
    position: OwnedStockPosition,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stock = position.stock
    val pnl = position.unrealizedPnl
    val pnlPct = position.unrealizedPnlPct
    val pnlColor = when {
        pnl == null -> CrowColors.TextMuted
        pnl >= 0.0 -> CrowColors.Ok
        else -> CrowColors.Danger
    }

    CrowCard(
        dimmed = dimmed,
        borderColor = if (position.isDividendReady) {
            CrowColors.AccentD.copy(alpha = 0.55f)
        } else {
            CrowColors.Outline
        },
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("portfolio_row_${stock.stockId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stock.acronym,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrowColors.SheenSoft
                    )
                    if (position.hasWatch) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Alert active",
                            tint = CrowColors.AccentD,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (position.isDividendReady) {
                        MiniBadge(text = "READY", color = CrowColors.AccentD)
                    } else if (position.currentTier > 0) {
                        MiniBadge(text = "T${position.currentTier}", color = CrowColors.SheenSoft)
                    }
                }
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = StockFormatters.formatCurrencyWhole(position.currentValue),
                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                    fontWeight = FontWeight.SemiBold,
                    color = CrowColors.Text
                )
                val changeText = position.changePct24h?.let {
                    " · ${StockFormatters.formatSignedPct(it)}"
                }.orEmpty()
                Text(
                    text = "$${"%.2f".format(Locale.US, stock.currentPrice)}$changeText",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${StockFormatters.formatSharesFull(position.holding.totalShares)} shares · ${"%.1f".format(Locale.US, position.portfolioWeightPct)}%",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.Text
                )
                position.avgBuyPrice?.let { avg ->
                    Text(
                        text = "Avg buy $${"%.2f".format(Locale.US, avg)}",
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                        color = CrowColors.TextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (pnl != null) {
                    val pctText = pnlPct?.let { " (${StockFormatters.formatSignedPct(it)})" }.orEmpty()
                    Text(
                        text = "${StockFormatters.formatSignedCurrency(pnl, compact = true)}$pctText",
                        style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.SemiBold,
                        color = pnlColor
                    )
                } else {
                    Text(
                        text = "No cost basis",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted
                    )
                }
            }
        }

        BenefitProgressFooter(position = position)
    }
}

@Composable
private fun BenefitProgressFooter(position: OwnedStockPosition) {
    val stock = position.stock
    val benefitDesc = stock.benefitDescription
    val req = stock.benefitRequirement

    val hasAnyBenefitInfo = position.isDividendReady ||
        (position.currentTier > 0 && position.dividendFrequency > 0) ||
        position.isMaxPassiveTier ||
        (req != null && req > 0L)

    if (!hasAnyBenefitInfo) return

    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))

    when {
        position.isDividendReady -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = CrowColors.AccentD,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Dividend ready to claim${benefitDesc?.let { " · $it" }.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CrowColors.AccentD,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CrowColors.AccentD,
                trackColor = CrowColors.Outline
            )
        }
        position.isMaxPassiveTier -> {
            Text(
                text = "Passive benefit active${benefitDesc?.let { " · $it" }.orEmpty()}",
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.Ok,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        position.currentTier > 0 && position.dividendFrequency > 0 -> {
            val frac = (position.dividendProgress.toFloat() / position.dividendFrequency.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tier ${position.currentTier}${benefitDesc?.let { " · $it" }.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.SheenSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${position.dividendProgress}/${position.dividendFrequency}d",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CrowColors.Sheen,
                trackColor = CrowColors.Outline
            )
        }
        req != null && req > 0L -> {
            val remaining = position.nextTierSharesRemaining ?: 0L
            val nextTier = position.currentTier + 1
            val prevCum = StockPortfolioCalculator.cumulativeSharesForTier(req, position.currentTier) ?: 0L
            val nextCum = StockPortfolioCalculator.cumulativeSharesForTier(req, nextTier) ?: req
            val span = (nextCum - prevCum).coerceAtLeast(1L)
            val frac = ((position.holding.totalShares - prevCum).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            val costText = position.nextTierCost?.let { " (${StockFormatters.formatCurrencyCompact(it)})" }.orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${StockFormatters.formatSharesCompact(remaining)} shares$costText to T$nextTier",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(frac * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CrowColors.Info,
                trackColor = CrowColors.Outline
            )
        }
    }
}

@Composable
fun StockPositionDetailCard(
    stock: TornStock,
    position: OwnedStockPosition?,
    modifier: Modifier = Modifier
) {
    val timeFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    CrowCard(
        borderColor = if (position != null) CrowColors.Sheen.copy(alpha = 0.45f) else CrowColors.Outline,
        modifier = modifier.testTag("stock_detail_position_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (position != null) "Your Position" else "Portfolio & Benefit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CrowColors.Text
            )
            if (position?.isDividendReady == true) {
                MiniBadge(text = "Dividend Ready", color = CrowColors.AccentD)
            } else if (position != null && position.currentTier > 0) {
                MiniBadge(text = "Tier ${position.currentTier}", color = CrowColors.SheenSoft)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (position == null) {
            Text(
                text = "You do not currently hold shares in ${stock.acronym}.",
                style = MaterialTheme.typography.bodySmall,
                color = CrowColors.TextMuted
            )
            stock.benefitRequirement?.takeIf { it > 0L }?.let { req ->
                val tier1Cost = req.toDouble() * stock.currentPrice
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tier 1 requires ${StockFormatters.formatSharesFull(req)} shares (${StockFormatters.formatCurrencyWhole(tier1Cost)})",
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                    color = CrowColors.SheenSoft
                )
            }
            return@CrowCard
        }

        DetailRow("Shares Owned", StockFormatters.formatSharesFull(position.holding.totalShares))
        DetailRow("Market Value", StockFormatters.formatCurrencyWhole(position.currentValue))
        DetailRow("Portfolio Weight", "${"%.2f".format(Locale.US, position.portfolioWeightPct)}%")
        position.avgBuyPrice?.let { avg ->
            DetailRow("Avg Buy Price", "$${"%.2f".format(Locale.US, avg)}")
        }
        position.costBasis?.let { cost ->
            DetailRow("Cost Basis", StockFormatters.formatCurrencyWhole(cost))
        }
        position.unrealizedPnl?.let { pnl ->
            val pctPart = position.unrealizedPnlPct?.let { " (${StockFormatters.formatSignedPct(it)})" }.orEmpty()
            val pnlColor = if (pnl >= 0.0) CrowColors.Ok else CrowColors.Danger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Unrealized P&L", style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                Text(
                    text = "${StockFormatters.formatSignedCurrency(pnl)}$pctPart",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    fontWeight = FontWeight.SemiBold,
                    color = pnlColor
                )
            }
        }

        BenefitProgressFooter(position = position)

        if (!position.isMaxPassiveTier && position.nextTierSharesRemaining != null && position.nextTierSharesRemaining > 0L) {
            Spacer(Modifier.height(6.dp))
            val nextTier = position.currentTier + 1
            val nextCost = position.nextTierCost?.let { StockFormatters.formatCurrencyWhole(it) } ?: "—"
            DetailRow(
                "Needed for Tier $nextTier",
                "${StockFormatters.formatSharesFull(position.nextTierSharesRemaining)} ($nextCost)"
            )
        }

        if (position.holding.transactions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Purchase Lots (${position.holding.transactions.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CrowColors.Text
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                position.holding.transactions.take(10).forEach { tx ->
                    val lotPnl = (stock.currentPrice - tx.boughtPrice) * tx.shares.toDouble()
                    val lotPct = if (tx.boughtPrice > 0.0) ((stock.currentPrice - tx.boughtPrice) / tx.boughtPrice) * 100.0 else 0.0
                    val lotColor = if (lotPnl >= 0.0) CrowColors.Ok else CrowColors.Danger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${StockFormatters.formatSharesFull(tx.shares)} @ $${"%.2f".format(Locale.US, tx.boughtPrice)}",
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                color = CrowColors.Text
                            )
                            tx.timeBoughtSec?.let { sec ->
                                Text(
                                    text = timeFmt.format(Date(sec * 1_000L)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrowColors.TextMuted
                                )
                            }
                        }
                        Text(
                            text = "${StockFormatters.formatSignedCurrency(lotPnl, compact = true)} (${StockFormatters.formatSignedPct(lotPct)})",
                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                            fontWeight = FontWeight.SemiBold,
                            color = lotColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle), color = CrowColors.Text)
    }
}
