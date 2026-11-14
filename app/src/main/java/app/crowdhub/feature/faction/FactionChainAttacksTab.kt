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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.components.formatRelativeAge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.TornFactionAttack
import app.crowdhub.provider.torn.TornFactionChain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactionChainAttacksTab(
    state: FactionUiState,
    clock: Clock,
    onFilterChange: (FactionAttackFilter) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAttackPlayer: (Long) -> Unit,
    onRetry: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val chainSection = state.chain
    val attacksSection = state.attacks
    val ownFactionId = state.ownFactionId
    var selectedAttack by remember { mutableStateOf<TornFactionAttack?>(null) }

    val attacks = attacksSection.data.orEmpty()
    val filteredAttacks = remember(attacks, state.attackFilter, ownFactionId) {
        attacks.filter { atk ->
            val isOutgoing = ownFactionId != null && atk.attackerFactionId == ownFactionId
            val isIncoming = ownFactionId != null && atk.defenderFactionId == ownFactionId && atk.attackerFactionId != ownFactionId
            when (state.attackFilter) {
                FactionAttackFilter.ALL -> true
                FactionAttackFilter.OUTGOING -> isOutgoing
                FactionAttackFilter.INCOMING -> isIncoming
                FactionAttackFilter.CHAIN -> atk.chainHit > 0
                FactionAttackFilter.RESPECT -> atk.respectGain > 0.0
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "chain_card") {
            ChainSummaryCard(
                chain = chainSection.data,
                bestChain = state.basic.data?.bestChain,
                fetchedAtMs = chainSection.fetchedAtMs,
                clock = clock
            )
        }

        if (attacksSection.isStale && attacksSection.error != null) {
            item(key = "attacks_stale_banner") {
                OfflineBanner("Cached attack log (${attacksSection.error.message})")
            }
        }

        item(key = "attacks_header_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attack Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrowColors.Text
                    )
                    if (attacksSection.fetchedAtMs > 0L) {
                        StaleBadge(fetchedAtMs = attacksSection.fetchedAtMs, clock = clock)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FactionAttackFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.attackFilter == f,
                            onClick = { onFilterChange(f) },
                            label = { Text(f.label) },
                            modifier = Modifier.testTag("faction_attack_filter_${f.name.lowercase()}")
                        )
                    }
                }
            }
        }

        when {
            attacksSection.isLoading && attacksSection.data == null -> {
                item(key = "attacks_loading") {
                    LoadingView("Loading faction attack log...")
                }
            }
            attacksSection.accessRestricted && attacksSection.data == null -> {
                item(key = "attacks_restricted") {
                    FactionPermissionNoticeCard(
                        title = "Faction Attack Log Requires Faction API Access",
                        message = "Your Torn API key does not currently have Faction API Access enabled for attack logs. Ask faction leadership or update your key in Account & API Manager.",
                        accountId = state.accountId,
                        onOpenAccount = onOpenAccount
                    )
                }
            }
            attacksSection.data == null && attacksSection.error != null -> {
                item(key = "attacks_error") {
                    ErrorView(
                        error = attacksSection.error,
                        onRetry = onRetry,
                        onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
                    )
                }
            }
            filteredAttacks.isEmpty() -> {
                item(key = "attacks_empty") {
                    EmptyView(
                        title = "No attacks found",
                        subtitle = if (attacks.isEmpty()) "No recent faction attacks recorded." else "Try changing your attack log filter."
                    )
                }
            }
            else -> {
                items(filteredAttacks, key = { it.attackId }) { atk ->
                    AttackLogRowCard(
                        attack = atk,
                        ownFactionId = ownFactionId,
                        clock = clock,
                        dimmed = attacksSection.isStale,
                        onClick = { selectedAttack = atk }
                    )
                }
            }
        }
    }

    selectedAttack?.let { atk ->
        ModalBottomSheet(
            onDismissRequest = { selectedAttack = null },
            containerColor = CrowColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${atk.attackerName} → ${atk.defenderName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                Text(
                    text = "Outcome: ${atk.result} · Respect ${"%.2f".format(atk.respectGain)}${if (atk.chainHit > 0) " · Chain #${atk.chainHit}" else ""}",
                    color = CrowColors.TextMuted
                )
                atk.modifierFairFight?.let { ff ->
                    Text(
                        text = "Fair Fight multiplier: ${"%.2f".format(ff)}x",
                        color = CrowColors.SheenSoft
                    )
                }

                Spacer(Modifier.height(4.dp))
                val isOwnDefender = ownFactionId != null && atk.defenderFactionId == ownFactionId && atk.attackerFactionId != ownFactionId
                val actionPlayerId = if (isOwnDefender) atk.attackerId else atk.defenderId
                val actionPlayerName = if (isOwnDefender) atk.attackerName else atk.defenderName
                if (actionPlayerId != null && actionPlayerId > 0L) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onOpenProfile(actionPlayerId)
                                selectedAttack = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Profile ($actionPlayerName)") }
                    }
                    Button(
                        onClick = {
                            onAttackPlayer(actionPlayerId)
                            selectedAttack = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrowColors.Danger,
                            contentColor = CrowColors.CrowBlack
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Attack $actionPlayerName") }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ChainSummaryCard(
    chain: TornFactionChain?,
    bestChain: Long?,
    fetchedAtMs: Long,
    clock: Clock
) {
    val active = chain?.isActive == true
    val cooldown = chain?.isInCooldown == true
    val borderColor = when {
        active -> CrowColors.Ok.copy(alpha = 0.7f)
        cooldown -> CrowColors.Jail.copy(alpha = 0.7f)
        else -> CrowColors.Outline
    }

    CrowCard(
        borderColor = borderColor,
        modifier = Modifier.testTag("faction_chain_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when {
                        active -> "ACTIVE CHAIN"
                        cooldown -> "CHAIN COOLDOWN"
                        else -> "CHAIN STATUS"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        active -> CrowColors.Ok
                        cooldown -> CrowColors.Jail
                        else -> CrowColors.TextMuted
                    }
                )
                if (chain != null) {
                    Text(
                        text = "${chain.current} / ${if (chain.max > 0) chain.max else "—"}",
                        style = MaterialTheme.typography.headlineSmall.merge(MonospaceNumberStyle),
                        fontWeight = FontWeight.Bold,
                        color = CrowColors.Text
                    )
                } else {
                    Text(
                        text = "No chain data",
                        style = MaterialTheme.typography.titleMedium,
                        color = CrowColors.TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (chain != null && (active || chain.current > 0)) {
                    Surface(
                        color = CrowColors.Sheen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${"%.2f".format(chain.modifier)}x",
                            style = MaterialTheme.typography.labelLarge.merge(MonospaceNumberStyle),
                            fontWeight = FontWeight.Bold,
                            color = CrowColors.SheenSoft,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                bestChain?.let { best ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Best: $best",
                        style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                        color = CrowColors.TextMuted
                    )
                }
            }
        }

        if (chain != null && chain.max > 0) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (chain.current.toFloat() / chain.max.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (active) CrowColors.Ok else CrowColors.AccentD,
                trackColor = CrowColors.SurfaceHigh
            )
        }

        if (chain != null && (active || cooldown)) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val baseMs = if (fetchedAtMs > 0L) fetchedAtMs else clock.nowEpochMs()
                if (active && chain.timeoutSec > 0) {
                    val timeoutTargetMs = baseMs + chain.timeoutSec * 1_000L
                    val urgent = (timeoutTargetMs - clock.nowEpochMs()) < 60_000L
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timeout:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrowColors.TextMuted
                        )
                        CountdownText(
                            targetEpochMs = timeoutTargetMs,
                            clock = clock,
                            readyText = "Broken",
                            color = if (urgent) CrowColors.Danger else CrowColors.Ok,
                            readyColor = CrowColors.Danger,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                if (cooldown && chain.cooldownSec > 0L) {
                    val cooldownTargetMs = baseMs + chain.cooldownSec * 1_000L
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cooldown:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrowColors.TextMuted
                        )
                        CountdownText(
                            targetEpochMs = cooldownTargetMs,
                            clock = clock,
                            readyText = "Ready",
                            color = CrowColors.Jail,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttackLogRowCard(
    attack: TornFactionAttack,
    ownFactionId: Long?,
    clock: Clock,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val isOutgoing = ownFactionId != null && attack.attackerFactionId == ownFactionId
    val isIncoming = ownFactionId != null && attack.defenderFactionId == ownFactionId && !isOutgoing
    val resultColor = when {
        attack.isInterrupted -> CrowColors.Jail
        isIncoming && !attack.result.contains("lost", ignoreCase = true) -> CrowColors.Danger
        attack.respectGain > 0.0 -> CrowColors.Ok
        else -> CrowColors.TextMuted
    }

    CrowCard(
        dimmed = dimmed,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("faction_attack_row_${attack.attackId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = (if (isIncoming) CrowColors.Danger else CrowColors.Ok).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isIncoming) "IN" else "OUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncoming) CrowColors.Danger else CrowColors.Ok,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = resultColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (attack.isInterrupted) "Interrupted" else attack.result,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = resultColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (attack.chainHit > 0) {
                    Text(
                        text = "#${attack.chainHit}",
                        style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                        color = CrowColors.SheenSoft,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val respectSign = if (attack.respectGain > 0.0) "+" else ""
            Text(
                text = "$respectSign${"%.2f".format(attack.respectGain)} resp",
                style = MaterialTheme.typography.labelMedium.merge(MonospaceNumberStyle),
                fontWeight = FontWeight.SemiBold,
                color = if (attack.respectGain > 0.0) CrowColors.Ok else CrowColors.TextMuted
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "${attack.attackerName} → ${attack.defenderName}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = CrowColors.Text
        )

        val ageText = (attack.endedAtSec ?: attack.startedAtSec)?.let { sec ->
            formatRelativeAge(clock.nowEpochMs() - sec * 1_000L)
        }
        val factionLine = listOfNotNull(
            attack.attackerFactionName?.let { "$it → ${attack.defenderFactionName ?: "No Faction"}" },
            attack.modifierFairFight?.let { "FF ${"%.2f".format(it)}x" },
            ageText
        ).joinToString(" · ")
        if (factionLine.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = factionLine,
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.TextMuted
            )
        }
    }
}

@Composable
fun FactionPermissionNoticeCard(
    title: String,
    message: String,
    accountId: String?,
    onOpenAccount: (String) -> Unit
) {
    CrowCard(borderColor = CrowColors.Jail.copy(alpha = 0.6f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CrowColors.Jail
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = CrowColors.TextMuted
        )
        if (accountId != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onOpenAccount(accountId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Account & API Manager")
            }
        }
    }
}
