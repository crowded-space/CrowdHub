package app.crowdhub.feature.war

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.TornRankedWar
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val warDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun CurrentWarStatusBanner(
    status: CurrentWarStatus,
    ffEnabled: Boolean,
    fetchedAtMs: Long,
    clock: Clock,
    onChangeStakeout: () -> Unit,
    onClearManualStakeout: () -> Unit
) {
    CrowCard(
        borderColor = when (status) {
            is CurrentWarStatus.Active -> CrowColors.Ok.copy(alpha = 0.7f)
            is CurrentWarStatus.Upcoming -> CrowColors.AccentD.copy(alpha = 0.7f)
            is CurrentWarStatus.ManualStakeout -> CrowColors.Sheen
            is CurrentWarStatus.NotInWar -> CrowColors.Outline
        },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("current_war_status_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when (status) {
                    is CurrentWarStatus.Active -> {
                        val w = status.war
                        Text(
                            text = "${w.ownFactionName} vs ${w.enemyFactionName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.Text
                        )
                        Text(
                            text = buildActiveWarSubtitle(w),
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.Ok
                        )
                    }
                    is CurrentWarStatus.Upcoming -> {
                        val w = status.war
                        Text(
                            text = "${w.ownFactionName} vs ${w.enemyFactionName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.Text
                        )
                        val startStr = w.startEpochSec?.let { formatWarDate(it) } ?: "TBA"
                        Text(
                            text = "Upcoming Ranked War · Starts $startStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.AccentD
                        )
                    }
                    is CurrentWarStatus.ManualStakeout -> {
                        Text(
                            text = status.factionName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.Text
                        )
                        Text(
                            text = "Manual Stakeout · ID ${status.factionId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.SheenSoft
                        )
                    }
                    is CurrentWarStatus.NotInWar -> {
                        Text(
                            text = "Not in an active ranked war",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.Text
                        )
                        Text(
                            text = status.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrowColors.TextMuted
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status is CurrentWarStatus.ManualStakeout) {
                    TextButton(onClick = onClearManualStakeout) {
                        Text("Auto")
                    }
                }
                TextButton(
                    onClick = onChangeStakeout,
                    modifier = Modifier.testTag("change_stakeout_button")
                ) {
                    Text(if (status is CurrentWarStatus.NotInWar) "Stake out" else "Change")
                }
            }
        }

        if (fetchedAtMs > 0L) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StaleBadge(fetchedAtMs = fetchedAtMs, clock = clock)
                if (ffEnabled && status !is CurrentWarStatus.NotInWar) {
                    Text(
                        text = "FFScouter on",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.SheenSoft
                    )
                }
            }
        }
    }
}

@Composable
fun WarHistorySectionHeader(
    factionCount: Int,
    totalWars: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = CrowColors.SheenSoft,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Previous War History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CrowColors.Text
            )
        }
        Text(
            text = "$factionCount factions · $totalWars wars",
            style = MaterialTheme.typography.labelSmall,
            color = CrowColors.TextMuted
        )
    }
}

