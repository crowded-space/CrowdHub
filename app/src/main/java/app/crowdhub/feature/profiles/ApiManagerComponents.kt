package app.crowdhub.feature.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.crowdhub.connector.ffscouter.FfKeyInfo
import app.crowdhub.core.db.ProviderAccountEntity
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle

@Composable
fun AccountSummaryCard(
    account: ProviderAccountEntity?,
    ffKeyInfo: FfKeyInfo,
    ffHasError: Boolean
) {
    val tornOk = account?.status == "VALID"
    val ffBadgeColor = when {
        !ffKeyInfo.isEnabled -> CrowColors.TextMuted
        ffHasError -> CrowColors.Danger
        else -> CrowColors.Ok
    }
    val ffBadgeLabel = when {
        !ffKeyInfo.isEnabled -> "FF: Off"
        ffHasError -> "FF: Action needed"
        ffKeyInfo.hasCustomKey -> "FF: Custom key"
        else -> "FF: Active"
    }

    CrowCard(borderColor = CrowColors.Sheen.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.displayName ?: "Torn Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Player ID: ${account?.externalId ?: "—"}",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ApiStatusPill(
                    label = "Torn: ${account?.status ?: "UNKNOWN"}",
                    color = if (tornOk) CrowColors.Ok else CrowColors.Danger
                )
                ApiStatusPill(
                    label = ffBadgeLabel,
                    color = ffBadgeColor
                )
            }
        }
    }
}

@Composable
fun TornApiManagerCard(
    account: ProviderAccountEntity?,
    maskedKey: String,
    isTesting: Boolean,
    testMessage: String?,
    testIsError: Boolean,
    clock: Clock,
    onReplaceKey: () -> Unit,
    onTestKey: () -> Unit,
    onRemoveKey: () -> Unit,
    onOpenTornApiPage: () -> Unit
) {
    val status = account?.status ?: "UNKNOWN"
    val statusColor = when (status) {
        "VALID" -> CrowColors.Ok
        "PAUSED" -> CrowColors.AccentD
        else -> CrowColors.Danger
    }

    CrowCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = CrowColors.SheenSoft,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Torn City API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CrowColors.Text
                    )
                    Text(
                        text = "api.torn.com · ${account?.accessLevel ?: "Limited"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted
                    )
                }
            }
            ApiStatusPill(label = status, color = statusColor)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Key: $maskedKey",
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
            color = CrowColors.Text
        )
        account?.lastValidatedAt?.let { validatedAt ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Last verified: ${formatRelativeAge(clock.nowEpochMs() - validatedAt)}",
                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                color = CrowColors.TextMuted
            )
        }

        if (testMessage != null) {
            Spacer(Modifier.height(8.dp))
            InlineFeedbackRow(message = testMessage, isError = testIsError)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onReplaceKey,
                modifier = Modifier
                    .weight(1f)
                    .testTag("replace_key_button")
            ) {
                Text("Replace key")
            }
            OutlinedButton(
                onClick = onTestKey,
                enabled = !isTesting,
                modifier = Modifier
                    .weight(1f)
                    .testTag("torn_test_key_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CrowColors.Sheen
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text("Test key")
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenTornApiPage,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Torn API page")
            }
            OutlinedButton(
                onClick = onRemoveKey,
                modifier = Modifier
                    .weight(1f)
                    .testTag("torn_remove_key_button")
            ) {
                Text("Remove key")
            }
        }
    }
}

