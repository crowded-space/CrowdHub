package app.crowdhub.feature.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.browser.CrowWebView
import app.crowdhub.core.browser.SessionMonitor
import app.crowdhub.core.browser.UrlDecision
import app.crowdhub.core.browser.UrlGuard
import app.crowdhub.core.browser.WebProfileManager
import app.crowdhub.core.network.Clock
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.scripts.GmBridge
import app.crowdhub.core.scripts.RunAt
import app.crowdhub.core.scripts.UserscriptEngine
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.PersistedBrowserTab
import app.crowdhub.core.settings.QuickLinkItem
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.LoadingView
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.vault.SecretVault
import app.crowdhub.feature.chain.HospitalPageParser
import app.crowdhub.feature.chain.TargetsViewModel
import app.crowdhub.provider.torn.TornSelectors
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch
import java.util.UUID

private const val MAX_LIVE_WEBVIEWS = 8

@Composable
fun BrowserScreen(
    initialUrlArg: String?,
    profileRepository: ProfileRepository,
    settingsStore: SettingsStore,
    secretVault: SecretVault,
    webProfileManager: WebProfileManager,
    urlGuard: UrlGuard,
    sessionMonitor: SessionMonitor,
    browserNavigator: BrowserNavigator,
    userscriptEngine: UserscriptEngine,
    gmBridge: GmBridge,
    targetsViewModel: TargetsViewModel,
    clock: Clock,
    onOpenScripts: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeProfile by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        initialValue = null as ActiveProfileState?
    )
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    val sessionState by sessionMonitor.state.collectAsStateWithLifecycle()
    val pendingRequest by browserNavigator.pendingRequest.collectAsStateWithLifecycle()

    val resolvedProfileId = activeProfile?.profile?.id
    val resolvedAccountId = activeProfile?.let { it.primaryAccount?.id ?: "default" }
    val playerId = activeProfile?.primaryAccount?.externalId

    var tabs by remember { mutableStateOf<List<BrowserTabRuntime>>(emptyList()) }
    var activeTabId by remember { mutableStateOf("") }
    var hydratedForProfileId by remember { mutableStateOf<String?>(null) }
    var tabsHydrated by remember { mutableStateOf(false) }
    var consumedInitialUrlArg by remember { mutableStateOf(false) }

    var externalDialogUrl by remember { mutableStateOf<String?>(null) }
    var longPressedLinkUrl by remember { mutableStateOf<String?>(null) }
    var showQuickLinks by remember { mutableStateOf(false) }
    var showAddQuickLink by remember { mutableStateOf(false) }
    var showScriptsMenu by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    val loginAttempts = remember(resolvedProfileId) { mutableStateListOf<Long>() }
    var autoLoginHaltedReason by remember(resolvedProfileId) { mutableStateOf<String?>(null) }

    fun persistTabsState(profileId: String, nextTabs: List<BrowserTabRuntime>, nextActiveId: String) {
        if (profileId.isBlank()) return
        scope.launch {
            settingsStore.setBrowserTabs(
                profileId = profileId,
                tabs = nextTabs.map { PersistedBrowserTab(it.id, it.url, it.title) },
                activeTabId = nextActiveId
            )
        }
    }

    fun updateTab(tabId: String, transform: (BrowserTabRuntime) -> BrowserTabRuntime) {
        val targetProfileId = hydratedForProfileId
        var changedUrlOrTitle = false
        val updated = tabs.map { tab ->
            if (tab.id == tabId) {
                val next = transform(tab)
                if (next.url != tab.url || next.title != tab.title) changedUrlOrTitle = true
                next
            } else {
                tab
            }
        }
        tabs = updated
        if (changedUrlOrTitle && tabsHydrated && targetProfileId != null && targetProfileId == resolvedProfileId) {
            persistTabsState(targetProfileId, updated, activeTabId)
        }
    }

    fun selectTab(tabId: String) {
        val targetProfileId = hydratedForProfileId ?: return
        if (tabs.none { it.id == tabId }) return
        val now = clock.nowEpochMs()
        tabs = tabs.map { if (it.id == tabId) it.copy(lastActiveAtMs = now) else it }
        activeTabId = tabId
        if (tabsHydrated && targetProfileId == resolvedProfileId) {
            persistTabsState(targetProfileId, tabs, activeTabId)
        }
    }

    fun openUrlInTab(url: String, inNewTab: Boolean, switchToTab: Boolean = true, spawnedFromLink: Boolean = false) {
        val targetProfileId = hydratedForProfileId ?: resolvedProfileId ?: return
        when (val dec = urlGuard.evaluate(url, isMainFrame = true)) {
            UrlDecision.AllowInApp -> {
                val now = clock.nowEpochMs()
                if (inNewTab || tabs.isEmpty()) {
                    val newTab = BrowserTabRuntime(
                        id = UUID.randomUUID().toString(),
                        url = url,
                        title = BrowserIconResolver.resolve(url).contentDescription,
                        lastActiveAtMs = now,
                        spawnedFromLink = spawnedFromLink
                    )
                    val nextTabs = tabs + newTab
                    val nextActive = if (switchToTab || activeTabId.isBlank()) newTab.id else activeTabId
                    tabs = nextTabs
                    activeTabId = nextActive
                    if (tabsHydrated) persistTabsState(targetProfileId, nextTabs, nextActive)
                } else {
                    val targetTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
                    updateTab(targetTab.id) { it.copy(loadError = null, url = url) }
                    targetTab.controller?.loadUrl(url)
                }
            }
            is UrlDecision.ConfirmExternal -> externalDialogUrl = dec.url
            is UrlDecision.Block -> scope.launch { snackbarHostState.showSnackbar(dec.reason) }
        }
    }

    fun closeTab(tabId: String) {
        val targetProfileId = hydratedForProfileId ?: return
        if (tabs.size <= 1) {
            val replacement = BrowserTabRuntime(
                id = UUID.randomUUID().toString(),
                url = TornUrls.HOME,
                title = "Torn Home",
                lastActiveAtMs = clock.nowEpochMs()
            )
            tabs = listOf(replacement)
            activeTabId = replacement.id
            persistTabsState(targetProfileId, tabs, activeTabId)
            return
        }
        val idx = tabs.indexOfFirst { it.id == tabId }
        if (idx < 0) return
        val remaining = tabs.filterNot { it.id == tabId }
        val nextActive = if (activeTabId == tabId) {
            remaining.getOrElse(idx.coerceAtMost(remaining.lastIndex)) { remaining.first() }.id
        } else {
            activeTabId
        }
        tabs = remaining
        activeTabId = nextActive
        persistTabsState(targetProfileId, remaining, nextActive)
    }

    fun closeOtherTabs(keepTabId: String) {
        val targetProfileId = hydratedForProfileId ?: return
        val kept = tabs.firstOrNull { it.id == keepTabId } ?: return
        tabs = listOf(kept.copy(lastActiveAtMs = clock.nowEpochMs()))
        activeTabId = kept.id
        persistTabsState(targetProfileId, tabs, activeTabId)
    }

    LaunchedEffect(resolvedProfileId) {
        val profileId = resolvedProfileId ?: return@LaunchedEffect
        if (hydratedForProfileId == profileId && tabsHydrated) return@LaunchedEffect

        tabsHydrated = false
        tabs = emptyList()
        activeTabId = ""

        settingsStore.migrateLegacyProfileScopedPrefsIfNeeded(profileId)
        val initialPrefs = settingsStore.current()
        val now = clock.nowEpochMs()
        val restored = initialPrefs.browserTabsFor(profileId).map { p ->
            BrowserTabRuntime(id = p.id, url = p.url, title = p.title, lastActiveAtMs = now)
        }.ifEmpty {
            listOf(
                BrowserTabRuntime(
                    id = UUID.randomUUID().toString(),
                    url = TornUrls.HOME,
                    title = "Torn Home",
                    lastActiveAtMs = now
                )
            )
        }
        val resolvedActive = restored.firstOrNull { it.id == initialPrefs.activeBrowserTabIdFor(profileId) }?.id
            ?: restored.first().id
        tabs = restored
        activeTabId = resolvedActive
        hydratedForProfileId = profileId
        tabsHydrated = true

        val pending = browserNavigator.consumePendingRequest()
        val startupArg = if (!consumedInitialUrlArg && !initialUrlArg.isNullOrBlank()) {
            consumedInitialUrlArg = true
            initialUrlArg
        } else {
            null
        }
        val startupUrl = pending?.url ?: startupArg
        if (!startupUrl.isNullOrBlank()) {
            openUrlInTab(
                url = startupUrl,
                inNewTab = pending?.inNewTab ?: true,
                switchToTab = pending?.switchToTab ?: true,
                spawnedFromLink = true
            )
        } else {
            persistTabsState(profileId, restored, resolvedActive)
        }
    }

    LaunchedEffect(pendingRequest, tabsHydrated, hydratedForProfileId) {
        if (!tabsHydrated || hydratedForProfileId == null) return@LaunchedEffect
        val req = pendingRequest ?: return@LaunchedEffect
        browserNavigator.consumePendingRequest()
        openUrlInTab(
            url = req.url,
            inNewTab = req.inNewTab,
            switchToTab = req.switchToTab,
            spawnedFromLink = req.inNewTab
        )
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()
    val currentUrl = activeTab?.url ?: TornUrls.HOME
    val pageTitle = activeTab?.title ?: "Torn"
    val progress = activeTab?.progress ?: 100
    val activeController = activeTab?.controller

    BackHandler {
        if (activeController?.goBack() != true) {
            onBackPressed()
        }
    }

    val profileQuickLinks = remember(prefs, resolvedProfileId) {
        resolvedProfileId?.let { prefs.quickLinksFor(it) }.orEmpty()
    }
    val defaultLinks = remember(playerId) { QuickLinks.defaults(playerId) }
    val allLinks = remember(defaultLinks, profileQuickLinks) { defaultLinks + profileQuickLinks }
    val isChallengeActive = remember(currentUrl) {
        val lower = currentUrl.lowercase()
        lower.contains("challenges.cloudflare.com") || lower.contains("/cdn-cgi/")
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                BrowserTopAppBar(
                    pageTitle = pageTitle,
                    isChallengeActive = isChallengeActive,
                    sessionState = sessionState,
                    scriptsKillSwitch = prefs.scriptsKillSwitch,
                    resolvedAccountId = resolvedAccountId,
                    allLinks = allLinks,
                    defaultLinksCount = defaultLinks.size,
                    customLinks = profileQuickLinks,
                    showQuickLinks = showQuickLinks,
                    onShowQuickLinksChange = { showQuickLinks = it },
                    showScriptsMenu = showScriptsMenu,
                    onShowScriptsMenuChange = { showScriptsMenu = it },
                    onExitBrowser = onBackPressed,
                    onBack = { activeController?.goBack() },
                    onForward = { activeController?.goForward() },
                    onReload = {
                        val t = activeTab ?: return@BrowserTopAppBar
                        updateTab(t.id) { it.copy(loadError = null) }
                        val ctrl = t.controller
                        if (ctrl != null) {
                            if (ctrl.webView.url.isNullOrBlank()) ctrl.loadUrl(t.url) else ctrl.reload()
                        }
                    },
                    onGoHome = { openUrlInTab(TornUrls.HOME, inNewTab = false) },
                    onQuickLinkClick = { openUrlInTab(it.url, inNewTab = false) },
                    onQuickLinkLongClick = { link ->
                        openUrlInTab(link.url, inNewTab = true, switchToTab = true, spawnedFromLink = true)
                        scope.launch { snackbarHostState.showSnackbar("Opened ${link.label} in new tab") }
                    },
                    onDeleteCustomLink = { link ->
                        val pid = resolvedProfileId ?: return@BrowserTopAppBar
                        val custom = profileQuickLinks.filterNot { it == link }
                        scope.launch { settingsStore.setQuickLinks(pid, custom) }
                    },
                    onAddQuickLinkClick = { showAddQuickLink = true },
                    onToggleScriptsKillSwitch = { off ->
                        scope.launch { settingsStore.setScriptsKillSwitch(off) }
                    },
                    onOpenScripts = onOpenScripts,
                    onResetCookies = {
                        val pid = resolvedProfileId ?: return@BrowserTopAppBar
                        scope.launch {
                            webProfileManager.clearSessionCookies(pid)
                            sessionMonitor.reset()
                            activeTab?.let { updateTab(it.id) { cur -> cur.copy(loadError = null) } }
                            activeController?.loadUrl(TornUrls.HOME)
                            snackbarHostState.showSnackbar("Session cookies cleared for this profile.")
                        }
                    }
                )

                if (tabs.isNotEmpty()) {
                    BrowserTabStrip(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { selectTab(it) },
                        onNewTab = { openUrlInTab(TornUrls.HOME, inNewTab = true, switchToTab = true) },
                        onCloseTab = { closeTab(it) },
                        onCloseOtherTabs = { closeOtherTabs(it) },
                        onDuplicateTab = { id ->
                            tabs.firstOrNull { it.id == id }?.let { src ->
                                openUrlInTab(src.url, inNewTab = true, switchToTab = true, spawnedFromLink = true)
                            }
                        },
                        onReloadTab = { id ->
                            tabs.firstOrNull { it.id == id }?.let { target ->
                                updateTab(id) { it.copy(loadError = null) }
                                target.controller?.reload()
                            }
                        }
                    )
                }

                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = CrowColors.Sheen
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isChallengeActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrowColors.SurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = CrowColors.AccentD, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Complete the Cloudflare verification below to continue to Torn.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.TextMuted
                    )
                }
            }

            if (autoLoginHaltedReason != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrowColors.AccentD.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(autoLoginHaltedReason.orEmpty(), style = MaterialTheme.typography.labelSmall, color = CrowColors.AccentD)
                    TextButton(onClick = { autoLoginHaltedReason = null }) { Text("Dismiss") }
                }
            }

            if (TornUrls.isHospitalListUrl(currentUrl)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrowColors.SurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hospital page detected", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                    Button(
                        onClick = {
                            activeController?.evaluateJs(TornSelectors.buildHospitalCaptureJs()) { result ->
                                scope.launch {
                                    val count = targetsViewModel.captureHospitalRowsJson(result)
                                    val msg = if (count > 0) "Captured $count hospital targets." else HospitalPageParser.UNRECOGNIZED_MESSAGE
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        modifier = Modifier.testTag("capture_hospital_page_button")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("Capture this page")
                    }
                }
            }

            val err = webViewError
            when {
                resolvedProfileId == null || resolvedAccountId == null || !tabsHydrated -> LoadingView("Preparing browser session...")
                err != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        CrowCard {
                            Text("WebView Unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    browserNavigator.openExternal(context, "https://play.google.com/store/apps/details?id=com.google.android.webview")
                                }
                            ) { Text("Update Android System WebView") }
                        }
                    }
                }
                else -> {
                    val liveTabs = remember(tabs, activeTabId) {
                        tabs.sortedByDescending { if (it.id == activeTabId) Long.MAX_VALUE else it.lastActiveAtMs }
                            .take(MAX_LIVE_WEBVIEWS)
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        liveTabs.forEach { tab ->
                            val isCurrent = tab.id == activeTabId
                            key(resolvedProfileId, tab.id) {
                                CrowWebView(
                                    accountId = resolvedAccountId,
                                    profileId = resolvedProfileId,
                                    tabId = tab.id,
                                    isActiveTab = isCurrent,
                                    initialUrl = tab.url,
                                    preferInitialUrl = tab.savedBundle == null,
                                    allowedHosts = TornUrls.ALLOWED_WEB_HOSTS,
                                    allowedOriginRules = TornUrls.ALLOWED_ORIGIN_RULES,
                                    webProfileManager = webProfileManager,
                                    urlGuard = urlGuard,
                                    sessionMonitor = sessionMonitor,
                                    savedStateBundle = tab.savedBundle,
                                    onSaveStateBundle = { b -> updateTab(tab.id) { it.copy(savedBundle = b) } },
                                    onControllerReady = { ctrl -> updateTab(tab.id) { it.copy(controller = ctrl) } },
                                    onUrlChanged = { nextUrl -> updateTab(tab.id) { it.copy(url = nextUrl) } },
                                    onTitleChanged = { nextTitle -> updateTab(tab.id) { it.copy(title = nextTitle) } },
                                    onProgressChanged = { pct -> updateTab(tab.id) { it.copy(progress = pct) } },
                                    onPageLoadError = { loadErr -> updateTab(tab.id) { it.copy(loadError = loadErr) } },
                                    onExternalUrlRequested = { externalDialogUrl = it },
                                    onBlockedUrl = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                    onLinkLongPressed = { href -> longPressedLinkUrl = href },
                                    onOpenInNewTab = { href -> openUrlInTab(href, inNewTab = true, switchToTab = true, spawnedFromLink = true) },
                                    onPageStartedScripts = { view, url ->
                                        if (!url.contains("/cdn-cgi/", ignoreCase = true)) {
                                            userscriptEngine.injectForStage(view, resolvedAccountId, url, RunAt.DOCUMENT_START)
                                        }
                                    },
                                    onPageFinishedScripts = { view, url ->
                                        if (url.contains("/cdn-cgi/", ignoreCase = true)) return@CrowWebView
                                        userscriptEngine.injectForStage(view, resolvedAccountId, url, RunAt.DOCUMENT_END)
                                        userscriptEngine.injectForStage(view, resolvedAccountId, url, RunAt.DOCUMENT_IDLE)
                                        if ((TornUrls.isLoginUrl(url) || TornUrls.isHomeUrl(url)) && prefs.autoLoginEnabled) {
                                            scope.launch {
                                                val now = clock.nowEpochMs()
                                                loginAttempts.removeAll { now - it > 600_000L }
                                                if (loginAttempts.size >= 2) {
                                                    autoLoginHaltedReason = "Manual login required (auto-login limit reached)."
                                                    return@launch
                                                }
                                                val user = secretVault.get(SecretVault.webUserAlias(resolvedAccountId))
                                                val pass = secretVault.get(SecretVault.webPassAlias(resolvedAccountId))
                                                if (!user.isNullOrBlank() && !pass.isNullOrEmpty()) {
                                                    val js = TornSelectors.buildSafeLoginFillJs(user, pass, prefs.autoLoginAutoSubmit)
                                                    view.evaluateJavascript(js) { rawStatus ->
                                                        when {
                                                            rawStatus?.contains("FILLED") == true -> loginAttempts.add(clock.nowEpochMs())
                                                            rawStatus?.contains("BLOCKED_CHALLENGE") == true -> {
                                                                autoLoginHaltedReason = "Manual login required (captcha/2FA detected)."
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onWebMessage = { view, payload -> gmBridge.handleWebMessage(view, resolvedAccountId, payload) },
                                    documentStartScript = userscriptEngine.buildBootstrapBridgeJs(),
                                    onWebViewUnavailable = { t -> webViewError = t.message ?: "Android System WebView is missing or outdated." },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        val activeLoadErr = activeTab?.loadError
                        if (activeLoadErr != null && activeTab != null) {
                            BrowserLoadErrorOverlay(
                                error = activeLoadErr,
                                onRetry = {
                                    val target = activeLoadErr.url.ifBlank { TornUrls.HOME }
                                    updateTab(activeTab.id) { it.copy(loadError = null) }
                                    activeTab.controller?.loadUrl(target)
                                },
                                onResetAndRetry = {
                                    val pid = resolvedProfileId ?: return@BrowserLoadErrorOverlay
                                    scope.launch {
                                        webProfileManager.clearSessionCookies(pid)
                                        updateTab(activeTab.id) { it.copy(loadError = null) }
                                        activeTab.controller?.loadUrl(TornUrls.HOME)
                                    }
                                },
                                onOpenExternal = {
                                    browserNavigator.openExternal(context, activeLoadErr.url.ifBlank { TornUrls.HOME })
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    longPressedLinkUrl?.let { href ->
        val decision = remember(href) { urlGuard.evaluate(href, isMainFrame = true) }
        LinkLongPressDialog(
            url = href,
            decision = decision,
            onDismiss = { longPressedLinkUrl = null },
            onOpenNewTab = {
                longPressedLinkUrl = null
                openUrlInTab(href, inNewTab = true, switchToTab = true, spawnedFromLink = true)
            },
            onOpenBackgroundTab = {
                longPressedLinkUrl = null
                openUrlInTab(href, inNewTab = true, switchToTab = false, spawnedFromLink = true)
                scope.launch { snackbarHostState.showSnackbar("Opened in background tab") }
            },
            onOpenCurrentTab = {
                longPressedLinkUrl = null
                openUrlInTab(href, inNewTab = false)
            },
            onOpenExternal = {
                longPressedLinkUrl = null
                browserNavigator.openExternal(context, href)
            },
            onCopyLink = {
                longPressedLinkUrl = null
                clipboard.setText(AnnotatedString(href))
                scope.launch { snackbarHostState.showSnackbar("Link copied") }
            }
        )
    }

    externalDialogUrl?.let { targetUrl ->
        ExternalConfirmDialog(
            url = targetUrl,
            onDismiss = { externalDialogUrl = null },
            onConfirmExternal = {
                browserNavigator.openExternal(context, targetUrl)
                externalDialogUrl = null
            }
        )
    }

    if (showAddQuickLink) {
        AddQuickLinkDialog(
            currentUrl = currentUrl,
            initialTitle = pageTitle,
            onDismiss = { showAddQuickLink = false },
            onSave = { label ->
                val pid = resolvedProfileId
                if (pid != null && urlGuard.evaluate(currentUrl, isMainFrame = true) is UrlDecision.AllowInApp && label.isNotBlank()) {
                    val updated = profileQuickLinks + QuickLinkItem(label, currentUrl)
                    scope.launch { settingsStore.setQuickLinks(pid, updated) }
                }
                showAddQuickLink = false
            }
        )
    }
}
