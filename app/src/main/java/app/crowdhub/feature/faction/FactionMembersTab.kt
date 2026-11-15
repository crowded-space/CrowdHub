package app.crowdhub.feature.faction

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.feature.flight.FlightChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactionMembersTab(
    state: FactionUiState,
    clock: Clock,
    onSearchChange: (String) -> Unit,
    onFilterChange: (FactionMemberFilter) -> Unit,
    onSortChange: (FactionMemberSort) -> Unit,
    onToggleStar: (Long) -> Unit,
    onAddToTargets: (Long, String, String) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAttack: (Long) -> Unit,
    onRetry: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val section = state.members
    var selectedMember by remember { mutableStateOf<FactionMemberItem?>(null) }

    when {
        section.isLoading && section.data == null -> {
            LoadingView("Loading faction members...")
        }
        section.data == null && section.error != null -> {
            ErrorView(
                error = section.error,
                onRetry = onRetry,
                onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
            )
        }
        else -> {
            val allMembers = section.data.orEmpty()
            val query = state.memberSearch.trim().lowercase()
            val filtered = remember(allMembers, query, state.memberFilter, state.memberSort) {
                allMembers
                    .filter { item ->
                        val m = item.member
                        val matchesQuery = query.isEmpty() ||
                            m.name.lowercase().contains(query) ||
                            (m.position?.lowercase()?.contains(query) == true) ||
                            m.status.description.lowercase().contains(query)
                        val matchesFilter = when (state.memberFilter) {
                            FactionMemberFilter.ALL -> true
                            FactionMemberFilter.OKAY -> m.status.state == StatusKind.OKAY
                            FactionMemberFilter.HOSPITAL -> m.status.state == StatusKind.HOSPITAL
                            FactionMemberFilter.TRAVELING -> m.status.state == StatusKind.TRAVELING
                            FactionMemberFilter.ABROAD -> m.status.state == StatusKind.ABROAD
                            FactionMemberFilter.ONLINE -> m.lastActionStatus.equals("Online", ignoreCase = true)
                        }
                        matchesQuery && matchesFilter
                    }
                    .let { list ->
                        when (state.memberSort) {
                            FactionMemberSort.FLIGHT_ETA -> list.sortedWith(
                                compareBy<FactionMemberItem>(
                                    { memberFlightSortGroup(it) },
                                    { it.flightEstimate?.windowStartMs ?: it.member.status.untilEpochSec?.times(1_000L) ?: Long.MAX_VALUE },
                                    { it.member.name.lowercase() }
                                )
                            )
                            FactionMemberSort.STATUS -> list.sortedWith(
                                compareBy<FactionMemberItem>(
                                    { it.member.status.state.ordinal },
                                    { it.member.status.untilEpochSec ?: Long.MAX_VALUE },
                                    { it.member.name.lowercase() }
                                )
                            )
                            FactionMemberSort.LEVEL -> list.sortedByDescending { it.member.level ?: 0 }
                            FactionMemberSort.NAME -> list.sortedBy { it.member.name.lowercase() }
                            FactionMemberSort.DAYS -> list.sortedByDescending { it.member.daysInFaction ?: 0 }
                        }
                    }
            }

            val onlineCount = remember(allMembers) {
                allMembers.count { it.member.lastActionStatus.equals("Online", ignoreCase = true) }
            }
            val flyingCount = remember(allMembers) {
                allMembers.count { it.member.status.state == StatusKind.TRAVELING }
            }
            val hospCount = remember(allMembers) {
                allMembers.count { it.member.status.state == StatusKind.HOSPITAL }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (section.isStale && section.error != null) {
                    OfflineBanner("Cached roster (${section.error.message})")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.memberSearch,
                        onValueChange = onSearchChange,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text("Search member, position, destination...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("faction_members_search")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FactionMemberFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.memberFilter == f,
                                onClick = { onFilterChange(f) },
                                label = { Text(f.label) },
                                modifier = Modifier.testTag("faction_member_filter_${f.name.lowercase()}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.TextMuted
                        )
                        FactionMemberSort.entries.forEach { s ->
                            FilterChip(
                                selected = state.memberSort == s,
                                onClick = { onSortChange(s) },
                                label = { Text(s.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filtered.size}/${allMembers.size} · $onlineCount online · $flyingCount flying · $hospCount hosp",
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = CrowColors.TextMuted
                        )
                        if (section.fetchedAtMs > 0L) {
                            StaleBadge(fetchedAtMs = section.fetchedAtMs, clock = clock)
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyView(
                        title = "No members match",
                        subtitle = if (allMembers.isEmpty()) "No roster members returned." else "Adjust your search or status filter."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.member.playerId }) { item ->
                            FactionMemberCard(
                                item = item,
                                clock = clock,
                                dimmed = section.isStale,
                                onToggleStar = { onToggleStar(item.member.playerId) },
                                onClick = { selectedMember = item }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMember?.let { item ->
        val m = item.member
        ModalBottomSheet(
            onDismissRequest = { selectedMember = null },
            containerColor = CrowColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${m.name} [${m.playerId}]",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                val roleLine = listOfNotNull(
                    m.position,
                    m.level?.let { "Lv $it" },
                    m.daysInFaction?.let { "$it days in faction" }
                ).joinToString(" · ")
                if (roleLine.isNotEmpty()) {
                    Text(roleLine, style = MaterialTheme.typography.labelMedium, color = CrowColors.SheenSoft)
                }
                Text("Status: ${m.status.description}", color = CrowColors.TextMuted)
                Text(
                    text = "Last action: ${m.lastActionStatus ?: "—"} (${m.lastActionRelative ?: "—"})",
                    color = CrowColors.TextMuted
                )
                item.flightEstimate?.let { est ->
                    FlightChip(estimate = est, clock = clock)
                }

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onAddToTargets(
                                m.playerId,
                                m.name,
                                "Faction (${state.basic.data?.name ?: "Member"})"
                            )
                            selectedMember = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add target") }

                    OutlinedButton(
                        onClick = {
                            onOpenProfile(m.playerId)
                            selectedMember = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Profile") }
                }
                Button(
                    onClick = {
                        onAttack(m.playerId)
                        selectedMember = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrowColors.Danger,
                        contentColor = CrowColors.CrowBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Attack / Spar") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun FactionMemberCard(
    item: FactionMemberItem,
    clock: Clock,
    dimmed: Boolean,
    onToggleStar: () -> Unit,
    onClick: () -> Unit
) {
    val m = item.member
    CrowCard(
        dimmed = dimmed,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("faction_member_row_${m.playerId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${m.name}${m.level?.let { " · Lv $it" } ?: ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                val metaParts = buildList {
                    m.position?.let { add(it) }
                    m.daysInFaction?.let { add("${it}d") }
                    val last = listOfNotNull(m.lastActionStatus, m.lastActionRelative).joinToString(" · ")
                    if (last.isNotEmpty()) add(last)
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            m.lastActionStatus.equals("Online", ignoreCase = true) -> CrowColors.Ok
                            m.lastActionStatus.equals("Idle", ignoreCase = true) -> CrowColors.AccentD
                            else -> CrowColors.TextMuted
                        }
                    )
                }
            }

            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (item.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (item.isStarred) "Unstar member" else "Star member alerts",
                    tint = if (item.isStarred) CrowColors.AccentD else CrowColors.TextMuted
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(status = m.status.state)

            if (m.status.state == StatusKind.HOSPITAL || m.status.state == StatusKind.JAIL) {
                val untilSec = m.status.untilEpochSec
                if (untilSec != null) {
                    CountdownText(
                        targetEpochMs = untilSec * 1_000L,
                        clock = clock,
                        readyText = "Out",
                        color = CrowColors.Danger
                    )
                }
            }

            if (m.status.state == StatusKind.TRAVELING) {
                if (item.flightEstimate != null) {
                    FlightChip(estimate = item.flightEstimate, clock = clock)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FlightTakeoff,
                            contentDescription = null,
                            tint = CrowColors.Abroad,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = m.status.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.Abroad
                        )
                    }
                }
            } else if (m.status.state == StatusKind.ABROAD) {
                Text(
                    text = m.status.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.Abroad
                )
            }

            if (item.postLandingGraceEndMs != null) {
                CountdownText(
                    targetEpochMs = item.postLandingGraceEndMs,
                    clock = clock,
                    prefix = "Landed · ",
                    readyText = "Attackable",
                    color = CrowColors.AccentD,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun memberFlightSortGroup(item: FactionMemberItem): Int = when (item.member.status.state) {
    StatusKind.TRAVELING -> 0
    StatusKind.ABROAD -> 1
    StatusKind.OKAY -> 2
    StatusKind.HOSPITAL -> 3
    else -> 4
}
