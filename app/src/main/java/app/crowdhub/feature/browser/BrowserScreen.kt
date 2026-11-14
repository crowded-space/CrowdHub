package app.crowdhub.feature.browser

import android.os.Bundle
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.browser.CrowWebView
import app.crowdhub.core.browser.SessionMonitor
import app.crowdhub.core.browser.UrlDecision
import app.crowdhub.core.browser.UrlGuard
import app.crowdhub.core.browser.WebLoadError
import app.crowdhub.core.browser.WebProfileManager
import app.crowdhub.core.browser.WebSessionState
import app.crowdhub.core.browser.WebViewController
import app.crowdhub.core.network.Clock
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.scripts.GmBridge
import app.crowdhub.core.scripts.RunAt
import app.crowdhub.core.scripts.UserscriptEngine
import app.crowdhub.core.settings.AppPreferences
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeProfile by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        initialValue = null as ActiveProfileState?
    )
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    val sessionState by sessionMonitor.state.collectAsStateWithLifecycle()
    val pendingUrl by browserNavigator.pendingInAppUrl.collectAsStateWithLifecycle()

    val resolvedAccountId = activeProfile?.let { it.primaryAccount?.id ?: "default" }

    var controller by remember { mutableStateOf<WebViewController?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(initialUrlArg ?: TornUrls.HOME) }
    var pageTitle by remember { mutableStateOf("Torn") }
    var progress by remember { mutableIntStateOf(100) }
    var savedBundle by rememberSaveable(resolvedAccountId) { mutableStateOf<Bundle?>(null) }

    var externalDialogUrl by remember { mutableStateOf<String?>(null) }
    var showQuickLinks by remember { mutableStateOf(false) }
    var showAddQuickLink by remember { mutableStateOf(false) }
    var showScriptsMenu by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var pageLoadError by remember { mutableStateOf<WebLoadError?>(null) }

    // Auto-login rate guard: max 2 attempts per 10 minutes
    val loginAttempts = remember { mutableStateListOf<Long>() }
    var autoLoginHaltedReason by remember { mutableStateOf<String?>(null) }

    val playerId = activeProfile?.primaryAccount?.externalId

    LaunchedEffect(pendingUrl, controller) {
        val target = pendingUrl
        if (target != null && controller != null) {
            browserNavigator.consumePendingUrl()
            when (val dec = urlGuard.evaluate(target, isMainFrame = true)) {
                UrlDecision.AllowInApp -> {
                    pageLoadError = null
                    controller?.loadUrl(target)
                }
                is UrlDecision.ConfirmExternal -> externalDialogUrl = dec.url
                is UrlDecision.Block -> scope.launch { snackbarHostState.showSnackbar(dec.reason) }
            }
        }
    }

    BackHandler {
        val handled = controller?.goBack() == true
        if (!handled) {
            onBackPressed()
        }
    }

    val defaultLinks = remember(playerId) { QuickLinks.defaults(playerId) }
    val allLinks = remember(defaultLinks, prefs.quickLinks) { defaultLinks + prefs.quickLinks }
    val isChallengeActive = remember(currentUrl) {
        val lower = currentUrl.lowercase()
        lower.contains("challenges.cloudflare.com") || lower.contains("/cdn-cgi/")
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            val sessionText = when {
                                isChallengeActive -> "Verifying security check..."
                                sessionState == WebSessionState.LOGGED_IN -> "Logged in"
                                sessionState == WebSessionState.LOGGED_OUT -> "Logged out"
                                else -> "Unknown"
                            }
                            val sessionColor = when {
                                isChallengeActive -> CrowColors.AccentD
                                sessionState == WebSessionState.LOGGED_IN -> CrowColors.Ok
                                sessionState == WebSessionState.LOGGED_OUT -> CrowColors.Danger
                                else -> CrowColors.TextMuted
                            }
                            Text(
                                text = sessionText,
                                style = MaterialTheme.typography.labelSmall,
                                color = sessionColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                    actions = {
                        IconButton(onClick = { controller?.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(onClick = { controller?.goForward() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = {
                            pageLoadError = null
                            val ctrl = controller
                            if (ctrl != null) {
                                if (ctrl.webView.url.isNullOrBlank()) {
                                    ctrl.loadUrl(currentUrl.ifBlank { TornUrls.HOME })
                                } else {
                                    ctrl.reload()
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = {
                            pageLoadError = null
                            controller?.loadUrl(TornUrls.HOME)
                        }) {
                            Icon(Icons.Outlined.Home, contentDescription = "Torn Home")
                        }

                        Box {
                            IconButton(onClick = { showQuickLinks = true }) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Quick links")
                            }
                            DropdownMenu(
                                expanded = showQuickLinks,
                                onDismissRequest = { showQuickLinks = false }
                            ) {
                                allLinks.forEachIndexed { idx, link ->
                                    DropdownMenuItem(
                                        text = { Text(link.label) },
                                        trailingIcon = if (idx >= defaultLinks.size) {
                                            {
                                                IconButton(
                                                    onClick = {
                                                        val custom = prefs.quickLinks.filterNot { it == link }
                                                        scope.launch { settingsStore.setQuickLinks(custom) }
                                                    }
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove link")
                                                }
                                            }
                                        } else null,
                                        onClick = {
                                            showQuickLinks = false
                                            pageLoadError = null
                                            controller?.loadUrl(link.url)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ Add current page") },
                                    onClick = {
                                        showQuickLinks = false
                                        showAddQuickLink = true
                                    }
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { showScriptsMenu = true }) {
                                Icon(
                                    Icons.Outlined.Code,
                                    contentDescription = "Scripts menu",
                                    tint = if (prefs.scriptsKillSwitch) CrowColors.Danger else CrowColors.Text
                                )
                            }
                            DropdownMenu(
                                expanded = showScriptsMenu,
                                onDismissRequest = { showScriptsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Disable all scripts")
                                            Switch(
                                                checked = prefs.scriptsKillSwitch,
                                                onCheckedChange = { off ->
                                                    scope.launch { settingsStore.setScriptsKillSwitch(off) }
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setScriptsKillSwitch(!prefs.scriptsKillSwitch)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage Scripts") },
                                    onClick = {
                                        showScriptsMenu = false
                                        onOpenScripts()
                                    }
                                )
                                if (resolvedAccountId != null) {
                                    DropdownMenuItem(
                                        text = { Text("Reset web session cookies") },
                                        onClick = {
                                            showScriptsMenu = false
                                            scope.launch {
                                                webProfileManager.clearSessionCookies(resolvedAccountId)
                                                sessionMonitor.reset()
                                                pageLoadError = null
                                                controller?.loadUrl(TornUrls.HOME)
                                                snackbarHostState.showSnackbar("Session cookies cleared.")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
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
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = CrowColors.AccentD,
                        modifier = Modifier.size(16.dp)
                    )
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
                    Text(
                        text = autoLoginHaltedReason.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.AccentD
                    )
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
                    Text(
                        text = "Hospital page detected",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrowColors.TextMuted
                    )
                    Button(
                        onClick = {
                            controller?.evaluateJs(TornSelectors.buildHospitalCaptureJs()) { result ->
                                scope.launch {
                                    val count = targetsViewModel.captureHospitalRowsJson(result)
                                    val msg = if (count > 0) {
                                        "Captured $count hospital targets."
                                    } else {
                                        HospitalPageParser.UNRECOGNIZED_MESSAGE
                                    }
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
            val loadErr = pageLoadError
            when {
                resolvedAccountId == null -> {
                    LoadingView("Preparing browser session...")
                }
                err != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CrowCard {
                            Text("WebView Unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    browserNavigator.openExternal(
                                        context,
                                        "https://play.google.com/store/apps/details?id=com.google.android.webview"
                                    )
                                }
                            ) {
                                Text("Update Android System WebView")
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CrowWebView(
                            accountId = resolvedAccountId,
                            initialUrl = initialUrlArg ?: TornUrls.HOME,
                            preferInitialUrl = initialUrlArg != null,
                            allowedHosts = TornUrls.ALLOWED_WEB_HOSTS,
                            allowedOriginRules = TornUrls.ALLOWED_ORIGIN_RULES,
                            webProfileManager = webProfileManager,
                            urlGuard = urlGuard,
                            sessionMonitor = sessionMonitor,
                            savedStateBundle = savedBundle,
                            onSaveStateBundle = { savedBundle = it },
                            onControllerReady = { controller = it },
                            onUrlChanged = { currentUrl = it },
                            onTitleChanged = { pageTitle = it },
                            onProgressChanged = { progress = it },
                            onPageLoadError = { pageLoadError = it },
                            onExternalUrlRequested = { externalDialogUrl = it },
                            onBlockedUrl = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
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
                                            val js = TornSelectors.buildSafeLoginFillJs(
                                                username = user,
                                                password = pass,
                                                autoSubmit = prefs.autoLoginAutoSubmit
                                            )
                                            view.evaluateJavascript(js) { rawStatus ->
                                                when {
                                                    rawStatus?.contains("FILLED") == true -> {
                                                        loginAttempts.add(clock.nowEpochMs())
                                                    }
                                                    rawStatus?.contains("BLOCKED_CHALLENGE") == true -> {
                                                        autoLoginHaltedReason = "Manual login required (captcha/2FA detected)."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onWebMessage = { view, payload ->
                                gmBridge.handleWebMessage(view, resolvedAccountId, payload)
                            },
                            documentStartScript = userscriptEngine.buildBootstrapBridgeJs(),
                            onWebViewUnavailable = { t ->
                                webViewError = t.message ?: "Android System WebView is missing or outdated."
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (loadErr != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CrowColors.CrowBlack.copy(alpha = 0.92f))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CrowCard(borderColor = CrowColors.Danger.copy(alpha = 0.5f)) {
                                    Text(
                                        text = "Page Failed to Load",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CrowColors.Text
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = loadErr.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrowColors.Danger
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = loadErr.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrowColors.TextMuted,
                                        maxLines = 2
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val target = loadErr.url.ifBlank { TornUrls.HOME }
                                                pageLoadError = null
                                                controller?.loadUrl(target)
                                            },
                                            modifier = Modifier.testTag("browser_retry_button")
                                        ) {
                                            Text("Retry")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    webProfileManager.clearSessionCookies(resolvedAccountId)
                                                    pageLoadError = null
                                                    controller?.loadUrl(TornUrls.HOME)
                                                }
                                            }
                                        ) {
                                            Text("Reset & Retry")
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(
                                        onClick = {
                                            browserNavigator.openExternal(context, loadErr.url.ifBlank { TornUrls.HOME })
                                        }
                                    ) {
                                        Text("Open in external browser")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    externalDialogUrl?.let { targetUrl ->
        AlertDialog(
            onDismissRequest = { externalDialogUrl = null },
            title = { Text("Open in your browser?") },
            text = { Text("External host is not allowed inside Crowd Hub:\n$targetUrl") },
            confirmButton = {
                Button(
                    onClick = {
                        browserNavigator.openExternal(context, targetUrl)
                        externalDialogUrl = null
                    }
                ) { Text("Open externally") }
            },
            dismissButton = {
                TextButton(onClick = { externalDialogUrl = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddQuickLink) {
        var label by remember { mutableStateOf(pageTitle.take(20)) }
        AlertDialog(
            onDismissRequest = { showAddQuickLink = false },
            title = { Text("Add Quick Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        singleLine = true
                    )
                    Text(currentUrl, style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlGuard.evaluate(currentUrl, isMainFrame = true) is UrlDecision.AllowInApp && label.isNotBlank()) {
                            val updated = prefs.quickLinks + QuickLinkItem(label.trim(), currentUrl)
                            scope.launch { settingsStore.setQuickLinks(updated) }
                        }
                        showAddQuickLink = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddQuickLink = false }) { Text("Cancel") }
            }
        )
    }
}
