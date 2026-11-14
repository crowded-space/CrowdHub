package app.crowdhub.feature.war

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.alerts.WatchModeService
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.ErrorView
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.browser.AttackNavigator
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarScreen(
    viewModel: WarViewModel,
    settingsStore: SettingsStore,
    attackNavigator: AttackNavigator,
    browserNavigator: BrowserNavigator,
    clock: Clock,
    onOpenAccount: (String) -> Unit,
    onNavigateToBrowser: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickFilters by viewModel.quickFilters.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val extraFilters by viewModel.extraFilters.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    var showFiltersSheet by remember { mutableStateOf(false) }
    var showManualFactionDialog by remember { mutableStateOf(false) }
    var manualFactionInput by remember { mutableStateOf(prefs.manualWarFactionId) }
    var detailRow by remember { mutableStateOf<WarMemberRowModel?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.refresh(forceRefresh = false)
        }
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("War") },
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
                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Filters & sort")
                    }
                    IconButton(onClick = { viewModel.refresh(forceRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh war")
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
            when (val state = uiState) {
                WarUiState.Loading -> LoadingView("Loading ranked war...")
                is WarUiState.Error -> ErrorView(
                    error = state.error,
                    onRetry = { viewModel.refresh(forceRefresh = true) },
                    onFixKey = state.accountId?.let { id -> { onOpenAccount(id) } }
                )
                is WarUiState.NoActiveWar -> {
                    EmptyView(
                        title = "No active ranked war",
                        subtitle = state.message,
                        actionLabel = "Stake out another faction",
                        onAction = { showManualFactionDialog = true }
                    )
                }
                is WarUiState.Content -> {
                    val dimmed = state.isStale || (clock.nowEpochMs() - state.fetchedAtMs > 60_000L)
                    if (state.isStale) {
                        OfflineBanner("Rate-limited or offline (${state.staleReason ?: "stale"}) — cached roster")
                    }

                    CrowCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.war?.let { "${it.ownFactionName} vs ${it.enemyFactionName}" }
                                        ?: state.factionName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CrowColors.Text
                                )
                                Text(
                                    text = state.war?.let { if (it.isActive) "Active Ranked War" else "Ended War" }
                                        ?: "Manual Stakeout · ID ${state.factionId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrowColors.SheenSoft
                                )
                            }
                            TextButton(onClick = { showManualFactionDialog = true }) {
                                Text("Change")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        StaleBadge(fetchedAtMs = state.fetchedAtMs, clock = clock)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WarQuickFilter.entries.forEach { f ->
                            FilterChip(
                                selected = f in quickFilters,
                                onClick = { viewModel.toggleQuickFilter(f) },
                                label = { Text(f.label) }
                            )
                        }
                    }

                    if (state.rows.isEmpty()) {
                        EmptyView(title = "No members match active filters.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.rows, key = { it.member.playerId }) { row ->
                                MemberRow(
                                    row = row,
                                    ffEnabled = state.ffEnabled,
                                    ffLowThreshold = prefs.ffLowThreshold,
                                    ffHighThreshold = prefs.ffHighThreshold,
                                    clock = clock,
                                    dimmed = dimmed,
                                    onToggleStar = { viewModel.toggleStar(row.member.playerId) },
                                    onAttack = {
                                        scope.launch {
                                            attackNavigator.open(context, row.member.playerId, onNavigateToBrowser)
                                        }
                                    },
                                    onRowClick = { detailRow = row }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualFactionDialog) {
        AlertDialog(
            onDismissRequest = { showManualFactionDialog = false },
            title = { Text("Stake Out Faction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter an enemy faction ID (leave blank to auto-detect your active ranked war).")
                    OutlinedTextField(
                        value = manualFactionInput,
                        onValueChange = { manualFactionInput = it.filter(Char::isDigit) },
                        label = { Text("Faction ID") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setManualFactionId(manualFactionInput)
                        showManualFactionDialog = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showManualFactionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFiltersSheet) {
        var minFfStr by remember(extraFilters) { mutableStateOf(extraFilters.minFf?.toString().orEmpty()) }
        var maxFfStr by remember(extraFilters) { mutableStateOf(extraFilters.maxFf?.toString().orEmpty()) }
        var minLvlStr by remember(extraFilters) { mutableStateOf(extraFilters.minLevel?.toString().orEmpty()) }
        var maxLvlStr by remember(extraFilters) { mutableStateOf(extraFilters.maxLevel?.toString().orEmpty()) }
        var offlineHrsStr by remember(extraFilters) { mutableStateOf(extraFilters.hideOfflineOverHours?.toString().orEmpty()) }

        ModalBottomSheet(
            onDismissRequest = { showFiltersSheet = false },
            containerColor = CrowColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Sort & Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WarSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { viewModel.setSortMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minFfStr,
                        onValueChange = { minFfStr = it },
                        label = { Text("Min FF") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxFfStr,
                        onValueChange = { maxFfStr = it },
                        label = { Text("Max FF") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minLvlStr,
                        onValueChange = { minLvlStr = it.filter(Char::isDigit) },
                        label = { Text("Min Level") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxLvlStr,
                        onValueChange = { maxLvlStr = it.filter(Char::isDigit) },
                        label = { Text("Max Level") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = offlineHrsStr,
                    onValueChange = { offlineHrsStr = it.filter(Char::isDigit) },
                    label = { Text("Hide offline > X hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.setExtraFilters(
                            extraFilters.copy(
                                minFf = minFfStr.toDoubleOrNull(),
                                maxFf = maxFfStr.toDoubleOrNull(),
                                minLevel = minLvlStr.toIntOrNull(),
                                maxLevel = maxLvlStr.toIntOrNull(),
                                hideOfflineOverHours = offlineHrsStr.toIntOrNull()
                            )
                        )
                        showFiltersSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Apply") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    detailRow?.let { row ->
        ModalBottomSheet(
            onDismissRequest = { detailRow = null },
            containerColor = CrowColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${row.member.name} [${row.member.playerId}]",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Status: ${row.member.status.description}", color = CrowColors.TextMuted)
                Text("Last action: ${row.member.lastActionStatus ?: "—"} (${row.member.lastActionRelative ?: "—"})", color = CrowColors.TextMuted)

                row.stakeout?.let { stake ->
                    Spacer(Modifier.height(4.dp))
                    Text("Stakeout Alert Options", fontWeight = FontWeight.SemiBold, color = CrowColors.Text)
                    StakeoutToggle("Leaving hospital", stake.alertHospital) {
                        viewModel.updateStakeout(stake.copy(alertHospital = it))
                    }
                    StakeoutToggle("Landing", stake.alertLanding) {
                        viewModel.updateStakeout(stake.copy(alertLanding = it))
                    }
                    StakeoutToggle("Becomes Okay", stake.alertOkay) {
                        viewModel.updateStakeout(stake.copy(alertOkay = it))
                    }
                    StakeoutToggle("Goes Online", stake.alertOnline) {
                        viewModel.updateStakeout(stake.copy(alertOnline = it))
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addToTargets(row.member)
                            detailRow = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add to targets") }
                    OutlinedButton(
                        onClick = {
                            browserNavigator.requestInAppOpen(TornUrls.profile(row.member.playerId))
                            detailRow = null
                            onNavigateToBrowser()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Open profile") }
                }
                Button(
                    onClick = {
                        detailRow = null
                        scope.launch {
                            attackNavigator.open(context, row.member.playerId, onNavigateToBrowser)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Attack") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StakeoutToggle(
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
