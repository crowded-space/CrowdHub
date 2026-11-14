package app.crowdhub.feature.faction

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.TornOcSlot
import app.crowdhub.provider.torn.TornOrganizedCrime

@Composable
fun FactionOcTab(
    state: FactionUiState,
    clock: Clock,
    onFilterChange: (FactionOcFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val ocSection = state.oc
    val ocData = ocSection.data
    val ownPlayerId = state.ownPlayerId

    when {
        ocSection.isLoading && ocData == null -> {
            LoadingView("Loading Organized Crimes...")
        }
        ocData == null && ocSection.error != null -> {
            ErrorView(
                error = ocSection.error,
                onRetry = onRetry,
                onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
            )
        }
        else -> {
            val allCrimes = ocData?.factionCrimes.orEmpty()
            val selfCrime = ocData?.selfCrime
            val filteredCrimes = remember(allCrimes, state.ocFilter) {
                allCrimes.filter { c ->
                    val statusLow = c.status.lowercase()
                    when (state.ocFilter) {
                        FactionOcFilter.ALL -> true
                        FactionOcFilter.ACTIVE -> statusLow in setOf("ready", "planning", "in progress", "recruiting")
                        FactionOcFilter.READY -> statusLow == "ready"
                        FactionOcFilter.RECRUITING -> statusLow == "recruiting"
                        FactionOcFilter.COMPLETED -> statusLow in setOf("successful", "completed", "failure", "failed", "expired")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ocSection.isStale && ocSection.error != null) {
                    item(key = "oc_stale") {
                        OfflineBanner("Cached OC data (${ocSection.error.message})")
                    }
                }

                item(key = "self_oc_hero") {
                    SelfOcSpotlightCard(
                        crime = selfCrime,
                        ownPlayerId = ownPlayerId,
                        clock = clock,
                        fetchedAtMs = ocSection.fetchedAtMs
                    )
                }

                if (ocData?.factionCrimesRestricted == true) {
                    item(key = "oc_restricted_banner") {
                        FactionPermissionNoticeCard(
                            title = "All Faction OCs Require Faction API Access",
                            message = "Your current OC is shown above via personal access. Enable Faction API Access in Torn to view all faction crimes and teammates' CPR.",
                            accountId = state.accountId,
                            onOpenAccount = onOpenAccount
                        )
                    }
                } else {
                    item(key = "faction_oc_filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Faction Organized Crimes (${filteredCrimes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrowColors.Text
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FactionOcFilter.entries.forEach { f ->
                                    FilterChip(
                                        selected = state.ocFilter == f,
                                        onClick = { onFilterChange(f) },
                                        label = { Text(f.label) },
                                        modifier = Modifier.testTag("faction_oc_filter_${f.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    if (filteredCrimes.isEmpty()) {
                        item(key = "faction_oc_empty") {
                            EmptyView(
                                title = "No Organized Crimes in filter",
                                subtitle = if (allCrimes.isEmpty()) "No faction organized crimes found." else "Switch filter chips to view other OC stages."
                            )
                        }
                    } else {
                        items(filteredCrimes, key = { it.crimeId }) { crime ->
                            OrganizedCrimeCard(
                                crime = crime,
                                ownPlayerId = ownPlayerId,
                                clock = clock,
                                dimmed = ocSection.isStale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfOcSpotlightCard(
    crime: TornOrganizedCrime?,
    ownPlayerId: Long?,
    clock: Clock,
    fetchedAtMs: Long
) {
    if (crime == null) {
        CrowCard(
            borderColor = CrowColors.Outline,
            modifier = Modifier.testTag("self_oc_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Current OC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrowColors.SheenSoft
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Not currently assigned to an Organized Crime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrowColors.TextMuted
                    )
                }
                if (fetchedAtMs > 0L) {
                    StaleBadge(fetchedAtMs = fetchedAtMs, clock = clock)
                }
            }
        }
        return
    }

    val selfSlot = ownPlayerId?.let { pid -> crime.slots.firstOrNull { it.userId == pid } }
    val statusColor = ocStatusColor(crime.status)

    CrowCard(
        borderColor = statusColor.copy(alpha = 0.7f),
        modifier = Modifier.testTag("self_oc_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR CURRENT OC",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CrowColors.SheenSoft
            )
            Surface(
                color = statusColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = crime.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = crime.name + (crime.difficulty?.let { " · Lv $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrowColors.Text,
                modifier = Modifier.weight(1f)
            )
            val countdownSec = crime.readyAtSec ?: crime.expiredAtSec
            if (countdownSec != null && crime.executedAtSec == null) {
                CountdownText(
                    targetEpochMs = countdownSec * 1_000L,
                    clock = clock,
                    readyText = "Ready now",
                    color = statusColor,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (selfSlot != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = CrowColors.SurfaceHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Role: ${selfSlot.position}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CrowColors.Text
                        )
                        val cpr = selfSlot.checkpointPassRate
                        Text(
                            text = cpr?.let { "CPR ${formatPct(it)}" } ?: "CPR —",
                            style = MaterialTheme.typography.labelMedium.merge(MonospaceNumberStyle),
                            fontWeight = FontWeight.Bold,
                            color = cprColor(cpr)
                        )
                    }

                    selfSlot.checkpointPassRate?.let { cpr ->
                        LinearProgressIndicator(
                            progress = { (cpr / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp),
                            color = cprColor(cpr),
                            trackColor = CrowColors.CrowBlack
                        )
                    }

                    selfSlot.itemRequirementName?.let { itemName ->
                        val availText = when (selfSlot.itemAvailable) {
                            true -> "Ready"
                            false -> "Missing"
                            null -> "Required"
                        }
                        Text(
                            text = "Item: $itemName ($availText)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selfSlot.itemAvailable == false) CrowColors.Danger else CrowColors.TextMuted
                        )
                    }
                }
            }
        }

        if (crime.slots.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Participants (${crime.slots.count { it.userId != null }}/${crime.slots.size})",
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.TextMuted
            )
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                crime.slots.forEach { slot ->
                    OcSlotRow(slot = slot, isSelf = ownPlayerId != null && slot.userId == ownPlayerId)
                }
            }
        }
    }
}

@Composable
private fun OrganizedCrimeCard(
    crime: TornOrganizedCrime,
    ownPlayerId: Long?,
    clock: Clock,
    dimmed: Boolean
) {
    val statusColor = ocStatusColor(crime.status)
    val filledCount = crime.slots.count { it.userId != null }
    val cprValues = crime.slots.mapNotNull { it.checkpointPassRate }
    val avgCpr = if (cprValues.isNotEmpty()) cprValues.average() else null

    CrowCard(
        dimmed = dimmed,
        modifier = Modifier.testTag("faction_oc_card_${crime.crimeId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = crime.name + (crime.difficulty?.let { " · Lv $it" } ?: ""),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                val summary = buildString {
                    append("$filledCount/${crime.slots.size} slots")
                    avgCpr?.let { append(" · Avg CPR ${formatPct(it)}") }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                    color = CrowColors.TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = crime.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                val countdownSec = crime.readyAtSec ?: crime.expiredAtSec
                if (countdownSec != null && crime.executedAtSec == null) {
                    Spacer(Modifier.height(4.dp))
                    CountdownText(
                        targetEpochMs = countdownSec * 1_000L,
                        clock = clock,
                        readyText = "Ready",
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (crime.slots.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                crime.slots.forEach { slot ->
                    OcSlotRow(slot = slot, isSelf = ownPlayerId != null && slot.userId == ownPlayerId)
                }
            }
        }
    }
}

@Composable
private fun OcSlotRow(slot: TornOcSlot, isSelf: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val playerLabel = when {
                isSelf -> "${slot.userName ?: "You"} (You)"
                slot.userName != null -> slot.userName
                else -> "Open slot"
            }
            Text(
                text = "${slot.position} · $playerLabel",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelf) CrowColors.SheenSoft else if (slot.userId != null) CrowColors.Text else CrowColors.TextMuted
            )
            slot.itemRequirementName?.let { itemName ->
                Text(
                    text = itemName + if (slot.itemAvailable == false) " (missing)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (slot.itemAvailable == false) CrowColors.Danger else CrowColors.TextMuted
                )
            }
        }

        val cpr = slot.checkpointPassRate
        Text(
            text = when {
                cpr != null -> "CPR ${formatPct(cpr)}"
                slot.userId == null -> "Vacant"
                else -> slot.status
            },
            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
            fontWeight = FontWeight.SemiBold,
            color = if (cpr != null) cprColor(cpr) else CrowColors.TextMuted
        )
    }
}

private fun ocStatusColor(status: String): Color = when (status.lowercase()) {
    "ready", "successful", "completed" -> CrowColors.Ok
    "planning", "in progress" -> CrowColors.AccentD
    "recruiting" -> CrowColors.Abroad
    "failure", "failed", "expired" -> CrowColors.Danger
    else -> CrowColors.TextMuted
}

private fun cprColor(cpr: Double?): Color = when {
    cpr == null -> CrowColors.TextMuted
    cpr >= 80.0 -> CrowColors.Ok
    cpr >= 60.0 -> CrowColors.Jail
    else -> CrowColors.Danger
}

private fun formatPct(value: Double): String {
    return if (value % 1.0 == 0.0) "${value.toInt()}%" else "${"%.1f".format(value)}%"
}
