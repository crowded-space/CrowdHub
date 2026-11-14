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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.Clock
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.ArmoryCategory
import app.crowdhub.provider.torn.TornArmoryItem
import app.crowdhub.provider.torn.TornFactionBalance
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FactionArmoryTab(
    state: FactionUiState,
    clock: Clock,
    onCategoryChange: (ArmoryCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val armorySection = state.armory
    val balanceSection = state.balance
    val items = armorySection.data.orEmpty()
    val query = state.armorySearch.trim().lowercase()

    val filtered = remember(items, state.armoryCategory, query) {
        items.filter { item ->
            val catOk = state.armoryCategory == ArmoryCategory.ALL || item.category == state.armoryCategory
            val qOk = query.isEmpty() ||
                item.name.lowercase().contains(query) ||
                item.category.label.lowercase().contains(query) ||
                (item.rawType?.lowercase()?.contains(query) == true)
            catOk && qOk
        }
    }

    val totalUnits = remember(filtered) { filtered.sumOf { it.quantity.toLong() } }
    val totalValue = remember(filtered) {
        filtered.sumOf { (it.marketPrice ?: 0L) * it.quantity.toLong() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        balanceSection.data?.let { bal ->
            item(key = "vault_balance_card") {
                VaultBalanceCard(balance = bal)
            }
        }

        if (armorySection.isStale && armorySection.error != null) {
            item(key = "armory_stale") {
                OfflineBanner("Cached armory (${armorySection.error.message})")
            }
        }

        when {
            armorySection.isLoading && armorySection.data == null -> {
                item(key = "armory_loading") {
                    LoadingView("Loading faction armory...")
                }
            }
            armorySection.accessRestricted && armorySection.data == null -> {
                item(key = "armory_restricted") {
                    FactionPermissionNoticeCard(
                        title = "Faction Armory Requires Faction API Access",
                        message = "Your Torn API key does not currently have Faction API Access enabled for armory inventory. Ask faction leadership or update your key in Account & API Manager.",
                        accountId = state.accountId,
                        onOpenAccount = onOpenAccount
                    )
                }
            }
            armorySection.data == null && armorySection.error != null -> {
                item(key = "armory_error") {
                    ErrorView(
                        error = armorySection.error,
                        onRetry = onRetry,
                        onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
                    )
                }
            }
            else -> {
                item(key = "armory_controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.armorySearch,
                            onValueChange = onSearchChange,
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            placeholder = { Text("Search armory items...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("faction_armory_search")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ArmoryCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = state.armoryCategory == cat,
                                    onClick = { onCategoryChange(cat) },
                                    label = { Text(cat.label) },
                                    modifier = Modifier.testTag("faction_armory_cat_${cat.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val valueSuffix = if (totalValue > 0L) " · Est. $${formatCompactMoney(totalValue)}" else ""
                            Text(
                                text = "${filtered.size} types · ${formatInt(totalUnits)} units$valueSuffix",
                                style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                                color = CrowColors.TextMuted
                            )
                            if (armorySection.fetchedAtMs > 0L) {
                                StaleBadge(fetchedAtMs = armorySection.fetchedAtMs, clock = clock)
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item(key = "armory_empty") {
                        EmptyView(
                            title = "No armory items match",
                            subtitle = if (items.isEmpty()) "The faction armory is empty." else "Try another category or search term."
                        )
                    }
                } else {
                    items(filtered, key = { "${it.category.name}_${it.itemId}_${it.name}" }) { item ->
                        ArmoryItemCard(item = item, dimmed = armorySection.isStale)
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultBalanceCard(balance: TornFactionBalance) {
    CrowCard(
        borderColor = CrowColors.Sheen.copy(alpha = 0.5f),
        modifier = Modifier.testTag("faction_vault_balance_card")
    ) {
        Text(
            text = "Faction Vault",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CrowColors.Text
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Faction Cash", style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                Text(
                    text = balance.factionMoney?.let { "$${formatInt(it)}" } ?: "—",
                    style = MaterialTheme.typography.titleMedium.merge(MonospaceNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Ok
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Faction Points", style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                Text(
                    text = balance.factionPoints?.let { formatInt(it) } ?: "—",
                    style = MaterialTheme.typography.titleMedium.merge(MonospaceNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.SheenSoft
                )
            }
        }
        if (balance.memberMoney != null || balance.memberPoints != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your vault balance: $${formatInt(balance.memberMoney ?: 0L)} · ${formatInt(balance.memberPoints ?: 0L)} pts",
                style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                color = CrowColors.TextMuted
            )
        }
    }
}

@Composable
private fun ArmoryItemCard(
    item: TornArmoryItem,
    dimmed: Boolean
) {
    CrowCard(dimmed = dimmed) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CrowColors.SurfaceHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.rawType?.takeIf { it.isNotBlank() } ?: item.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.SheenSoft,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.loaned > 0) {
                        Text(
                            text = "${item.loaned} loaned",
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = CrowColors.AccentD
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (item.loaned > 0) {
                        "${formatInt(item.available.toLong())} / ${formatInt(item.quantity.toLong())}"
                    } else {
                        "x${formatInt(item.quantity.toLong())}"
                    },
                    style = MaterialTheme.typography.titleSmall.merge(MonospaceNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = if (item.available > 0) CrowColors.Ok else CrowColors.TextMuted
                )
                item.marketPrice?.takeIf { it > 0L }?.let { unitPrice ->
                    Text(
                        text = "$${formatCompactMoney(unitPrice)} ea",
                        style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                        color = CrowColors.TextMuted
                    )
                }
            }
        }
    }
}

private fun formatInt(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)

private fun formatCompactMoney(value: Long): String = when {
    value >= 1_000_000_000L -> "%.2fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.2fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}
