package app.crowdhub.feature.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.crowdhub.core.browser.WebLoadError
import app.crowdhub.core.browser.WebSessionState
import app.crowdhub.core.settings.QuickLinkItem
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserTopAppBar(
    pageTitle: String,
    isChallengeActive: Boolean,
    sessionState: WebSessionState,
    scriptsKillSwitch: Boolean,
    resolvedAccountId: String?,
    allLinks: List<QuickLinkItem>,
    defaultLinksCount: Int,
    customLinks: List<QuickLinkItem>,
    showQuickLinks: Boolean,
    onShowQuickLinksChange: (Boolean) -> Unit,
    showScriptsMenu: Boolean,
    onShowScriptsMenuChange: (Boolean) -> Unit,
    onExitBrowser: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onQuickLinkClick: (QuickLinkItem) -> Unit,
    onQuickLinkLongClick: (QuickLinkItem) -> Unit,
    onDeleteCustomLink: (QuickLinkItem) -> Unit,
    onAddQuickLinkClick: () -> Unit,
    onToggleScriptsKillSwitch: (Boolean) -> Unit,
    onOpenScripts: () -> Unit,
    onResetCookies: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onExitBrowser,
                modifier = Modifier.testTag("browser_exit_button")
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Return to CrowdHub")
            }
        },
        title = {
            Column {
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sessionText = when {
                    isChallengeActive -> "Verifying security check..."
                    sessionState == WebSessionState.LOGGED_IN -> "Logged in"
                    sessionState == WebSessionState.LOGGED_OUT -> "Logged out"
                    else -> "Torn Browser"
                }
                val sessionColor = when {
                    isChallengeActive -> CrowColors.AccentD
                    sessionState == WebSessionState.LOGGED_IN -> CrowColors.Ok
                    sessionState == WebSessionState.LOGGED_OUT -> CrowColors.Danger
                    else -> CrowColors.TextMuted
                }
                Text(
                    text = sessionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = sessionColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
        actions = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = onReload) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Reload")
            }
            IconButton(onClick = onGoHome) {
                Icon(Icons.Outlined.Home, contentDescription = "Torn Home")
            }

            Box {
                IconButton(onClick = { onShowQuickLinksChange(true) }) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Quick links")
                }
                DropdownMenu(
                    expanded = showQuickLinks,
                    onDismissRequest = { onShowQuickLinksChange(false) }
                ) {
                    Text(
                        text = "Tap to open · Hold for new tab",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    allLinks.forEachIndexed { idx, link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        onShowQuickLinksChange(false)
                                        onQuickLinkClick(link)
                                    },
                                    onLongClick = {
                                        onShowQuickLinksChange(false)
                                        onQuickLinkLongClick(link)
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val visual = BrowserIconResolver.resolve(link.url, link.label)
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = CrowColors.SheenSoft,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(link.label, style = MaterialTheme.typography.bodyMedium, color = CrowColors.Text)
                            }
                            if (idx >= defaultLinksCount && link in customLinks) {
                                IconButton(
                                    onClick = { onDeleteCustomLink(link) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Remove link",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add current page") },
                        onClick = {
                            onShowQuickLinksChange(false)
                            onAddQuickLinkClick()
                        }
                    )
                }
            }

            Box {
                IconButton(onClick = { onShowScriptsMenuChange(true) }) {
                    Icon(
                        Icons.Outlined.Code,
                        contentDescription = "Scripts menu",
                        tint = if (scriptsKillSwitch) CrowColors.Danger else CrowColors.Text
                    )
                }
                DropdownMenu(
                    expanded = showScriptsMenu,
                    onDismissRequest = { onShowScriptsMenuChange(false) }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Disable all scripts")
                                Switch(
                                    checked = scriptsKillSwitch,
                                    onCheckedChange = onToggleScriptsKillSwitch
                                )
                            }
                        },
                        onClick = { onToggleScriptsKillSwitch(!scriptsKillSwitch) }
                    )
                    DropdownMenuItem(
                        text = { Text("Manage Scripts") },
                        onClick = {
                            onShowScriptsMenuChange(false)
                            onOpenScripts()
                        }
                    )
                    if (resolvedAccountId != null) {
                        DropdownMenuItem(
                            text = { Text("Reset web session cookies") },
                            onClick = {
                                onShowScriptsMenuChange(false)
                                onResetCookies()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BrowserLoadErrorOverlay(
    error: WebLoadError,
    onRetry: () -> Unit,
    onResetAndRetry: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrowColors.CrowBlack.copy(alpha = 0.92f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        CrowCard(borderColor = CrowColors.Danger.copy(alpha = 0.5f)) {
            Text("Page Failed to Load", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(error.message, style = MaterialTheme.typography.bodySmall, color = CrowColors.Danger)
            Spacer(Modifier.height(4.dp))
            Text(error.url, style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("browser_retry_button")
                ) { Text("Retry") }
                OutlinedButton(onClick = onResetAndRetry) { Text("Reset & Retry") }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenExternal) { Text("Open in external browser") }
        }
    }
}
