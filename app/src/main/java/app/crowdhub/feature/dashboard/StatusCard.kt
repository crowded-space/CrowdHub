package app.crowdhub.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornBasicInfo

@Composable
fun StatusCard(
    basic: TornBasicInfo,
    clock: Clock,
    dimmed: Boolean
) {
    CrowCard(dimmed = dimmed) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${basic.name}${basic.level?.let { " · Lv $it" } ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = basic.status.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
            }
            StatusChip(status = basic.status.state)
        }
        val untilSec = basic.status.untilEpochSec
        if (untilSec != null && untilSec * 1_000L > clock.nowEpochMs()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ends in", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                CountdownText(
                    targetEpochMs = untilSec * 1_000L,
                    clock = clock,
                    readyText = "Ended",
                    color = CrowColors.AccentD
                )
            }
        }
    }
}
