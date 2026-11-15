package app.crowdhub.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
    dimmed: Boolean,
    onEnergyClick: (() -> Unit)? = null,
    onNerveClick: (() -> Unit)? = null
) {
    CrowCard(dimmed = dimmed) {
        Text(
            text = "Bars",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = CrowColors.Text
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.energy?.let {
                BarRow(
                    bar = it,
                    accent = CrowColors.Ok,
                    fetchedAtMs = fetchedAtMs,
                    clock = clock,
                    shortcutHint = if (onEnergyClick != null) "Gym" else null,
                    testTag = "bar_energy_shortcut",
                    onClick = onEnergyClick
                )
            }
            bars.nerve?.let {
                BarRow(
                    bar = it,
                    accent = CrowColors.Danger,
                    fetchedAtMs = fetchedAtMs,
                    clock = clock,
                    shortcutHint = if (onNerveClick != null) "Crimes" else null,
                    testTag = "bar_nerve_shortcut",
                    onClick = onNerveClick
                )
            }
            bars.happy?.let {
                BarRow(
                    bar = it,
                    accent = CrowColors.AccentD,
                    fetchedAtMs = fetchedAtMs,
                    clock = clock
                )
            }
            bars.life?.let {
                BarRow(
                    bar = it,
                    accent = CrowColors.Info,
                    fetchedAtMs = fetchedAtMs,
                    clock = clock
                )
            }
        }
    }
}

@Composable
private fun BarRow(
    bar: BarValue,
    accent: Color,
    fetchedAtMs: Long,
    clock: Clock,
    shortcutHint: String? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    val progress = if (bar.maximum > 0) (bar.current.toFloat() / bar.maximum.toFloat()).coerceIn(0f, 1f) else 0f
    val targetFullMs = if (bar.current < bar.maximum && bar.fullTimeSec != null && bar.fullTimeSec > 0L) {
        fetchedAtMs + bar.fullTimeSec * 1_000L
    } else {
        null
    }

    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .let { m ->
            if (onClick != null) {
                m.defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            } else {
                m.padding(horizontal = 4.dp, vertical = 4.dp)
            }
        }
        .let { m -> if (testTag != null) m.testTag(testTag) else m }

    Column(
        modifier = rowModifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${bar.label}  ${bar.current}/${bar.maximum}",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = CrowColors.Text,
                    fontWeight = FontWeight.Medium
                )
                if (shortcutHint != null) {
                    Text(
                        text = shortcutHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.SheenSoft
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open $shortcutHint in new tab",
                        tint = CrowColors.SheenSoft,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
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
