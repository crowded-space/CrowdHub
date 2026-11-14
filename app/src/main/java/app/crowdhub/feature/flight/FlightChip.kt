package app.crowdhub.feature.flight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.formatDurationSec
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle

@Composable
fun FlightChip(
    estimate: FlightEstimate,
    clock: Clock,
    modifier: Modifier = Modifier
) {
    var showExplain by remember { mutableStateOf(false) }
    val nowMs = clock.nowEpochMs()

    val dotColor = when (estimate.confidence) {
        FlightConfidence.HIGH, FlightConfidence.GREEN -> CrowColors.Ok
        FlightConfidence.YELLOW -> CrowColors.AccentD
        FlightConfidence.GRAY -> CrowColors.TextMuted
    }

    val startSec = ((estimate.windowStartMs - nowMs) / 1_000L).coerceAtLeast(0L)
    val endSec = ((estimate.windowEndMs - nowMs) / 1_000L).coerceAtLeast(0L)
    val labelText = when {
        estimate.isDelayed -> "Delayed (+${formatDurationSec(estimate.overdueByMs / 1_000L)}) est."
        estimate.usedApiUntil || (endSec - startSec) <= 120L ->
            "Lands ~${formatDurationSec(startSec)} est."
        else ->
            "Lands ~${formatDurationSec(startSec)}–${formatDurationSec(endSec)} est."
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(dotColor.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, dotColor.copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
            .clickable { showExplain = true }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = labelText,
            color = dotColor,
            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
            fontWeight = FontWeight.SemiBold
        )
    }

    if (showExplain) {
        AlertDialog(
            onDismissRequest = { showExplain = false },
            title = { Text("Flight ETA Estimate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Place: ${estimate.place.displayName} (${estimate.direction.name})")
                    Text("Plane category: ${estimate.category.label}")
                    Text("Confidence: ${estimate.confidence.name}")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Uncertainty sources: 30s polling interval, ±3% flight variance, unknown book ('Mailing Yourself Abroad' -25%), Detective Agency Watchlist delays, and hidden travel (10-star Theater).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplain = false }) { Text("OK") }
            }
        )
    }
}
