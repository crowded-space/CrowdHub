package app.crowdhub.feature.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.theme.CrowColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserTabStrip(
    tabs: List<BrowserTabRuntime>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseOtherTabs: (String) -> Unit,
    onDuplicateTab: (String) -> Unit,
    onReloadTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuTabId by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CrowColors.Surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId
                val visual = remember(tab.url, tab.title) {
                    BrowserIconResolver.resolve(tab.url, tab.title)
                }
                val bgColor = if (isSelected) CrowColors.Sheen.copy(alpha = 0.22f) else CrowColors.SurfaceHigh
                val borderColor = if (isSelected) CrowColors.SheenSoft else CrowColors.Outline.copy(alpha = 0.6f)
                val iconTint = if (isSelected) CrowColors.SheenSoft else CrowColors.TextMuted

                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(width = 48.dp, height = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSelected) {
                                        menuTabId = tab.id
                                    } else {
                                        onSelectTab(tab.id)
                                    }
                                },
                                onLongClick = { menuTabId = tab.id }
                            )
                            .testTag("browser_tab_${tab.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = visual.contentDescription,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            when {
                                tab.loadError != null -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-6).dp, y = 6.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CrowColors.Danger)
                                    )
                                }
                                tab.progress in 1..99 -> {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-6).dp, y = 6.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CrowColors.AccentD)
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(CrowColors.SheenSoft, RoundedCornerShape(1.dp))
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuTabId == tab.id,
                        onDismissRequest = { menuTabId = null }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab.title.ifBlank { visual.contentDescription },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrowColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tab.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrowColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.4f))
                        DropdownMenuItem(
                            text = { Text("Reload tab") },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                            onClick = {
                                menuTabId = null
                                onReloadTab(tab.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate in new tab") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuTabId = null
                                onDuplicateTab(tab.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Close tab") },
                            leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                            onClick = {
                                menuTabId = null
                                onCloseTab(tab.id)
                            }
                        )
                        if (tabs.size > 1) {
                            DropdownMenuItem(
                                text = { Text("Close other tabs") },
                                onClick = {
                                    menuTabId = null
                                    onCloseOtherTabs(tab.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onNewTab,
            modifier = Modifier
                .size(48.dp)
                .testTag("browser_new_tab_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "New browser tab",
                tint = CrowColors.SheenSoft
            )
        }
    }
}
