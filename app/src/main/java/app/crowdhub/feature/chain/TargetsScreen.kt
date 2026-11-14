package app.crowdhub.feature.chain

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import app.crowdhub.connector.ffscouter.BsText
import app.crowdhub.connector.ffscouter.FfBadge
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CountdownText
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.components.StatusChip
import app.crowdhub.core.ui.components.StatusKind
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.browser.AttackNavigator
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(
    viewModel: TargetsViewModel,
    settingsStore: SettingsStore,
    attackNavigator: AttackNavigator,
    browserNavigator: BrowserNavigator,
    clock: Clock,
    onNavigateToBrowser: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortMode by viewModel.currentSort.collectAsStateWithLifecycle()
    val filter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    var showFiltersSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addIdInput by remember { mutableStateOf("") }
    var addNoteInput by remember { mutableStateOf("") }

    val availableTabs = remember(uiState.ffEnabled) {
        if (uiState.ffEnabled) TargetSourceTab.entries
        else listOf(TargetSourceTab.HOSPITAL, TargetSourceTab.MY_LIST)
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Targets") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                actions = {
                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Filter targets")
                    }
                    if (uiState.activeTab == TargetSourceTab.MY_LIST) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add target")
                        }
                    }
                    IconButton(
                        onClick = {
                            when (uiState.activeTab) {
                                TargetSourceTab.HOSPITAL -> viewModel.refreshTopN()
                                TargetSourceTab.FFSCOUTER -> viewModel.fetchFfScouterTargets()
                                TargetSourceTab.MY_LIST -> viewModel.refreshMyListTargets()
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh targets")
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                availableTabs.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = availableTabs.size),
                        modifier = Modifier.testTag("targets_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.label)
                    }
                }
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = CrowColors.Sheen
                )
            }

            uiState.message?.let { msg ->
                OfflineBanner(message = msg)
            }

            when (uiState.activeTab) {
                TargetSourceTab.HOSPITAL -> {
                    val capturedAt = uiState.hospitalCapturedAtMs
                    val isHospitalStale = capturedAt != null && (clock.nowEpochMs() - capturedAt > 10 * 60_000L)
                    if (capturedAt != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StaleBadge(fetchedAtMs = capturedAt, clock = clock)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        browserNavigator.requestInAppOpen(TornUrls.HOSPITAL_LIST)
                                        onNavigateToBrowser()
                                    }
                                ) {
                                    Text("Hospital in Browser")
                                }
                                Button(onClick = { viewModel.refreshTopN() }) {
                                    Text("Refresh top 20")
                                }
                            }
                        }
                        if (isHospitalStale) {
                            OfflineBanner("Hospital capture is older than 10 minutes — recapture or refresh.")
                        }
                    }

                    if (uiState.items.isEmpty()) {
                        EmptyView(
                            title = "No captured hospital targets",
                            subtitle = "Open Hospital in Browser, scroll to the page you want, tap Capture.",
                            actionLabel = "Open Hospital in Browser",
                            onAction = {
                                browserNavigator.requestInAppOpen(TornUrls.HOSPITAL_LIST)
                                onNavigateToBrowser()
                            }
                        )
                    } else {
                        TargetList(
                            items = uiState.items,
                            ffEnabled = uiState.ffEnabled,
                            ffLow = prefs.ffLowThreshold,
                            ffHigh = prefs.ffHighThreshold,
                            dimmed = isHospitalStale,
                            clock = clock,
                            isMyList = false,
                            onSaveToMyList = { viewModel.saveFromOtherTab(it) },
                            onToggleWatch = { viewModel.toggleWatchTarget(it) },
                            onRemove = {},
                            onAttack = { id ->
                                scope.launch { attackNavigator.open(context, id, onNavigateToBrowser) }
                            }
                        )
                    }
                }
                TargetSourceTab.FFSCOUTER -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { viewModel.fetchFfScouterTargets() }) {
                            Text("Load FFScouter targets")
                        }
                    }
                    if (uiState.items.isEmpty()) {
                        EmptyView(
                            title = "No FFScouter targets loaded",
                            subtitle = "Tap Load FFScouter targets to query suggested chain targets.",
                            actionLabel = "Load targets",
                            onAction = { viewModel.fetchFfScouterTargets() }
                        )
                    } else {
                        TargetList(
                            items = uiState.items,
                            ffEnabled = true,
                            ffLow = prefs.ffLowThreshold,
                            ffHigh = prefs.ffHighThreshold,
                            dimmed = false,
                            clock = clock,
                            isMyList = false,
                            onSaveToMyList = { viewModel.saveFromOtherTab(it) },
                            onToggleWatch = { viewModel.toggleWatchTarget(it) },
                            onRemove = {},
                            onAttack = { id ->
                                scope.launch { attackNavigator.open(context, id, onNavigateToBrowser) }
                            }
                        )
                    }
                }
                TargetSourceTab.MY_LIST -> {
                    if (uiState.items.isEmpty()) {
                        EmptyView(
                            title = "My target list is empty",
                            subtitle = "Add targets by player ID or save them from Hospital / War.",
                            actionLabel = "Add target",
                            onAction = { showAddDialog = true }
                        )
                    } else {
                        TargetList(
                            items = uiState.items,
                            ffEnabled = uiState.ffEnabled,
                            ffLow = prefs.ffLowThreshold,
                            ffHigh = prefs.ffHighThreshold,
                            dimmed = false,
                            clock = clock,
                            isMyList = true,
                            onSaveToMyList = {},
                            onToggleWatch = { viewModel.toggleWatchTarget(it) },
                            onRemove = { viewModel.removeMyTarget(it.playerId) },
                            onAttack = { id ->
                                scope.launch { attackNavigator.open(context, id, onNavigateToBrowser) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addIdInput,
                        onValueChange = { addIdInput = it.filter(Char::isDigit) },
                        label = { Text("Torn Player ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addNoteInput,
                        onValueChange = { addNoteInput = it },
                        label = { Text("Note (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addManualTarget(addIdInput, addNoteInput)
                        addIdInput = ""
                        addNoteInput = ""
                        showAddDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFiltersSheet) {
        var minFfStr by remember(filter) { mutableStateOf(filter.minFf?.toString().orEmpty()) }
        var maxFfStr by remember(filter) { mutableStateOf(filter.maxFf?.toString().orEmpty()) }
        var minLvlStr by remember(filter) { mutableStateOf(filter.minLevel?.toString().orEmpty()) }
        var maxLvlStr by remember(filter) { mutableStateOf(filter.maxLevel?.toString().orEmpty()) }
        var maxBsStr by remember(filter) { mutableStateOf(filter.maxBs?.toString().orEmpty()) }
        var leavingMinStr by remember(filter) { mutableStateOf(filter.leavingWithinMin?.toString().orEmpty()) }
        var statusFilter by remember(filter) { mutableStateOf(filter.statusFilter) }

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
                Text("Sort & Filter Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { viewModel.setSortMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TargetStatusFilter.entries.forEach { sf ->
                        FilterChip(
                            selected = statusFilter == sf,
                            onClick = { statusFilter = sf },
                            label = { Text(sf.label) }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxBsStr,
                        onValueChange = { maxBsStr = it.filter(Char::isDigit) },
                        label = { Text("Max BS") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leavingMinStr,
                        onValueChange = { leavingMinStr = it.filter(Char::isDigit) },
                        label = { Text("Leaves <= min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        viewModel.setFilterCriteria(
                            TargetFilterCriteria(
                                minFf = minFfStr.toDoubleOrNull(),
                                maxFf = maxFfStr.toDoubleOrNull(),
                                minLevel = minLvlStr.toIntOrNull(),
                                maxLevel = maxLvlStr.toIntOrNull(),
                                maxBs = maxBsStr.toLongOrNull(),
                                statusFilter = statusFilter,
                                leavingWithinMin = leavingMinStr.toIntOrNull()
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
}

@Composable
private fun TargetList(
    items: List<TargetItem>,
    ffEnabled: Boolean,
    ffLow: Double,
    ffHigh: Double,
    dimmed: Boolean,
    clock: Clock,
    isMyList: Boolean,
    onSaveToMyList: (TargetItem) -> Unit,
    onToggleWatch: (TargetItem) -> Unit,
    onRemove: (TargetItem) -> Unit,
    onAttack: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.playerId }) { item ->
            CrowCard(dimmed = dimmed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.name}${item.level?.let { " · Lv $it" } ?: ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CrowColors.Text
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(status = item.status)
                            if (item.status == StatusKind.HOSPITAL && item.hospitalLeavesAtSec != null) {
                                CountdownText(
                                    targetEpochMs = item.hospitalLeavesAtSec * 1_000L,
                                    clock = clock
                                )
                            }
                            if (ffEnabled) {
                                FfBadge(item.fairFight, ffLow, ffHigh)
                                BsText(item.bsEstimate, item.bsHuman)
                            }
                        }
                        item.note?.let { n ->
                            Text(n, style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleWatch(item) }) {
                            Icon(
                                imageVector = if (item.watched) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (item.watched) "Unwatch target" else "Watch target exit",
                                tint = if (item.watched) CrowColors.AccentD else CrowColors.TextMuted
                            )
                        }
                        if (!isMyList) {
                            IconButton(onClick = { onSaveToMyList(item) }) {
                                Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save to My List")
                            }
                        } else {
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove target", tint = CrowColors.Danger)
                            }
                        }
                        Button(
                            onClick = { onAttack(item.playerId) },
                            modifier = Modifier.testTag("target_attack_${item.playerId}")
                        ) {
                            Text("Attack")
                        }
                    }
                }
            }
        }
    }
}
