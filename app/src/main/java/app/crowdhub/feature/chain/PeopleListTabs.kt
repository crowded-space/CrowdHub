package app.crowdhub.feature.chain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.crowdhub.connector.ffscouter.BsText
import app.crowdhub.connector.ffscouter.FfBadge
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.flight.FlightChip
import app.crowdhub.provider.torn.TornUserListCategory

@Composable
internal fun PeopleSearchAndStatusHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    statusFilter: TargetStatusFilter,
    onStatusFilterChange: (TargetStatusFilter) -> Unit,
    showStatusFilters: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search name or ID...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("people_search_input")
        )
        if (showStatusFilters) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetStatusFilter.entries.forEach { f ->
                    FilterChip(
                        selected = statusFilter == f,
                        onClick = { onStatusFilterChange(f) },
                        label = { Text(f.label) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun PeopleApiListSection(
    category: TornUserListCategory,
    items: List<PeopleUserListItem>,
    fetchedAtMs: Long?,
    errorMessage: String?,
    ffEnabled: Boolean,
    ffLow: Double,
    ffHigh: Double,
    clock: Clock,
    onRefresh: () -> Unit,
    onToggleStakeout: (PeopleUserListItem) -> Unit,
    onSaveToAdditional: (PeopleUserListItem) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAttack: (Long) -> Unit
) {
    if (errorMessage != null) {
        OfflineBanner(message = errorMessage)
    }
    if (fetchedAtMs != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${items.size} ${category.label.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.TextMuted
            )
            StaleBadge(fetchedAtMs = fetchedAtMs, clock = clock)
        }
    }

    if (items.isEmpty()) {
        EmptyView(
            title = "No ${category.label.lowercase()} loaded",
            subtitle = errorMessage ?: "Tap Refresh to sync your in-game Torn ${category.label} list.",
            actionLabel = "Refresh",
            onAction = onRefresh
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { "${category.apiValue}_${it.playerId}" }) { item ->
                CrowCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.name}${item.level?.let { " · Lv $it" } ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrowColors.Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val act = item.lastActionRelative ?: item.lastActionStatus
                                if (!act.isNullOrBlank()) {
                                    Text(
                                        text = "Last action: $act",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrowColors.TextMuted
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onToggleStakeout(item) }) {
                                    Icon(
                                        imageVector = if (item.stakedOut) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (item.stakedOut) "Remove stakeout" else "Add to stakeout",
                                        tint = if (item.stakedOut) CrowColors.AccentD else CrowColors.TextMuted
                                    )
                                }
                                IconButton(onClick = { onSaveToAdditional(item) }) {
                                    Icon(
                                        imageVector = if (item.inAdditional) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkAdd,
                                        contentDescription = "Save to Additional list",
                                        tint = if (item.inAdditional) CrowColors.SheenSoft else CrowColors.TextMuted
                                    )
                                }
                                IconButton(onClick = { onOpenProfile(item.playerId) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = "Open profile in new tab",
                                        tint = CrowColors.TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusChip(status = item.status)
                                if (item.status == StatusKind.HOSPITAL && item.hospitalLeavesAtSec != null) {
                                    CountdownText(
                                        targetEpochMs = item.hospitalLeavesAtSec * 1_000L,
                                        clock = clock
                                    )
                                }
                                if (ffEnabled) {
                                    FfBadge(item.fairFight, ffLow, ffHigh)
                                    BsText(item.bsEstimate, item.bsHuman)
                                }
                            }
                            Button(
                                onClick = { onAttack(item.playerId) },
                                modifier = Modifier.testTag("people_attack_${item.playerId}")
                            ) {
                                Text("Attack")
                            }
                        }

                        item.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                            Text(
                                text = "Note: $noteText",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrowColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PeopleTargetCardList(
    items: List<TargetItem>,
    ffEnabled: Boolean,
    ffLow: Double,
    ffHigh: Double,
    dimmed: Boolean,
    clock: Clock,
    isAdditionalTab: Boolean,
    onSaveToAdditional: (TargetItem) -> Unit,
    onToggleWatch: (TargetItem) -> Unit,
    onEditNote: (TargetItem) -> Unit,
    onRemove: (TargetItem) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAttack: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.playerId }) { item ->
            CrowCard(dimmed = dimmed) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name}${item.level?.let { " · Lv $it" } ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CrowColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.note?.takeIf { it.isNotBlank() }?.let { n ->
                                Text(n, style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleWatch(item) }) {
                                Icon(
                                    imageVector = if (item.watched) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (item.watched) "Remove stakeout" else "Stake out player",
                                    tint = if (item.watched) CrowColors.AccentD else CrowColors.TextMuted
                                )
                            }
                            if (!isAdditionalTab) {
                                IconButton(onClick = { onSaveToAdditional(item) }) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save to Additional")
                                }
                            } else {
                                IconButton(onClick = { onEditNote(item) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit note", tint = CrowColors.TextMuted)
                                }
                                IconButton(onClick = { onRemove(item) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove target", tint = CrowColors.Danger)
                                }
                            }
                            IconButton(onClick = { onOpenProfile(item.playerId) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open profile in new tab",
                                    tint = CrowColors.TextMuted
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(status = item.status)
                            if (item.status == StatusKind.HOSPITAL && item.hospitalLeavesAtSec != null) {
                                CountdownText(
                                    targetEpochMs = item.hospitalLeavesAtSec * 1_000L,
                                    clock = clock
                                )
                            }
                            if (ffEnabled) {
                                FfBadge(item.fairFight, ffLow, ffHigh)
                                BsText(item.bsEstimate, item.bsHuman)
                            }
                        }
                        Button(
                            onClick = { onAttack(item.playerId) },
                            modifier = Modifier.testTag("target_attack_${item.playerId}")
                        ) {
                            Text("Attack")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PeopleStakeoutSection(
    items: List<StakeoutHubItem>,
    ffEnabled: Boolean,
    ffLow: Double,
    ffHigh: Double,
    clock: Clock,
    onAddStakeoutClick: () -> Unit,
    onToggleStar: (StakeoutHubItem) -> Unit,
    onSelectDetail: (StakeoutHubItem) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAttack: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyView(
            title = "No players staked out",
            subtitle = "Star players from Friends, Targets, Enemies, Chain, War, or Faction, or add one by Player ID.",
            actionLabel = "Add Player ID",
            onAction = onAddStakeoutClick
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { "stake_${it.stakeout.playerId}" }) { item ->
                CrowCard(
                    modifier = Modifier.clickable { onSelectDetail(item) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.name}${item.level?.let { " · Lv $it" } ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrowColors.Text
                                )
                                Text(
                                    text = item.statusDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrowColors.TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onToggleStar(item) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = "Remove stakeout",
                                        tint = CrowColors.AccentD
                                    )
                                }
                                IconButton(onClick = { onOpenProfile(item.stakeout.playerId) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = "Open profile in new tab",
                                        tint = CrowColors.TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusChip(status = item.status)
                                if (item.status == StatusKind.HOSPITAL && item.hospitalLeavesAtSec != null) {
                                    CountdownText(
                                        targetEpochMs = item.hospitalLeavesAtSec * 1_000L,
                                        clock = clock
                                    )
                                }
                                if (ffEnabled) {
                                    FfBadge(item.fairFight, ffLow, ffHigh)
                                    BsText(item.bsEstimate, item.bsHuman)
                                }
                            }
                            Button(
                                onClick = { onAttack(item.stakeout.playerId) },
                                modifier = Modifier.testTag("stakeout_attack_${item.stakeout.playerId}")
                            ) {
                                Text("Attack")
                            }
                        }

                        item.flightEstimate?.let { est ->
                            FlightChip(estimate = est, clock = clock)
                        }

                        val activeAlerts = buildList {
                            if (item.stakeout.alertHospital) add("Hospital")
                            if (item.stakeout.alertLanding) add("Landing")
                            if (item.stakeout.alertOkay) add("Okay")
                            if (item.stakeout.alertOnline) add("Online")
                        }
                        Text(
                            text = if (activeAlerts.isEmpty()) {
                                "Alerts: none (tap card to configure)"
                            } else {
                                "Alerts: ${activeAlerts.joinToString(" · ")} (tap to edit)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.SheenSoft
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun StakeoutAlertToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CrowColors.Text)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
