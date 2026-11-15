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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.LocalTimeFormatter
import app.crowdhub.core.ui.components.formatDurationSec
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.TravelDirection
import kotlinx.coroutines.delay

@Composable
fun FlightChip(
    estimate: FlightEstimate,
    clock: Clock,
    modifier: Modifier = Modifier
) {
    var showExplain by remember { mutableStateOf(false) }
    var nowMs by remember(estimate) { mutableLongStateOf(clock.nowEpochMs()) }

    LaunchedEffect(estimate) {
        while (true) {
            nowMs = clock.nowEpochMs()
            delay(1_000L)
        }
    }

    val dotColor = when (estimate.confidence) {
        FlightConfidence.HIGH, FlightConfidence.GREEN -> CrowColors.Ok
        FlightConfidence.YELLOW -> CrowColors.AccentD
        FlightConfidence.GRAY -> CrowColors.TextMuted
    }

    val startSec = ((estimate.windowStartMs - nowMs) / 1_000L).coerceAtLeast(0L)
    val endSec = ((estimate.windowEndMs - nowMs) / 1_000L).coerceAtLeast(0L)
    val localWindow = LocalTimeFormatter.formatTimeRange(
        startMs = estimate.windowStartMs,
        endMs = estimate.windowEndMs,
        includeSeconds = false
    )
    val localExactEnd = LocalTimeFormatter.formatTimeHm(estimate.windowEndMs)

    val labelText = when {
        estimate.isDelayed -> "Delayed +${formatDurationSec(estimate.overdueByMs / 1_000L)} · est. $localExactEnd"
        estimate.usedApiUntil || (endSec - startSec) <= 120L ->
            "Lands ~${formatDurationSec(startSec)} · $localExactEnd"
        else ->
            "Lands ~${formatDurationSec(startSec)}–${formatDurationSec(endSec)} · $localWindow"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(dotColor.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, dotColor.copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
            .clickable { showExplain = true }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("flight_eta_chip"),
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
        val zoneShort = LocalTimeFormatter.shortZoneLabel(estimate.windowEndMs)
        val zoneFull = LocalTimeFormatter.fullZoneLabel()
        val directionLabel = when (estimate.direction) {
            TravelDirection.OUT -> "Outbound to ${estimate.place.displayName}"
            TravelDirection.RETURN -> "Returning from ${estimate.place.displayName} to Torn"
            TravelDirection.ABROAD -> "Abroad in ${estimate.place.displayName}"
            TravelDirection.UNKNOWN -> estimate.place.displayName
        }
        val lengthLabel = when {
            estimate.baseMinutesMin == null -> "Unknown"
            estimate.baseMinutesMin == estimate.baseMinutesMax -> "${estimate.baseMinutesMin} min"
            else -> "${estimate.baseMinutesMin}–${estimate.baseMinutesMax} min (range)"
        }
        val bookLabel = when (estimate.hasBook) {
            true -> "Owned (-25% flight time)"
            false -> "Not active (100%)"
            null -> "Unknown (75%–100% range)"
        }
        val departLabel = if (estimate.departWindowStartMs != null && estimate.departWindowEndMs != null) {
            val pollGapSec = ((estimate.departWindowEndMs - estimate.departWindowStartMs) / 1_000L).coerceAtLeast(0L)
            val departClock = LocalTimeFormatter.formatTimeRange(
                startMs = estimate.departWindowStartMs,
                endMs = estimate.departWindowEndMs,
                includeSeconds = true
            )
            "Observed at $departClock (±${pollGapSec}s poll window)"
        } else {
            val seenTime = estimate.firstSeenAtMs?.let { LocalTimeFormatter.formatTimeHms(it) } ?: "Unknown"
            "Mid-flight when first seen ($seenTime $zoneShort)"
        }
        val localRangeHms = LocalTimeFormatter.formatTimeRange(
            startMs = estimate.windowStartMs,
            endMs = estimate.windowEndMs,
            includeSeconds = true
        )
        val countdownSummary = if (estimate.usedApiUntil || (endSec - startSec) <= 120L) {
            formatDurationSec(startSec)
        } else {
            "${formatDurationSec(startSec)} – ${formatDurationSec(endSec)}"
        }

        AlertDialog(
            onDismissRequest = { showExplain = false },
            title = { Text("Flight ETA Breakdown") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BreakdownLine("Destination", directionLabel)
                    BreakdownLine("Travel Type", estimate.category.label)
                    BreakdownLine("Flight Length", lengthLabel)
                    BreakdownLine("Book Modifier", bookLabel)
                    BreakdownLine("Status Change", departLabel)
                    BreakdownLine(
                        "Tolerance",
                        if (estimate.usedApiUntil) "Exact API arrival timestamp"
                        else "±3% flight variance + poll detection window"
                    )
                    HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.5f))
                    BreakdownLine("Countdown", countdownSummary)
                    BreakdownLine("Local ETA ($zoneShort)", localRangeHms)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "System timezone: $zoneFull",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.SheenSoft
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplain = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CrowColors.TextMuted,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.merge(MonospaceNumberStyle),
            color = CrowColors.Text,
            fontWeight = FontWeight.Medium
        )
    }
}
