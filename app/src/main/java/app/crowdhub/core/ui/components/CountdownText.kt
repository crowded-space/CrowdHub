package app.crowdhub.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import kotlinx.coroutines.delay

@Composable
fun CountdownText(
    targetEpochMs: Long?,
    clock: Clock,
    modifier: Modifier = Modifier,
    prefix: String = "",
    readyText: String = "Ready",
    color: Color = CrowColors.Text,
    readyColor: Color = CrowColors.Ok,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    var nowMs by remember(targetEpochMs) { mutableLongStateOf(clock.nowEpochMs()) }

    LaunchedEffect(targetEpochMs) {
        if (targetEpochMs == null) return@LaunchedEffect
        while (true) {
            nowMs = clock.nowEpochMs()
            if (nowMs >= targetEpochMs) break
            delay(1_000L)
        }
    }

    if (targetEpochMs == null) {
        Text(
            text = "—",
            color = CrowColors.TextMuted,
            style = style.merge(MonospaceNumberStyle),
            modifier = modifier
        )
        return
    }

    val remainingSec = ((targetEpochMs - nowMs) / 1_000L)
    if (remainingSec <= 0L) {
        Text(
            text = readyText,
            color = readyColor,
            style = style.merge(MonospaceNumberStyle),
            modifier = modifier
        )
    } else {
        Text(
            text = prefix + formatDurationSec(remainingSec),
            color = color,
            style = style.merge(MonospaceNumberStyle),
            modifier = modifier
        )
    }
}

fun formatDurationSec(totalSeconds: Long): String {
    val sec = totalSeconds.coerceAtLeast(0L)
    val days = sec / 86_400L
    val hours = (sec % 86_400L) / 3_600L
    val minutes = (sec % 3_600L) / 60L
    val seconds = sec % 60L
    return when {
        days > 0 -> "${days}d %02d:%02d:%02d".format(hours, minutes, seconds)
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}

fun formatRelativeAge(ageMs: Long): String {
    val sec = (ageMs / 1_000L).coerceAtLeast(0L)
    return when {
        sec < 5L -> "just now"
        sec < 60L -> "${sec}s ago"
        sec < 3_600L -> "${sec / 60L}m ago"
        sec < 86_400L -> "${sec / 3_600L}h ago"
        else -> "${sec / 86_400L}d ago"
    }
}
