package app.crowdhub.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.crowdhub.core.browser.UrlDecision
import app.crowdhub.core.ui.theme.CrowColors

@Composable
fun LinkLongPressDialog(
    url: String,
    decision: UrlDecision,
    onDismiss: () -> Unit,
    onOpenNewTab: () -> Unit,
    onOpenBackgroundTab: () -> Unit,
    onOpenCurrentTab: () -> Unit,
    onOpenExternal: () -> Unit,
    onCopyLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                when (decision) {
                    UrlDecision.AllowInApp -> {
                        Button(
                            onClick = onOpenNewTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_open_new_tab_button")
                        ) { Text("Open in new tab") }
                        OutlinedButton(
                            onClick = onOpenBackgroundTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_open_bg_tab_button")
                        ) { Text("Open in background tab") }
                        OutlinedButton(
                            onClick = onOpenCurrentTab,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open in current tab") }
                    }
                    is UrlDecision.ConfirmExternal -> {
                        Button(
                            onClick = onOpenExternal,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open in external browser") }
                    }
                    is UrlDecision.Block -> {
                        Text(
                            text = decision.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrowColors.Danger
                        )
                    }
                }
                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy link URL") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExternalConfirmDialog(
    url: String,
    onDismiss: () -> Unit,
    onConfirmExternal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open in your browser?") },
        text = { Text("External host is not allowed inside Crowd Hub:\n$url") },
        confirmButton = {
            Button(onClick = onConfirmExternal) { Text("Open externally") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddQuickLinkDialog(
    currentUrl: String,
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var label by remember(initialTitle) { mutableStateOf(initialTitle.take(20)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Quick Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true
                )
                Text(
                    text = currentUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(label.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
