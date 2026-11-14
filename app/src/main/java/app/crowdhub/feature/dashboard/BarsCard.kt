package app.crowdhub.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import app.crowdhub.provider.torn.BarValue
import app.crowdhub.provider.torn.TornBars

@Composable
fun BarsCard(
    bars: TornBars,
    fetchedAtMs: Long,
    clock: Clock,
    dimmed: Boolean
) {
    CrowCard(dimmed = dimmed) {
        Text(
            text = "Bars",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = CrowColors.Text
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            bars.energy?.let { BarRow(it, CrowColors.Ok, fetchedAtMs, clock) }
            bars.nerve?.let { BarRow(it, CrowColors.Danger, fetchedAtMs, clock) }
            bars.happy?.let { BarRow(it, CrowColors.AccentD, fetchedAtMs, clock) }
            bars.life?.let { BarRow(it, CrowColors.Info, fetchedAtMs, clock) }
        }
    }
}

@Composable
private fun BarRow(
    bar: BarValue,
    accent: Color,
    fetchedAtMs: Long,
    clock: Clock
) {
    val progress = if (bar.maximum > 0) (bar.current.toFloat() / bar.maximum.toFloat()).coerceIn(0f, 1f) else 0f
    val targetFullMs = if (bar.current < bar.maximum && bar.fullTimeSec != null && bar.fullTimeSec > 0L) {
        fetchedAtMs + bar.fullTimeSec * 1_000L
    } else {
        null
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${bar.label}  ${bar.current}/${bar.maximum}",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = CrowColors.Text,
                fontWeight = FontWeight.Medium
            )
            if (bar.current >= bar.maximum) {
                Text("Full", style = MaterialTheme.typography.labelMedium, color = CrowColors.Ok)
            } else if (targetFullMs != null) {
                CountdownText(
                    targetEpochMs = targetFullMs,
                    clock = clock,
                    prefix = "Full in ",
                    readyText = "Full",
                    color = CrowColors.TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = CrowColors.SurfaceHigh
        )
    }
}
