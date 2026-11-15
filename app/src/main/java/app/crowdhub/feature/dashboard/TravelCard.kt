package app.crowdhub.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.LocalTimeFormatter
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornTravel

@Composable
fun TravelCard(
    travel: TornTravel,
    statusState: StatusKind,
    clock: Clock,
    dimmed: Boolean,
    onLandingReached: () -> Unit
) {
    val isFlying = statusState == StatusKind.TRAVELING || travel.isActivelyFlying
    val isAbroad = statusState == StatusKind.ABROAD && !isFlying
    if (!isFlying && !isAbroad) return

    val arrivalMs = travel.arrivalEpochSec?.let { it * 1_000L }
    val departedMs = travel.departedEpochSec?.let { it * 1_000L }
    val nowMs = clock.nowEpochMs()

    LaunchedEffect(isFlying, arrivalMs) {
        if (isFlying && arrivalMs != null && arrivalMs <= nowMs) {
            onLandingReached()
        }
    }

    CrowCard(
        dimmed = dimmed,
        borderColor = if (isFlying) CrowColors.Info.copy(alpha = 0.5f) else CrowColors.Abroad.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isFlying) "Flight Tracker" else "Abroad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CrowColors.Text
            )
            StatusChip(status = if (isFlying) StatusKind.TRAVELING else StatusKind.ABROAD)
        }
        Spacer(Modifier.height(8.dp))

        if (isFlying) {
            val directionLabel = if (travel.destination.equals("Torn", ignoreCase = true)) {
                "Returning to Torn"
            } else {
                "Outbound to ${travel.destination}"
            }
            Text(
                text = directionLabel + (travel.method?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CrowColors.Text
            )
            Spacer(Modifier.height(6.dp))

            if (arrivalMs != null) {
                val arrivalLocal = LocalTimeFormatter.formatTimeHms(arrivalMs)
                val zoneLabel = LocalTimeFormatter.shortZoneLabel(arrivalMs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Arrives $arrivalLocal $zoneLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                    if (arrivalMs <= nowMs) {
                        Text(
                            text = "Landing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrowColors.Ok,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        CountdownText(
                            targetEpochMs = arrivalMs,
                            clock = clock,
                            readyText = "Landing...",
                            color = CrowColors.Info
                        )
                    }
                }
                if (departedMs != null && arrivalMs > departedMs) {
                    Spacer(Modifier.height(8.dp))
                    val progress = ((nowMs - departedMs).toFloat() / (arrivalMs - departedMs).toFloat())
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CrowColors.Info,
                        trackColor = CrowColors.SurfaceHigh
                    )
                }
            }
        } else {
            Text(
                text = "Currently in ${travel.destination}",
                style = MaterialTheme.typography.bodyMedium,
                color = CrowColors.Text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Not flying · Returning flights are tracked once departed.",
                style = MaterialTheme.typography.bodySmall,
                color = CrowColors.TextMuted
            )
        }
    }
}