@Composable
fun FfScouterApiManagerCard(
    ffKeyInfo: FfKeyInfo,
    isTesting: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onSetOrReplaceKey: () -> Unit,
    onTestConnection: () -> Unit,
    onRemoveCustomKey: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onOpenSite: () -> Unit
) {
    val pillLabel = when {
        !ffKeyInfo.isEnabled -> "DISABLED"
        isError -> "CHECK KEY"
        else -> "ENABLED"
    }
    val pillColor = when {
        !ffKeyInfo.isEnabled -> CrowColors.TextMuted
        isError -> CrowColors.Danger
        else -> CrowColors.Ok
    }

    val keySourceDesc = when {
        ffKeyInfo.hasCustomKey -> "Key: ${ffKeyInfo.maskedCustomKey} (Custom FFScouter key)"
        ffKeyInfo.hasTornFallback -> "Key: ${ffKeyInfo.maskedTornKey} (Using Torn API key)"
        else -> "Key: Not set"
    }

    CrowCard(
        borderColor = if (ffKeyInfo.isEnabled && isError) {
            CrowColors.Danger.copy(alpha = 0.5f)
        } else {
            CrowColors.Outline
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = CrowColors.AccentD,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "FFScouter API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CrowColors.Text
                    )
                    Text(
                        text = "ffscouter.com · Fair Fight & BS estimates",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted
                    )
                }
            }
            ApiStatusPill(label = pillLabel, color = pillColor)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = keySourceDesc,
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
            color = CrowColors.Text
        )
        if (!ffKeyInfo.hasCustomKey) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Have a separate FFScouter API key? Enter it below to use it instead of your Torn key.",
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.TextMuted
            )
        }

        if (statusMessage != null) {
            Spacer(Modifier.height(8.dp))
            InlineFeedbackRow(message = statusMessage, isError = isError)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSetOrReplaceKey,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ff_set_key_button")
            ) {
                Text(if (ffKeyInfo.hasCustomKey) "Replace FF key" else "Enter FF key")
            }
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTesting && ffKeyInfo.hasAnyKey,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ff_test_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CrowColors.Sheen
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text("Test")
            }
        }

        if (ffKeyInfo.hasCustomKey) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRemoveCustomKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ff_remove_key_button")
            ) {
                Text(
                    if (ffKeyInfo.hasTornFallback) "Remove custom key (revert to Torn key)"
                    else "Remove FFScouter key"
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenSite,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("ffscouter.com")
            }
            if (ffKeyInfo.isEnabled) {
                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("disable_ff_button")
                ) {
                    Text("Disable")
                }
            } else {
                Button(
                    onClick = onEnable,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("enable_ff_button")
                ) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
fun TornKeyReplaceDialog(
    keyInput: String,
    error: String?,
    isBusy: Boolean,
    onKeyChange: (String) -> Unit,
    onOpenTornApiPage: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Replace Torn API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a Limited-access Torn API key. The key will be verified with api.torn.com before saving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = onKeyChange,
                    label = { Text("New Limited API key") },
                    singleLine = true,
                    enabled = !isBusy,
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
                        .testTag("torn_key_input")
                )
                if (error != null) {
                    Text(error, color = CrowColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onOpenTornApiPage,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Torn API page")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy && keyInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("torn_save_key_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Validate & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancel") }
        }
    )
}

@Composable
fun FfApiKeyDialog(
    hasExistingCustomKey: Boolean,
    isAlreadyEnabled: Boolean,
    keyInput: String,
    error: String?,
    isBusy: Boolean,
    onKeyChange: (String) -> Unit,
    onOpenSite: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(if (hasExistingCustomKey) "Replace FFScouter API Key" else "Enter FFScouter API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isAlreadyEnabled) {
                        "Your FFScouter key is encrypted locally and sent only to ffscouter.com to verify and fetch battle stat estimates."
                    } else {
                        "Saving validates your key with ffscouter.com and enables FFScouter estimates across War and Targets."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = onKeyChange,
                    label = { Text("FFScouter API key") },
                    singleLine = true,
                    enabled = !isBusy,
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
                        .testTag("ff_key_input")
                )
                if (error != null) {
                    Text(error, color = CrowColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onOpenSite,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open ffscouter.com")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy && keyInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("ff_save_key_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Validate & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancel") }
        }
    )
}

@Composable
private fun ApiStatusPill(
    label: String,
    color: Color
) {
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun InlineFeedbackRow(
    message: String,
    isError: Boolean
) {
    val color = if (isError) CrowColors.Danger else CrowColors.Ok
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

private fun formatRelativeAge(deltaMs: Long): String {
    val sec = (deltaMs / 1_000L).coerceAtLeast(0L)
    return when {
        sec < 60L -> "${sec}s ago"
        sec < 3_600L -> "${sec / 60L}m ago"
        sec < 86_400L -> "${sec / 3_600L}h ago"
        else -> "${sec / 86_400L}d ago"
    }
}
