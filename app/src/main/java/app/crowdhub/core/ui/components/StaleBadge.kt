package app.crowdhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.theme.CrowColors
import kotlinx.coroutines.delay

@Composable
fun StaleBadge(
    fetchedAtMs: Long?,
    clock: Clock,
    refreshIntervalMs: Long = 30_000L,
    modifier: Modifier = Modifier
) {
    if (fetchedAtMs == null) return
    var nowMs by remember(fetchedAtMs) { mutableLongStateOf(clock.nowEpochMs()) }
    LaunchedEffect(fetchedAtMs) {
        while (true) {
            nowMs = clock.nowEpochMs()
            delay(5_000L)
        }
    }
    val ageMs = (nowMs - fetchedAtMs).coerceAtLeast(0L)
    val isStale = ageMs > refreshIntervalMs * 2L
    val textColor = if (isStale) CrowColors.AccentD else CrowColors.TextMuted
    val bgColor = if (isStale) CrowColors.AccentD.copy(alpha = 0.12f) else CrowColors.SurfaceHigh
    val borderColor = if (isStale) CrowColors.AccentD.copy(alpha = 0.45f) else CrowColors.Outline

    Text(
        text = if (isStale) "Stale · updated ${formatRelativeAge(ageMs)}" else "Updated ${formatRelativeAge(ageMs)}",
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
