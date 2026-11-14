package app.crowdhub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.profile.OnboardingStep
import app.crowdhub.core.ui.components.CrowdWordmark
import app.crowdhub.core.ui.nav.CrowRoute
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.CrowdHubTheme
import app.crowdhub.feature.browser.BrowserScreen
import app.crowdhub.feature.chain.TargetsScreen
import app.crowdhub.feature.dashboard.DashboardScreen
import app.crowdhub.feature.dashboard.DashboardViewModel
import app.crowdhub.feature.flight.FlightDebugScreen
import app.crowdhub.feature.profiles.AddAccountScreen
import app.crowdhub.feature.profiles.OnboardingScreen
import app.crowdhub.feature.profiles.ProfilesScreen
import app.crowdhub.feature.scripts.ScriptEditorScreen
import app.crowdhub.feature.scripts.ScriptsScreen
import app.crowdhub.feature.settings.AboutScreen
import app.crowdhub.feature.settings.DeveloperScreen
import app.crowdhub.feature.settings.PrivacyScreen
import app.crowdhub.feature.settings.ReliabilityScreen
import app.crowdhub.feature.settings.SettingsScreen
import app.crowdhub.feature.stocks.StockDetailScreen
import app.crowdhub.feature.stocks.StocksScreen
import app.crowdhub.feature.stocks.StocksViewModel
import app.crowdhub.feature.war.WarScreen
import app.crowdhub.feature.war.WarViewModel
import app.crowdhub.provider.torn.TornUrls