@Composable
fun HistoryOpponentFactionHeaderCard(
    group: HistoryFactionGroup,
    onToggleExpand: () -> Unit,
    onRefreshMembers: () -> Unit
) {
    val latest = group.latestWar
    val latestWon = latest.winnerFactionId != null && latest.winnerFactionId == latest.ownFactionId
    val recordBadgeColor = when {
        group.wins > group.losses -> CrowColors.Ok
        group.losses > group.wins -> CrowColors.Danger
        else -> CrowColors.AccentD
    }

    CrowCard(
        borderColor = if (group.isExpanded) CrowColors.Sheen else CrowColors.Outline,
        modifier = Modifier
            .clickable(onClick = onToggleExpand)
            .testTag("history_faction_${group.enemyFactionId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = group.enemyFactionName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CrowColors.Text
                    )
                    Surface(
                        color = recordBadgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val badgeText = if (group.wars.size == 1) {
                            if (latestWon) "WON" else "LOST"
                        } else {
                            "${group.wars.size} WARS · ${group.wins}W-${group.losses}L"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = recordBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                val dateStr = (latest.endEpochSec ?: latest.startEpochSec)?.let { formatWarDate(it) } ?: "Ended"
                val scoreSummary = if (latest.ownScore != null && latest.enemyScore != null) {
                    "Latest: ${formatScore(latest.ownScore)} – ${formatScore(latest.enemyScore)} · $dateStr"
                } else {
                    "Latest war: $dateStr"
                }
                Text(
                    text = "$scoreSummary · ID ${group.enemyFactionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.isExpanded) {
                    IconButton(
                        onClick = onRefreshMembers,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh ${group.enemyFactionName} members",
                            tint = CrowColors.TextMuted
                        )
                    }
                }
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (group.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (group.isExpanded) {
                            "Collapse ${group.enemyFactionName}"
                        } else {
                            "Expand ${group.enemyFactionName}"
                        },
                        tint = CrowColors.SheenSoft
                    )
                }
            }
        }

        if (group.isExpanded) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = CrowColors.Outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                group.wars.forEach { war ->
                    val won = war.winnerFactionId != null && war.winnerFactionId == war.ownFactionId
                    val date = (war.endEpochSec ?: war.startEpochSec)?.let { formatWarDate(it) } ?: "—"
                    val scorePart = if (war.ownScore != null && war.enemyScore != null) {
                        "${formatScore(war.ownScore)} – ${formatScore(war.enemyScore)}"
                    } else {
                        "—"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (won) "Won" else "Lost"} · $scorePart",
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = if (won) CrowColors.Ok else CrowColors.Danger
                        )
                        Text(
                            text = "$date${war.targetScore?.let { " · Target ${formatScore(it)}" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.TextMuted
                        )
                    }
                }
            }

            val mState = group.membersState
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countLabel = when {
                    mState.isLoading && mState.totalMemberCount == 0 -> "Loading ${group.enemyFactionName} roster..."
                    mState.totalMemberCount > 0 && mState.rows.size != mState.totalMemberCount ->
                        "${mState.rows.size} of ${mState.totalMemberCount} players shown"
                    mState.totalMemberCount > 0 -> "${mState.totalMemberCount} players in ${group.enemyFactionName}"
                    else -> "${group.enemyFactionName} roster"
                }
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.SheenSoft
                )
                if (mState.isStale) {
                    Text(
                        text = "Cached",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.AccentD
                    )
                }
            }
        }
    }
}

@Composable
fun NestedFactionMemberContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(CrowColors.Sheen.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun NestedFactionStatusBox(
    isLoading: Boolean,
    errorMessage: String?,
    hasFilteredOutAll: Boolean,
    factionName: String,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit
) {
    NestedFactionMemberContainer {
        CrowCard(containerColor = CrowColors.SurfaceHigh) {
            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CrowColors.Sheen,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Fetching $factionName members...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrowColors.TextMuted
                        )
                    }
                }
                errorMessage != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Could not load $factionName: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrowColors.Danger
                        )
                        OutlinedButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
                hasFilteredOutAll -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No members in $factionName match active filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrowColors.TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearFilters) {
                            Text("Clear")
                        }
                    }
                }
                else -> {
                    Text(
                        text = "No members returned for $factionName.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                }
            }
        }
    }
}

private fun buildActiveWarSubtitle(war: TornRankedWar): String {
    val scoreText = if (war.ownScore != null && war.enemyScore != null) {
        val targetPart = war.targetScore?.let { " / ${formatScore(it)}" } ?: ""
        " · ${formatScore(war.ownScore)} – ${formatScore(war.enemyScore)}$targetPart"
    } else ""
    return "Active Ranked War$scoreText"
}

private fun formatScore(score: Int): String =
    NumberFormat.getIntegerInstance(Locale.US).format(score)

private fun formatWarDate(epochSec: Long): String {
    return runCatching {
        warDateFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epochSec))
    }.getOrDefault("—")
}
