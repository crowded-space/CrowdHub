package app.crowdhub.feature.chain

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.components.OfflineBanner
import app.crowdhub.core.ui.components.StaleBadge
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.browser.AttackNavigator
import app.crowdhub.feature.flight.FlightChip
import app.crowdhub.provider.torn.TornUrls
import app.crowdhub.provider.torn.TornUserListCategory
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

    val uiState by viewModel.peopleUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.currentPeopleSearch.collectAsStateWithLifecycle()
    val listStatusFilter by viewModel.currentPeopleListStatusFilter.collectAsStateWithLifecycle()
    val sortMode by viewModel.currentSort.collectAsStateWithLifecycle()
    val filter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())

    var showFiltersSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addForStakeout by remember { mutableStateOf(false) }
    var addIdInput by remember { mutableStateOf("") }
    var addNoteInput by remember { mutableStateOf("") }
    var editingNoteTarget by remember { mutableStateOf<TargetItem?>(null) }
    var editNoteInput by remember { mutableStateOf("") }
    var detailStakeoutId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (uiState.activeTab == PeopleInnerTab.FRIENDS) {
            viewModel.selectPeopleTab(PeopleInnerTab.FRIENDS)
        }
    }

    BackHandler(enabled = uiState.activeTab != PeopleInnerTab.FRIENDS) {
        viewModel.selectPeopleTab(PeopleInnerTab.FRIENDS)
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("People") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                actions = {
                    if (uiState.activeTab == PeopleInnerTab.CHAIN || uiState.activeTab == PeopleInnerTab.ADDITIONAL) {
                        IconButton(
                            onClick = { showFiltersSheet = true },
                            modifier = Modifier.testTag("people_filter_button")
                        ) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "Sort and filter")
                        }
                    }
                    if (uiState.activeTab == PeopleInnerTab.ADDITIONAL || uiState.activeTab == PeopleInnerTab.STAKEOUT) {
                        IconButton(
                            onClick = {
                                addForStakeout = uiState.activeTab == PeopleInnerTab.STAKEOUT
                                addIdInput = ""
                                addNoteInput = ""
                                showAddDialog = true
                            },
                            modifier = Modifier.testTag("people_add_button")
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = if (uiState.activeTab == PeopleInnerTab.STAKEOUT) "Add stakeout player" else "Add target"
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refreshCurrentPeopleTab(forceRefresh = true) },
                        modifier = Modifier.testTag("people_refresh_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh current tab")
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
            val tabs = PeopleInnerTab.entries
            val selectedIndex = tabs.indexOf(uiState.activeTab).coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = CrowColors.Surface,
                contentColor = CrowColors.SheenSoft,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = CrowColors.SheenSoft
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val selected = tab == uiState.activeTab
                    Tab(
                        selected = selected,
                        onClick = { viewModel.selectPeopleTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) CrowColors.SheenSoft else CrowColors.TextMuted
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
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

            val openProfileInNewTab: (Long) -> Unit = { pid ->
                browserNavigator.requestInAppOpen(TornUrls.profile(pid), inNewTab = true, switchToTab = true)
                onNavigateToBrowser()
            }
            val openAttackInNewTab: (Long) -> Unit = { pid ->
                scope.launch { attackNavigator.open(context, pid, onNavigateToBrowser) }
            }

            when (uiState.activeTab) {
                PeopleInnerTab.FRIENDS,
                PeopleInnerTab.TARGETS,
                PeopleInnerTab.ENEMIES -> {
                    val category = when (uiState.activeTab) {
                        PeopleInnerTab.FRIENDS -> TornUserListCategory.FRIENDS
                        PeopleInnerTab.ENEMIES -> TornUserListCategory.ENEMIES
                        else -> TornUserListCategory.TARGETS
                    }
                    val listItems = when (category) {
                        TornUserListCategory.FRIENDS -> uiState.friendsItems
                        TornUserListCategory.ENEMIES -> uiState.enemiesItems
                        TornUserListCategory.TARGETS -> uiState.targetsListItems
                    }
                    PeopleSearchAndStatusHeader(
                        query = searchQuery,
                        onQueryChange = viewModel::setPeopleSearchQuery,
                        statusFilter = listStatusFilter,
                        onStatusFilterChange = viewModel::setPeopleListStatusFilter
                    )
                    PeopleApiListSection(
                        category = category,
                        items = listItems,
                        fetchedAtMs = uiState.listFetchedAtMs[category],
                        errorMessage = uiState.listErrors[category],
                        ffEnabled = uiState.ffEnabled,
                        ffLow = prefs.ffLowThreshold,
                        ffHigh = prefs.ffHighThreshold,
                        clock = clock,
                        onRefresh = { viewModel.refreshUserListCategory(category, forceRefresh = true) },
                        onToggleStakeout = { item -> viewModel.toggleStakeout(item.playerId, item.name) },
                        onSaveToAdditional = { item ->
                            viewModel.saveToAdditional(item.playerId, item.name, item.note, "LIST_${category.name}")
                        },
                        onOpenProfile = openProfileInNewTab,
                        onAttack = openAttackInNewTab
                    )
                }

                PeopleInnerTab.CHAIN -> {
                    val chainTabs = remember(uiState.ffEnabled) {
                        if (uiState.ffEnabled) ChainSubTab.entries
                        else listOf(ChainSubTab.HOSPITAL)
                    }
                    if (chainTabs.size > 1) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            chainTabs.forEachIndexed { idx, sub ->
                                SegmentedButton(
                                    selected = uiState.chainSubTab == sub,
                                    onClick = { viewModel.selectChainSubTab(sub) },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = chainTabs.size),
                                    modifier = Modifier.testTag(sub.testTag)
                                ) {
                                    Text(sub.label)
                                }
                            }
                        }
                    }

                    when (uiState.chainSubTab) {
                        ChainSubTab.HOSPITAL -> {
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
                                                browserNavigator.requestInAppOpen(
                                                    TornUrls.HOSPITAL_LIST,
                                                    inNewTab = true,
                                                    switchToTab = true
                                                )
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
                            if (uiState.hospitalChainItems.isEmpty()) {
                                EmptyView(
                                    title = "No captured hospital targets",
                                    subtitle = "Open Hospital in Browser, scroll to the page you want, tap Capture.",
                                    actionLabel = "Open Hospital in Browser",
                                    onAction = {
                                        browserNavigator.requestInAppOpen(
                                            TornUrls.HOSPITAL_LIST,
                                            inNewTab = true,
                                            switchToTab = true
                                        )
                                        onNavigateToBrowser()
                                    }
                                )
                            } else {
                                PeopleTargetCardList(
                                    items = uiState.hospitalChainItems,
                                    ffEnabled = uiState.ffEnabled,
                                    ffLow = prefs.ffLowThreshold,
                                    ffHigh = prefs.ffHighThreshold,
                                    dimmed = isHospitalStale,
                                    clock = clock,
                                    isAdditionalTab = false,
                                    onSaveToAdditional = { viewModel.saveFromOtherTab(it) },
                                    onToggleWatch = { viewModel.toggleWatchTarget(it) },
                                    onEditNote = {},
                                    onRemove = {},
                                    onOpenProfile = openProfileInNewTab,
                                    onAttack = openAttackInNewTab
                                )
                            }
                        }

                        ChainSubTab.FFSCOUTER -> {
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
                            if (uiState.ffChainItems.isEmpty()) {
                                EmptyView(
                                    title = "No FFScouter targets loaded",
                                    subtitle = "Tap Load FFScouter targets to query suggested chain targets.",
                                    actionLabel = "Load targets",
                                    onAction = { viewModel.fetchFfScouterTargets() }
                                )
                            } else {
                                PeopleTargetCardList(
                                    items = uiState.ffChainItems,
                                    ffEnabled = true,
                                    ffLow = prefs.ffLowThreshold,
                                    ffHigh = prefs.ffHighThreshold,
                                    dimmed = false,
                                    clock = clock,
                                    isAdditionalTab = false,
                                    onSaveToAdditional = { viewModel.saveFromOtherTab(it) },
                                    onToggleWatch = { viewModel.toggleWatchTarget(it) },
                                    onEditNote = {},
                                    onRemove = {},
                                    onOpenProfile = openProfileInNewTab,
                                    onAttack = openAttackInNewTab
                                )
                            }
                        }
                    }
                }

                PeopleInnerTab.ADDITIONAL -> {
                    if (uiState.additionalItems.isEmpty()) {
                        EmptyView(
                            title = "Additional list is empty",
                            subtitle = "Save players from Friends, Targets, Enemies, Chain, or War, or add a Torn Player ID manually.",
                            actionLabel = "Add player",
                            onAction = {
                                addForStakeout = false
                                addIdInput = ""
                                addNoteInput = ""
                                showAddDialog = true
                            }
                        )
                    } else {
                        PeopleTargetCardList(
                            items = uiState.additionalItems,
                            ffEnabled = uiState.ffEnabled,
                            ffLow = prefs.ffLowThreshold,
                            ffHigh = prefs.ffHighThreshold,
                            dimmed = false,
                            clock = clock,
                            isAdditionalTab = true,
                            onSaveToAdditional = {},
                            onToggleWatch = { viewModel.toggleWatchTarget(it) },
                            onEditNote = { target ->
                                editingNoteTarget = target
                                editNoteInput = target.note.orEmpty()
                            },
                            onRemove = { viewModel.removeMyTarget(it.playerId) },
                            onOpenProfile = openProfileInNewTab,
                            onAttack = openAttackInNewTab
                        )
                    }
                }

                PeopleInnerTab.STAKEOUT -> {
                    PeopleSearchAndStatusHeader(
                        query = searchQuery,
                        onQueryChange = viewModel::setPeopleSearchQuery,
                        statusFilter = listStatusFilter,
                        onStatusFilterChange = viewModel::setPeopleListStatusFilter,
                        showStatusFilters = false
                    )
                    PeopleStakeoutSection(
                        items = uiState.stakeoutItems,
                        ffEnabled = uiState.ffEnabled,
                        ffLow = prefs.ffLowThreshold,
                        ffHigh = prefs.ffHighThreshold,
                        clock = clock,
                        onAddStakeoutClick = {
                            addForStakeout = true
                            addIdInput = ""
                            addNoteInput = ""
                            showAddDialog = true
                        },
                        onToggleStar = { viewModel.toggleStakeout(it.stakeout.playerId, it.name) },
                        onSelectDetail = { detailStakeoutId = it.stakeout.playerId },
                        onOpenProfile = openProfileInNewTab,
                        onAttack = openAttackInNewTab
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (addForStakeout) "Add Stakeout Player" else "Add to Additional List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addIdInput,
                        onValueChange = { addIdInput = it.filter(Char::isDigit) },
                        label = { Text("Torn Player ID") },
                        singleLine = true
                    )
                    if (!addForStakeout) {
                        OutlinedTextField(
                            value = addNoteInput,
                            onValueChange = { addNoteInput = it },
                            label = { Text("Note (optional)") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addForStakeout) {
                            viewModel.addManualStakeout(addIdInput)
                        } else {
                            viewModel.addManualTarget(addIdInput, addNoteInput)
                        }
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

    editingNoteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { editingNoteTarget = null },
            title = { Text("Edit Note · ${target.name}") },
            text = {
                OutlinedTextField(
                    value = editNoteInput,
                    onValueChange = { editNoteInput = it },
                    label = { Text("Note") },
                    singleLine = false
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAdditionalNote(target.playerId, editNoteInput)
                        editingNoteTarget = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteTarget = null }) { Text("Cancel") }
            }
        )
    }

    detailStakeoutId?.let { pid ->
        val item = uiState.stakeoutItems.firstOrNull { it.stakeout.playerId == pid }
        if (item == null) {
            detailStakeoutId = null
        } else {
            ModalBottomSheet(
                onDismissRequest = { detailStakeoutId = null },
                containerColor = CrowColors.Surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${item.name} [${item.stakeout.playerId}]",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Status: ${item.statusDescription}", color = CrowColors.TextMuted)
                    val lastAct = item.lastActionRelative ?: item.lastActionStatus
                    if (!lastAct.isNullOrBlank()) {
                        Text("Last action: $lastAct", color = CrowColors.TextMuted)
                    }
                    item.flightEstimate?.let { est ->
                        FlightChip(estimate = est, clock = clock)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Stakeout Alert Options", fontWeight = FontWeight.SemiBold, color = CrowColors.Text)
                    StakeoutAlertToggleRow("Leaving hospital", item.stakeout.alertHospital) {
                        viewModel.updateStakeoutAlerts(item.stakeout.copy(alertHospital = it))
                    }
                    StakeoutAlertToggleRow("Landing", item.stakeout.alertLanding) {
                        viewModel.updateStakeoutAlerts(item.stakeout.copy(alertLanding = it))
                    }
                    StakeoutAlertToggleRow("Becomes Okay", item.stakeout.alertOkay) {
                        viewModel.updateStakeoutAlerts(item.stakeout.copy(alertOkay = it))
                    }
                    StakeoutAlertToggleRow("Goes Online", item.stakeout.alertOnline) {
                        viewModel.updateStakeoutAlerts(item.stakeout.copy(alertOnline = it))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveToAdditional(item.stakeout.playerId, item.name, null, "STAKEOUT")
                                detailStakeoutId = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (item.inAdditional) "In Additional" else "Add to Additional") }
                        OutlinedButton(
                            onClick = {
                                browserNavigator.requestInAppOpen(
                                    TornUrls.profile(item.stakeout.playerId),
                                    inNewTab = true,
                                    switchToTab = true
                                )
                                detailStakeoutId = null
                                onNavigateToBrowser()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Open profile") }
                    }
                    Button(
                        onClick = {
                            detailStakeoutId = null
                            scope.launch { attackNavigator.open(context, item.stakeout.playerId, onNavigateToBrowser) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Attack") }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
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
