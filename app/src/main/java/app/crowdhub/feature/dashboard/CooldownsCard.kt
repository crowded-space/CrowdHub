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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornCooldowns

@Composable
fun CooldownsCard(
    cooldowns: TornCooldowns,
    fetchedAtMs: Long,
    clock: Clock,
    dimmed: Boolean
) {
    CrowCard(dimmed = dimmed) {
        Text(
            text = "Cooldowns",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = CrowColors.Text
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CooldownRow("Drug", cooldowns.drugSec, fetchedAtMs, clock)
            CooldownRow("Medical", cooldowns.medicalSec, fetchedAtMs, clock)
            CooldownRow("Booster", cooldowns.boosterSec, fetchedAtMs, clock)
        }
    }
}

@Composable
private fun CooldownRow(
    label: String,
    remainingSec: Long,
    fetchedAtMs: Long,
    clock: Clock
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CrowColors.TextMuted)
        if (remainingSec <= 0L) {
            Text("Ready", style = MaterialTheme.typography.bodyMedium, color = CrowColors.Ok)
        } else {
            CountdownText(
                targetEpochMs = fetchedAtMs + remainingSec * 1_000L,
                clock = clock,
                readyText = "Ready",
                color = CrowColors.AccentD
            )
        }
    }
}
