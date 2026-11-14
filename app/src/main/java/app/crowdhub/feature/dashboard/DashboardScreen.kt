package app.crowdhub.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.alerts.WatchModeService
import app.crowdhub.core.network.Clock
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowdWordmark
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.KeyProblemBanner
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    settingsStore: SettingsStore,
    clock: Clock,
    onOpenAccount: (String) -> Unit,
    onOpenBrowserHome: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    var menuExpanded by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshIfOlderThan(30_000L)
    }

    // Auto-refresh every 30 s ONLY while DashboardScreen is visible
    LaunchedEffect(activeProfile.primaryAccount?.id, activeProfile.accountStatus) {
        while (true) {
            delay(30_000L)
            viewModel.refresh(forceRefresh = false)
        }
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { CrowdWordmark() },
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
                                if (enable) {
                                    WatchModeService.start(context)
                                } else {
                                    WatchModeService.stop(context)
                                }
                            },
                            modifier = Modifier.testTag("dashboard_watch_switch")
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refresh(forceRefresh = true) },
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh dashboard")
                    }
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("overflow_menu_button")
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Scripts") },
                            onClick = {
                                menuExpanded = false
                                onOpenScripts()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                menuExpanded = false
                                onOpenAbout()
                            }
                        )
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

            when (val state = uiState) {
                DashboardUiState.Loading -> LoadingView("Loading dashboard...")
                is DashboardUiState.Error -> {
                    ErrorView(
                        error = state.error,
                        onRetry = { viewModel.refresh(forceRefresh = true) },
                        onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
                    )
                }
                is DashboardUiState.Content -> {
                    val data = state.data
                    val dimmed = data.isStale || (clock.nowEpochMs() - data.fetchedAtMs > 60_000L)
                    if (data.isStale) {
                        OfflineBanner(
                            message = "Offline/throttled (${data.staleReason ?: "stale"}) — showing cached data"
                        )
                    }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh(forceRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "stale_badge") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    StaleBadge(fetchedAtMs = data.fetchedAtMs, clock = clock)
                                }
                            }
                            data.basic?.let { basic ->
                                item(key = "status") {
                                    StatusCard(basic = basic, clock = clock, dimmed = dimmed)
                                }
                            }
                            data.bars?.let { bars ->
                                item(key = "bars") {
                                    BarsCard(
                                        bars = bars,
                                        fetchedAtMs = data.fetchedAtMs,
                                        clock = clock,
                                        dimmed = dimmed
                                    )
                                }
                            }
                            data.cooldowns?.let { cd ->
                                item(key = "cooldowns") {
                                    CooldownsCard(
                                        cooldowns = cd,
                                        fetchedAtMs = data.fetchedAtMs,
                                        clock = clock,
                                        dimmed = dimmed
                                    )
                                }
                            }
                            data.travel?.let { travel ->
                                item(key = "travel") {
                                    TravelCard(
                                        travel = travel,
                                        statusState = data.basic?.status?.state ?: StatusKind.UNKNOWN,
                                        clock = clock,
                                        dimmed = dimmed,
                                        onLandingReached = { viewModel.refresh(forceRefresh = true) }
                                    )
                                }
                            }
                            data.money?.let { money ->
                                item(key = "money") {
                                    MoneyCard(money = money, dimmed = dimmed)
                                }
                            }
                            data.notifications?.let { notif ->
                                item(key = "events") {
                                    EventsCard(
                                        notifications = notif,
                                        dimmed = dimmed,
                                        onOpenTorn = onOpenBrowserHome
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
