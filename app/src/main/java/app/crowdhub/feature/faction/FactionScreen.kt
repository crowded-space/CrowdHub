package app.crowdhub.feature.faction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.alerts.WatchModeService
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.feature.browser.AttackNavigator
import app.crowdhub.feature.war.WarScreen
import app.crowdhub.feature.war.WarViewModel
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactionScreen(
    factionViewModel: FactionViewModel,
    warViewModel: WarViewModel,
    settingsStore: SettingsStore,
    attackNavigator: AttackNavigator,
    browserNavigator: BrowserNavigator,
    clock: Clock,
    initialTab: FactionInnerTab? = null,
    onOpenAccount: (String) -> Unit,
    onNavigateToBrowser: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by factionViewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by factionViewModel.selectedTab.collectAsStateWithLifecycle()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    var warFiltersRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            factionViewModel.selectTab(initialTab)
        }
    }

    BackHandler(enabled = selectedTab != FactionInnerTab.MEMBERS) {
        factionViewModel.selectTab(FactionInnerTab.MEMBERS)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (selectedTab != FactionInnerTab.WAR) {
            factionViewModel.refreshCurrentTab(forceRefresh = false)
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == FactionInnerTab.WAR) return@LaunchedEffect
        while (true) {
            delay(30_000L)
            factionViewModel.refreshCurrentTab(forceRefresh = false)
        }
    }

    val basic = uiState.basic.data
    val headerTitle = if (basic != null) {
        val tagPrefix = basic.tag?.let { "[$it] " }.orEmpty()
        "$tagPrefix${basic.name}"
    } else {
        "Faction"
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text(headerTitle, maxLines = 1) },
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
                            },
                            modifier = Modifier.testTag("faction_watch_switch")
                        )
                    }
                    if (selectedTab == FactionInnerTab.WAR) {
                        IconButton(
                            onClick = { warFiltersRequest++ },
                            modifier = Modifier.testTag("faction_war_filter_button")
                        ) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "War filters & sort")
                        }
                    }
                    IconButton(
                        onClick = {
                            if (selectedTab == FactionInnerTab.WAR) {
                                warViewModel.refresh(forceRefresh = true)
                            } else {
                                factionViewModel.refreshCurrentTab(forceRefresh = true)
                            }
                        },
                        modifier = Modifier.testTag("faction_refresh_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh faction")
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
            val chain = uiState.chain.data
            if (basic != null || (chain != null && (chain.isActive || chain.isInCooldown))) {
                FactionSummaryStrip(
                    respect = basic?.respect,
                    memberCount = basic?.memberCount ?: uiState.members.data?.size,
                    capacity = basic?.capacity,
                    chainCurrent = chain?.current,
                    chainActive = chain?.isActive == true,
                    chainCooldown = chain?.isInCooldown == true
                )
            }

            val tabs = FactionInnerTab.entries
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = CrowColors.Surface,
                contentColor = CrowColors.SheenSoft,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    val idx = tabs.indexOf(selectedTab).coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color = CrowColors.SheenSoft
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Tab(
                        selected = selected,
                        onClick = { factionViewModel.selectTab(tab) },
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

            if (uiState.notInFaction && selectedTab != FactionInnerTab.WAR) {
                EmptyView(
                    title = "Not in a faction",
                    subtitle = "Join a faction in Torn to view Members, Chain, Armory, and Organized Crimes. You can still use the War tab for manual stakeouts."
                )
            } else {
                when (selectedTab) {
                    FactionInnerTab.MEMBERS -> FactionMembersTab(
                        state = uiState,
                        clock = clock,
                        onSearchChange = factionViewModel::setMemberSearch,
                        onFilterChange = factionViewModel::setMemberFilter,
                        onSortChange = factionViewModel::setMemberSort,
                        onToggleStar = factionViewModel::toggleStar,
                        onAddToTargets = factionViewModel::addToTargets,
                        onOpenProfile = { pid ->
                            browserNavigator.requestInAppOpen(TornUrls.profile(pid), inNewTab = true, switchToTab = true)
                            onNavigateToBrowser()
                        },
                        onAttack = { pid ->
                            scope.launch {
                                attackNavigator.open(context, pid, onNavigateToBrowser)
                            }
                        },
                        onRetry = { factionViewModel.refreshForTab(FactionInnerTab.MEMBERS, forceRefresh = true) },
                        onOpenAccount = onOpenAccount
                    )
                    FactionInnerTab.WAR -> WarScreen(
                        viewModel = warViewModel,
                        settingsStore = settingsStore,
                        attackNavigator = attackNavigator,
                        browserNavigator = browserNavigator,
                        clock = clock,
                        onOpenAccount = onOpenAccount,
                        onNavigateToBrowser = onNavigateToBrowser,
                        showTopBar = false,
                        openFiltersRequest = warFiltersRequest
                    )
                    FactionInnerTab.CHAIN -> FactionChainAttacksTab(
                        state = uiState,
                        clock = clock,
                        onFilterChange = factionViewModel::setAttackFilter,
                        onOpenProfile = { pid ->
                            browserNavigator.requestInAppOpen(TornUrls.profile(pid), inNewTab = true, switchToTab = true)
                            onNavigateToBrowser()
                        },
                        onAttackPlayer = { pid ->
                            scope.launch {
                                attackNavigator.open(context, pid, onNavigateToBrowser)
                            }
                        },
                        onRetry = { factionViewModel.refreshForTab(FactionInnerTab.CHAIN, forceRefresh = true) },
                        onOpenAccount = onOpenAccount
                    )
                    FactionInnerTab.ARMORY -> FactionArmoryTab(
                        state = uiState,
                        clock = clock,
                        onCategoryChange = factionViewModel::setArmoryCategory,
                        onSearchChange = factionViewModel::setArmorySearch,
                        onRetry = { factionViewModel.refreshForTab(FactionInnerTab.ARMORY, forceRefresh = true) },
                        onOpenAccount = onOpenAccount
                    )
                    FactionInnerTab.OC -> FactionOcTab(
                        state = uiState,
                        clock = clock,
                        onFilterChange = factionViewModel::setOcFilter,
                        onRetry = { factionViewModel.refreshForTab(FactionInnerTab.OC, forceRefresh = true) },
                        onOpenAccount = onOpenAccount
                    )
                }
            }
        }
    }
}

@Composable
private fun FactionSummaryStrip(
    respect: Long?,
    memberCount: Int?,
    capacity: Int?,
    chainCurrent: Int?,
    chainActive: Boolean,
    chainCooldown: Boolean
) {
    val numFmt = remember { NumberFormat.getIntegerInstance(Locale.US) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrowColors.Surface)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val parts = buildList {
            respect?.let { add("Respect ${numFmt.format(it)}") }
            if (memberCount != null) {
                val cap = capacity?.let { "/$it" }.orEmpty()
                add("$memberCount$cap members")
            }
        }
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
            color = CrowColors.TextMuted
        )

        if (chainCurrent != null && (chainActive || chainCooldown)) {
            val pillColor = if (chainActive) CrowColors.Ok else CrowColors.Jail
            Surface(
                color = pillColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (chainActive) "Chain $chainCurrent" else "Chain CD",
                    style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = pillColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
