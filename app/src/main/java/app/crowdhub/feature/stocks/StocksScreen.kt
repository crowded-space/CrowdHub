package app.crowdhub.feature.stocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.alerts.WatchModeService
import app.crowdhub.core.network.Clock
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.KeyProblemBanner
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(
    viewModel: StocksViewModel,
    settingsStore: SettingsStore,
    clock: Clock,
    onOpenDetail: (Int) -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    LaunchedEffect(activeProfile.primaryAccount?.id) {
        while (true) {
            delay(30_000L)
            viewModel.refresh(forceRefresh = false)
        }
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Stocks") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Watch",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prefs.watchModeActive) CrowColors.Ok else CrowColors.TextMuted
                        )
                        Switch(
                            checked = prefs.watchModeActive,
                            onCheckedChange = { enable ->
                                if (enable) WatchModeService.start(context) else WatchModeService.stop(context)
                            }
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refresh(forceRefresh = true) },
                        modifier = Modifier.testTag("stocks_refresh_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh stocks")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val acct = activeProfile.primaryAccount
            if (acct != null &&
                (activeProfile.accountStatus == AccountStatus.INVALID ||
                    activeProfile.accountStatus == AccountStatus.PAUSED)
            ) {
                KeyProblemBanner(onClick = { onOpenAccount(acct.id) })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrowColors.SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Background checks run about every 15 minutes. Turn on Watch Mode for 30-second checks.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrowColors.TextMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search name or acronym") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stocks_search_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockSort.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { viewModel.setSortMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            when (val state = uiState) {
                StocksUiState.Loading -> LoadingView("Loading Torn stock market...")
                is StocksUiState.Empty -> EmptyView(title = state.message)
                is StocksUiState.Error -> ErrorView(
                    error = state.error,
                    onRetry = { viewModel.refresh(forceRefresh = true) },
                    onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
                )
                is StocksUiState.Content -> {
                    val dimmed = state.isStale || (clock.nowEpochMs() - state.fetchedAtMs > 60_000L)
                    if (state.isStale) {
                        OfflineBanner("Offline/throttled (${state.staleReason ?: "stale"}) — cached prices")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "stocks_age") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                StaleBadge(fetchedAtMs = state.fetchedAtMs, clock = clock)
                            }
                        }
                        items(state.items, key = { it.stock.stockId }) { item ->
                            val pct = item.changePct24h
                            val pctColor = when {
                                pct == null -> CrowColors.TextMuted
                                pct >= 0.0 -> CrowColors.Ok
                                else -> CrowColors.Danger
                            }
                            CrowCard(
                                dimmed = dimmed,
                                modifier = Modifier
                                    .clickable { onOpenDetail(item.stock.stockId) }
                                    .testTag("stock_row_${item.stock.stockId}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.stock.acronym,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CrowColors.SheenSoft
                                            )
                                            if (item.hasWatch) {
                                                Icon(
                                                    Icons.Outlined.NotificationsActive,
                                                    contentDescription = "Alert active",
                                                    tint = CrowColors.AccentD,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.stock.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CrowColors.TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                    SparklineChart(
                                        points = item.sparkline,
                                        lineColor = if ((pct ?: 0.0) >= 0.0) CrowColors.Ok else CrowColors.Danger,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(28.dp)
                                            .padding(horizontal = 6.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${"%.2f".format(item.stock.currentPrice)}",
                                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                            fontWeight = FontWeight.SemiBold,
                                            color = CrowColors.Text
                                        )
                                        Text(
                                            text = pct?.let {
                                                val sign = if (it >= 0.0) "+" else ""
                                                "$sign${"%.2f".format(it)}%"
                                            } ?: "—",
                                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                            color = pctColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