private data class TopTabItem(
    val route: CrowRoute,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val TOP_TABS = listOf(
    TopTabItem(CrowRoute.Dashboard, "Dash", Icons.Outlined.Dashboard, "tab_dashboard"),
    TopTabItem(CrowRoute.Stocks, "Stocks", Icons.Outlined.ShowChart, "tab_stocks"),
    TopTabItem(CrowRoute.War, "War", Icons.Outlined.Shield, "tab_war"),
    TopTabItem(CrowRoute.Targets, "Targets", Icons.Outlined.GpsFixed, "tab_targets"),
    TopTabItem(CrowRoute.Browser(), "Browser", Icons.Outlined.Language, "tab_browser")
)

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingDeepLink = intent?.getStringExtra(Scheduler.EXTRA_DEEP_LINK)

        val app = application as CrowdHubApp
        val c = app.container

        setContent {
            CrowdHubTheme {
                var startRoute by mutableStateOf<CrowRoute?>(null)

                LaunchedEffect(Unit) {
                    val step = c.profileRepository.determineStartStep()
                    startRoute = if (step == OnboardingStep.DONE) CrowRoute.Dashboard else CrowRoute.Onboarding
                }

                val initial = startRoute
                if (initial == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CrowColors.CrowBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        CrowdWordmark()
                    }
                } else {
                    CrowdHubRoot(
                        container = c,
                        startRoute = initial,
                        deepLink = pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(Scheduler.EXTRA_DEEP_LINK)?.let { link ->
            pendingDeepLink = link
        }
    }
}

@Composable
private fun CrowdHubRoot(
    container: AppContainer,
    startRoute: CrowRoute,
    deepLink: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            profileRepository = container.profileRepository,
            tornApi = container.tornApi,
            alertEngine = container.alertEngine,
            clock = container.clock
        )
    )
    val stocksVm: StocksViewModel = viewModel(
        factory = StocksViewModel.Factory(
            profileRepository = container.profileRepository,
            stockRepository = container.stockRepository,
            alertEngine = container.alertEngine,
            clock = container.clock
        )
    )
    val warVm: WarViewModel = viewModel(
        factory = WarViewModel.Factory(
            profileRepository = container.profileRepository,
            tornApi = container.tornApi,
            ffRepository = container.ffRepository,
            flightRepository = container.flightRepository,
            stakeoutManager = container.stakeoutManager,
            alertEngine = container.alertEngine,
            targetDao = container.db.targetDao(),
            settingsStore = container.settingsStore,
            clock = container.clock
        )
    )

    fun navigateTopTab(route: CrowRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLink, startRoute) {
        if (deepLink != null && startRoute != CrowRoute.Onboarding) {
            val targetRoute: CrowRoute? = when {
                deepLink == "dashboard" -> CrowRoute.Dashboard
                deepLink == "war" -> CrowRoute.War
                deepLink == "targets" -> CrowRoute.Targets
                deepLink == "browser" -> CrowRoute.Browser()
                deepLink.startsWith("stock/") ->
                    deepLink.removePrefix("stock/").toIntOrNull()?.let { CrowRoute.StockDetail(it) }
                deepLink.startsWith("account/") ->
                    deepLink.removePrefix("account/").takeIf { it.isNotBlank() }?.let { CrowRoute.AccountDetail(it) }
                else -> null
            }
            if (targetRoute != null) {
                navController.navigate(targetRoute) {
                    launchSingleTop = true
                }
            }
            onDeepLinkConsumed()
        }
    }

    val isTopTabDestination = destination != null && (
        destination.hasRoute<CrowRoute.Dashboard>() ||
            destination.hasRoute<CrowRoute.Stocks>() ||
            destination.hasRoute<CrowRoute.War>() ||
            destination.hasRoute<CrowRoute.Targets>() ||
            destination.hasRoute<CrowRoute.Browser>()
        )

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (isTopTabDestination) {
                NavigationBar(
                    containerColor = CrowColors.Surface,
                    contentColor = CrowColors.Text
                ) {
                    TOP_TABS.forEach { tab ->
                        val selected = when (tab.route) {
                            CrowRoute.Dashboard -> destination?.hasRoute<CrowRoute.Dashboard>() == true
                            CrowRoute.Stocks -> destination?.hasRoute<CrowRoute.Stocks>() == true
                            CrowRoute.War -> destination?.hasRoute<CrowRoute.War>() == true
                            CrowRoute.Targets -> destination?.hasRoute<CrowRoute.Targets>() == true
                            is CrowRoute.Browser -> destination?.hasRoute<CrowRoute.Browser>() == true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CrowColors.SheenSoft,
                                selectedTextColor = CrowColors.SheenSoft,
                                indicatorColor = CrowColors.Sheen.copy(alpha = 0.22f),
                                unselectedIconColor = CrowColors.TextMuted,
                                unselectedTextColor = CrowColors.TextMuted
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<CrowRoute.Onboarding> {
                OnboardingScreen(
                    profileRepository = container.profileRepository,
                    tornProvider = container.tornProvider,
                    settingsStore = container.settingsStore,
                    scheduler = container.scheduler,
                    onFinished = {
                        navController.navigate(CrowRoute.Dashboard) {
                            popUpTo(CrowRoute.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable<CrowRoute.Dashboard> {
                DashboardScreen(
                    viewModel = dashboardVm,
                    settingsStore = container.settingsStore,
                    clock = container.clock,
                    onOpenAccount = { id -> navController.navigate(CrowRoute.AccountDetail(id)) },
                    onOpenBrowserHome = {
                        container.browserNavigator.requestInAppOpen(TornUrls.HOME)
                        navigateTopTab(CrowRoute.Browser())
                    },
                    onOpenScripts = { navController.navigate(CrowRoute.Scripts) },
                    onOpenSettings = { navController.navigate(CrowRoute.Settings) },
                    onOpenAbout = { navController.navigate(CrowRoute.About) }
                )
            }

            composable<CrowRoute.Stocks> {
                StocksScreen(
                    viewModel = stocksVm,
                    settingsStore = container.settingsStore,
                    clock = container.clock,
                    onOpenDetail = { stockId -> navController.navigate(CrowRoute.StockDetail(stockId)) },
                    onOpenAccount = { id -> navController.navigate(CrowRoute.AccountDetail(id)) }
                )
            }

            composable<CrowRoute.StockDetail> { entry ->
                val args = entry.toRoute<CrowRoute.StockDetail>()
                StockDetailScreen(
                    stockId = args.stockId,
                    viewModel = stocksVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CrowRoute.War> {
                WarScreen(
                    viewModel = warVm,
                    settingsStore = container.settingsStore,
                    attackNavigator = container.attackNavigator,
                    browserNavigator = container.browserNavigator,
                    clock = container.clock,
                    onOpenAccount = { id -> navController.navigate(CrowRoute.AccountDetail(id)) },
                    onNavigateToBrowser = { navigateTopTab(CrowRoute.Browser()) }
                )
            }

            composable<CrowRoute.Targets> {
                TargetsScreen(
                    viewModel = container.targetsViewModel,
                    settingsStore = container.settingsStore,
                    attackNavigator = container.attackNavigator,
                    browserNavigator = container.browserNavigator,
                    clock = container.clock,
                    onNavigateToBrowser = { navigateTopTab(CrowRoute.Browser()) }
                )
            }

            composable<CrowRoute.Browser> { entry ->
                val args = entry.toRoute<CrowRoute.Browser>()
                BrowserScreen(
                    initialUrlArg = args.initialUrl ?: container.browserNavigator.consumePendingUrl(),
                    profileRepository = container.profileRepository,
                    settingsStore = container.settingsStore,
                    secretVault = container.secretVault,
                    webProfileManager = container.webProfileManager,
                    urlGuard = container.urlGuard,
                    sessionMonitor = container.sessionMonitor,
                    browserNavigator = container.browserNavigator,
                    userscriptEngine = container.userscriptEngine,
                    gmBridge = container.gmBridge,
                    targetsViewModel = container.targetsViewModel,
                    clock = container.clock,
                    onOpenScripts = { navController.navigate(CrowRoute.Scripts) },
                    onBackPressed = { navigateTopTab(CrowRoute.Dashboard) }
                )
            }

            composable<CrowRoute.Scripts> {
                ScriptsScreen(
                    profileRepository = container.profileRepository,
                    scriptRepository = container.scriptRepository,
                    settingsStore = container.settingsStore,
                    onBack = { navController.popBackStack() },
                    onEditScript = { id -> navController.navigate(CrowRoute.ScriptEditor(id)) }
                )
            }

            composable<CrowRoute.ScriptEditor> { entry ->
                val args = entry.toRoute<CrowRoute.ScriptEditor>()
                ScriptEditorScreen(
                    scriptId = args.scriptId.orEmpty(),
                    scriptRepository = container.scriptRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CrowRoute.Settings> {
                SettingsScreen(
                    settingsStore = container.settingsStore,
                    profileRepository = container.profileRepository,
                    responseCache = container.responseCache,
                    stockRepository = container.stockRepository,
                    onBack = { navController.popBackStack() },
                    onOpenAccount = { id -> navController.navigate(CrowRoute.AccountDetail(id)) },
                    onOpenProfiles = { navController.navigate(CrowRoute.Profiles) },
                    onOpenReliability = { navController.navigate(CrowRoute.Reliability) },
                    onOpenPrivacy = { navController.navigate(CrowRoute.Privacy) },
                    onOpenAbout = { navController.navigate(CrowRoute.About) },
                    onOpenDeveloperProbe = { navController.navigate(CrowRoute.DeveloperProbe) }
                )
            }

            composable<CrowRoute.Profiles> {
                ProfilesScreen(
                    profileRepository = container.profileRepository,
                    onBack = { navController.popBackStack() },
                    onOpenAccount = { id -> navController.navigate(CrowRoute.AccountDetail(id)) },
                    onNeedOnboarding = {
                        navController.navigate(CrowRoute.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<CrowRoute.AccountDetail> { entry ->
                val args = entry.toRoute<CrowRoute.AccountDetail>()
                AddAccountScreen(
                    accountId = args.accountId,
                    accountDao = container.db.providerAccountDao(),
                    profileRepository = container.profileRepository,
                    tornProvider = container.tornProvider,
                    secretVault = container.secretVault,
                    webProfileManager = container.webProfileManager,
                    sessionMonitor = container.sessionMonitor,
                    settingsStore = container.settingsStore,
                    ffRepository = container.ffRepository,
                    onBack = { navController.popBackStack() },
                    onAccountDeleted = {
                        navController.navigate(CrowRoute.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<CrowRoute.Privacy> {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }

            composable<CrowRoute.About> {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            composable<CrowRoute.Reliability> {
                ReliabilityScreen(
                    scheduler = container.scheduler,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CrowRoute.DeveloperProbe> {
                DeveloperScreen(
                    profileRepository = container.profileRepository,
                    tornApi = container.tornApi,
                    rateLimiter = container.rateLimiter,
                    clock = container.clock,
                    onBack = { navController.popBackStack() },
                    onOpenFlightDebug = { navController.navigate(CrowRoute.FlightDebug) }
                )
            }

            composable<CrowRoute.FlightDebug> {
                FlightDebugScreen(
                    flightRepository = container.flightRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
