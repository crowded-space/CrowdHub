package app.crowdhub.connector.ffscouter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import java.util.Locale

@Composable
fun FfBadge(
    fairFight: Double?,
    lowThreshold: Double = 2.0,
    highThreshold: Double = 3.0,
    modifier: Modifier = Modifier
) {
    var showInfo by remember { mutableStateOf(false) }
    val color = when {
        fairFight == null -> CrowColors.TextMuted
        fairFight >= highThreshold -> CrowColors.AccentD
        fairFight >= lowThreshold -> CrowColors.Ok
        else -> CrowColors.TextMuted
    }
    val text = if (fairFight == null) {
        "FF —"
    } else {
        "FF ${String.format(Locale.US, "%.2f", fairFight)} est."
    }

    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .clickable { showInfo = true }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("FFScouter Estimate") },
            text = { Text("Estimate from FFScouter; may be outdated.") },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun BsText(
    bsEstimate: Long?,
    bsHuman: String?,
    modifier: Modifier = Modifier
) {
    val label = when {
        !bsHuman.isNullOrBlank() -> "BS $bsHuman est."
        bsEstimate != null -> "BS ${formatCompactNumber(bsEstimate)} est."
        else -> "BS —"
    }
    Text(
        text = label,
        color = CrowColors.TextMuted,
        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
        modifier = modifier
    )
}

fun formatCompactNumber(value: Long): String {
    return when {
        value >= 1_000_000_000_000L -> String.format(Locale.US, "%.2ft", value / 1_000_000_000_000.0)
        value >= 1_000_000_000L -> String.format(Locale.US, "%.2fb", value / 1_000_000_000.0)
        value >= 1_000_000L -> String.format(Locale.US, "%.2fm", value / 1_000_000.0)
        value >= 1_000L -> String.format(Locale.US, "%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}

@Composable
fun FfConsentDialog(
    onOpenSite: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable FFScouter?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Before enabling FFScouter, note:")
                Text("1. Your Torn API key will be sent to ffscouter.com.")
                Text("2. FFScouter states it stores the key and restricts access to its service owners for debugging.")
                Text("3. You must already be registered at ffscouter.com.")
                Text("4. You can disable FFScouter anytime (clears cached estimates).")
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onOpenSite) {
                    Text("Open ffscouter.com")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("ff_consent_confirm_button")
            ) {
                Text("I understand, enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
