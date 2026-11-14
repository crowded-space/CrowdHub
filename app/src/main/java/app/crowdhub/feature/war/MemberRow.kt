package app.crowdhub.feature.war

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.connector.ffscouter.BsText
import app.crowdhub.connector.ffscouter.FfBadge
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.flight.FlightChip

@Composable
fun MemberRow(
    row: WarMemberRowModel,
    ffEnabled: Boolean,
    ffLowThreshold: Double,
    ffHighThreshold: Double,
    clock: Clock,
    dimmed: Boolean,
    onToggleStar: () -> Unit,
    onAttack: () -> Unit,
    onRowClick: () -> Unit
) {
    val m = row.member
    val isStarred = row.stakeout != null

    CrowCard(
        dimmed = dimmed,
        modifier = Modifier
            .clickable(onClick = onRowClick)
            .testTag("war_member_row_${m.playerId}")
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
                val lastAction = buildString {
                    append(m.lastActionStatus ?: "Unknown")
                    m.lastActionRelative?.let { append(" · $it") }
                }
                Text(
                    text = lastAction,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        m.lastActionStatus.equals("Online", ignoreCase = true) -> CrowColors.Ok
                        m.lastActionStatus.equals("Idle", ignoreCase = true) -> CrowColors.AccentD
                        else -> CrowColors.TextMuted
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isStarred) "Unstar stakeout" else "Star stakeout",
                        tint = if (isStarred) CrowColors.AccentD else CrowColors.TextMuted
                    )
                }
                Button(
                    onClick = onAttack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrowColors.Danger,
                        contentColor = CrowColors.CrowBlack
                    ),
                    modifier = Modifier.testTag("attack_button_${m.playerId}")
                ) {
                    Text("Attack")
                }
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
                } else {
                    Text(
                        text = "${m.status.state.label} (time unknown)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted
                    )
                }
            }

            if (m.status.state == StatusKind.TRAVELING && row.flightEstimate != null) {
                FlightChip(estimate = row.flightEstimate, clock = clock)
            }

            if (row.postLandingGraceEndMs != null) {
                CountdownText(
                    targetEpochMs = row.postLandingGraceEndMs,
                    clock = clock,
                    prefix = "Attackable in ",
                    readyText = "Attackable",
                    color = CrowColors.AccentD,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (ffEnabled) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FfBadge(
                    fairFight = row.ffStats?.fairFight,
                    lowThreshold = ffLowThreshold,
                    highThreshold = ffHighThreshold
                )
                BsText(
                    bsEstimate = row.ffStats?.bsEstimate,
                    bsHuman = row.ffStats?.bsHuman
                )
            }
        }
    }
}
