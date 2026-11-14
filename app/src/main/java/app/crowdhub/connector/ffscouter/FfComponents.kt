package app.crowdhub.connector.ffscouter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    hasExistingCustomKey: Boolean = false,
    onOpenSite: () -> Unit,
    onConfirm: (customApiKey: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var customKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable FFScouter?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Before enabling FFScouter, note:")
                Text(
                    "1. Requests to ffscouter.com use your FFScouter API key (or your Torn API key if no separate key is entered).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "2. FFScouter states it stores submitted keys and restricts access to its service owners for debugging.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "3. Your key must be registered at ffscouter.com.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "4. You can edit, remove, or disable FFScouter anytime (clears cached estimates).",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = customKey,
                    onValueChange = { customKey = it },
                    label = {
                        Text(
                            if (hasExistingCustomKey) "New FFScouter API key (optional)"
                            else "FFScouter API key (optional)"
                        )
                    },
                    placeholder = { Text("Leave blank to use Torn API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ff_consent_key_input")
                )

                OutlinedButton(
                    onClick = onOpenSite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open ffscouter.com")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(customKey.trim().takeIf { it.isNotEmpty() }) },
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
