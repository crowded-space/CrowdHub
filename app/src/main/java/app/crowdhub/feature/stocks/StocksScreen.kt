package app.crowdhub.feature.stocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
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
import androidx.compose.ui.draw.clip
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
import java.util.Locale

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
    val portfolioState by viewModel.portfolioState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val ownedOnly by viewModel.ownedOnlyFilter.collectAsStateWithLifecycle()
    val portfolioSort by viewModel.portfolioSort.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    LaunchedEffect(activeProfile.primaryAccount?.id) {
        while (true) {
            delay(30_000L)
            viewModel.refresh(forceRefresh = false)
        }
    }

    val portfolioSummary = (portfolioState as? PortfolioUiState.Content)?.summary

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

            StocksModeSelector(
                selectedTab = selectedTab,
                ownedCount = portfolioSummary?.ownedCount ?: 0,
                readyDividendCount = portfolioSummary?.readyDividendCount ?: 0,
                onSelectTab = { viewModel.setSelectedTab(it) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            if (selectedTab == StockTab.PORTFOLIO) {
                                "Search owned stocks"
                            } else {
                                "Search name or acronym"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stocks_search_input")
                )

                if (selectedTab == StockTab.MARKET) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = ownedOnly,
                            onClick = { viewModel.setOwnedOnlyFilter(!ownedOnly) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                val countSuffix = portfolioSummary?.ownedCount?.let { " ($it)" }.orEmpty()
                                Text("Owned$countSuffix")
                            },
                            modifier = Modifier.testTag("stocks_filter_owned")
                        )
                        StockSort.entries.forEach { mode ->
                            FilterChip(
                                selected = sortMode == mode,
                                onClick = { viewModel.setSortMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PortfolioSort.entries.forEach { mode ->
                            FilterChip(
                                selected = portfolioSort == mode,
                                onClick = { viewModel.setPortfolioSort(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                StockTab.MARKET -> {
                    MarketStocksBody(
                        uiState = uiState,
                        portfolioSummary = portfolioSummary,
                        clock = clock,
                        onOpenPortfolio = { viewModel.setSelectedTab(StockTab.PORTFOLIO) },
                        onOpenDetail = onOpenDetail,
                        onOpenAccount = onOpenAccount,
                        onRetry = { viewModel.refresh(forceRefresh = true) }
                    )
                }
                StockTab.PORTFOLIO -> {
                    PortfolioBody(
                        portfolioState = portfolioState,
                        clock = clock,
                        onOpenDetail = onOpenDetail,
                        onOpenAccount = onOpenAccount,
                        onBrowseMarket = { viewModel.setSelectedTab(StockTab.MARKET) },
                        onRetry = { viewModel.refresh(forceRefresh = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StocksModeSelector(
    selectedTab: StockTab,
    ownedCount: Int,
    readyDividendCount: Int,
    onSelectTab: (StockTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CrowColors.Surface)
            .border(1.dp, CrowColors.Outline, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StockTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val label = when (tab) {
                StockTab.MARKET -> "Market"
                StockTab.PORTFOLIO -> if (ownedCount > 0) "Portfolio ($ownedCount)" else "Portfolio"
            }
            val tag = if (tab == StockTab.MARKET) "stocks_tab_market" else "stocks_tab_portfolio"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) CrowColors.Sheen.copy(alpha = 0.22f) else CrowColors.Surface)
                    .clickable { onSelectTab(tab) }
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) CrowColors.SheenSoft else CrowColors.TextMuted
                )
                if (tab == StockTab.PORTFOLIO && readyDividendCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CrowColors.AccentD)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketStocksBody(
    uiState: StocksUiState,
    portfolioSummary: PortfolioSummary?,
    clock: Clock,
    onOpenPortfolio: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onOpenAccount: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (uiState) {
        StocksUiState.Loading -> LoadingView("Loading Torn stock market...")
        is StocksUiState.Empty -> EmptyView(title = uiState.message)
        is StocksUiState.Error -> ErrorView(
            error = uiState.error,
            onRetry = onRetry,
            onFixKey = uiState.accountId?.let { id -> { onOpenAccount(id) } }
        )
        is StocksUiState.Content -> {
            val dimmed = uiState.isStale || (clock.nowEpochMs() - uiState.fetchedAtMs > 60_000L)
            if (uiState.isStale) {
                OfflineBanner("Offline/throttled (${uiState.staleReason ?: "stale"}) — cached prices")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (portfolioSummary != null && portfolioSummary.ownedCount > 0) {
                    item(key = "market_portfolio_strip") {
                        PortfolioCompactStrip(
                            summary = portfolioSummary,
                            dimmed = dimmed,
                            onClick = onOpenPortfolio
                        )
                    }
                }
                item(key = "stocks_age") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        StaleBadge(fetchedAtMs = uiState.fetchedAtMs, clock = clock)
                    }
                }
                items(uiState.items, key = { it.stock.stockId }) { item ->
                    val pct = item.changePct24h
                    val pctColor = when {
                        pct == null -> CrowColors.TextMuted
                        pct >= 0.0 -> CrowColors.Ok
                        else -> CrowColors.Danger
                    }
                    val holding = item.holding
                    CrowCard(
                        dimmed = dimmed,
                        borderColor = if (holding != null) CrowColors.Sheen.copy(alpha = 0.35f) else CrowColors.Outline,
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
                                    if (holding != null) {
                                        MiniBadge(
                                            text = StockFormatters.formatSharesCompact(holding.totalShares),
                                            color = CrowColors.SheenSoft
                                        )
                                        if (holding.dividend?.ready == true) {
                                            MiniBadge(text = "READY", color = CrowColors.AccentD)
                                        }
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
                                    text = "$${"%.2f".format(Locale.US, item.stock.currentPrice)}",
                                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrowColors.Text
                                )
                                Text(
                                    text = pct?.let {
                                        val sign = if (it >= 0.0) "+" else ""
                                        "$sign${"%.2f".format(Locale.US, it)}%"
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

@Composable
private fun PortfolioBody(
    portfolioState: PortfolioUiState,
    clock: Clock,
    onOpenDetail: (Int) -> Unit,
    onOpenAccount: (String) -> Unit,
    onBrowseMarket: () -> Unit,
    onRetry: () -> Unit
) {
    when (portfolioState) {
        PortfolioUiState.Loading -> LoadingView("Loading your stock portfolio...")
        is PortfolioUiState.Empty -> EmptyView(
            title = portfolioState.message,
            subtitle = "Buy shares in Torn to track your portfolio value, P&L, and benefit timers.",
            actionLabel = "Browse Market",
            onAction = onBrowseMarket
        )
        is PortfolioUiState.Error -> ErrorView(
            error = portfolioState.error,
            onRetry = onRetry,
            onFixKey = portfolioState.accountId?.let { id -> { onOpenAccount(id) } }
        )
        is PortfolioUiState.Content -> {
            val dimmed = portfolioState.isStale || (clock.nowEpochMs() - portfolioState.fetchedAtMs > 60_000L)
            if (portfolioState.isStale) {
                OfflineBanner("Offline/throttled (${portfolioState.staleReason ?: "stale"}) — cached holdings")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "portfolio_age") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        StaleBadge(fetchedAtMs = portfolioState.fetchedAtMs, clock = clock)
                    }
                }
                item(key = "portfolio_summary") {
                    PortfolioSummaryCard(
                        summary = portfolioState.summary,
                        dimmed = dimmed
                    )
                }
                if (portfolioState.visiblePositions.isEmpty()) {
                    item(key = "portfolio_no_match") {
                        CrowCard {
                            Text(
                                text = "No owned stocks match your search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrowColors.TextMuted
                            )
                        }
                    }
                } else {
                    items(portfolioState.visiblePositions, key = { it.stock.stockId }) { pos ->
                        OwnedStockCard(
                            position = pos,
                            dimmed = dimmed,
                            onClick = { onOpenDetail(pos.stock.stockId) }
                        )
                    }
                }
            }
        }
    }
}
