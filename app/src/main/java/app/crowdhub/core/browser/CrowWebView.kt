package app.crowdhub.core.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

data class WebLoadError(
    val url: String,
    val message: String
)

data class WebViewController(
    val webView: WebView
) {
    fun loadUrl(url: String) = webView.loadUrl(url)
    fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }
    fun goForward() {
        if (webView.canGoForward()) webView.goForward()
    }
    fun reload() = webView.reload()
    fun evaluateJs(script: String, onResult: ((String?) -> Unit)? = null) {
        webView.evaluateJavascript(script) { res -> onResult?.invoke(res) }
    }
}

internal fun sanitizeUserAgent(rawUa: String): String {
    if (rawUa.isBlank()) return rawUa
    return rawUa
        .replace("; wv)", ")")
        .replace("; wv;", ";")
        .replace(Regex("\\bVersion/\\d+\\.\\d+\\s*"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

private fun isChallengeUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val lower = url.lowercase()
    return lower.contains("challenges.cloudflare.com") || lower.contains("/cdn-cgi/")
}

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
@Composable
fun CrowWebView(
    accountId: String,
    initialUrl: String,
    preferInitialUrl: Boolean = false,
    allowedHosts: Set<String>,
    allowedOriginRules: Set<String>,
    webProfileManager: WebProfileManager,
    urlGuard: UrlGuard,
    sessionMonitor: SessionMonitor,
    savedStateBundle: Bundle?,
    onSaveStateBundle: (Bundle) -> Unit,
    onControllerReady: (WebViewController) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageLoadError: (WebLoadError?) -> Unit,
    onExternalUrlRequested: (String) -> Unit,
    onBlockedUrl: (String) -> Unit,
    onPageStartedScripts: (WebView, String) -> Unit,
    onPageFinishedScripts: (WebView, String) -> Unit,
    onWebMessage: (WebView, String) -> Unit,
    documentStartScript: String?,
    onWebViewUnavailable: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val webView = remember(accountId) {
        runCatching {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webProfileManager.attach(this, accountId)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = sanitizeUserAgent(userAgentString)
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setSupportMultipleWindows(false)
                    setGeolocationEnabled(false)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                setDownloadListener { _, _, _, _, _ ->
                    onBlockedUrl("File downloads are disabled in Crowd Hub")
                }

                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    runCatching {
                        WebViewCompat.addWebMessageListener(
                            this,
                            "__crowd",
                            allowedOriginRules
                        ) { view, message, sourceOrigin, _, _ ->
                            val host = sourceOrigin.host?.lowercase()
                            if (host != null && allowedHosts.any { it.equals(host, ignoreCase = true) }) {
                                val payload = message.data ?: return@addWebMessageListener
                                onWebMessage(view, payload)
                            }
                        }
                    }
                }

                if (!documentStartScript.isNullOrBlank() &&
                    WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
                ) {
                    runCatching {
                        WebViewCompat.addDocumentStartJavaScript(
                            this,
                            documentStartScript,
                            allowedOriginRules
                        )
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (!title.isNullOrBlank()) {
                            onTitleChanged(title)
                        }
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val target = request?.url?.toString()
                        val isMainFrame = request?.isForMainFrame ?: true
                        return when (val decision = urlGuard.evaluate(target, isMainFrame = isMainFrame)) {
                            UrlDecision.AllowInApp -> false
                            is UrlDecision.ConfirmExternal -> {
                                if (isMainFrame) {
                                    onExternalUrlRequested(decision.url)
                                    true
                                } else {
                                    false
                                }
                            }
                            is UrlDecision.Block -> {
                                if (isMainFrame) {
                                    onBlockedUrl(decision.reason)
                                }
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (view != null && !url.isNullOrBlank()) {
                            onPageLoadError(null)
                            onUrlChanged(url)
                            if (!isChallengeUrl(url)) {
                                sessionMonitor.onPageVisited(url)
                            }
                            onPageStartedScripts(view, url)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view != null && !url.isNullOrBlank()) {
                            onUrlChanged(url)
                            if (!isChallengeUrl(url)) {
                                sessionMonitor.onPageVisited(url)
                            }
                            webProfileManager.flushCookies(accountId)
                            onPageFinishedScripts(view, url)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val failingUrl = request.url?.toString() ?: initialUrl
                            val desc = error?.description?.toString()?.takeIf { it.isNotBlank() }
                                ?: "Failed to load page"
                            val code = error?.errorCode
                            val msg = if (code != null) "$desc (code $code)" else desc
                            onPageLoadError(WebLoadError(url = failingUrl, message = msg))
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.cancel()
                        val failingUrl = error?.url ?: initialUrl
                        onPageLoadError(
                            WebLoadError(
                                url = failingUrl,
                                message = "TLS/SSL certificate error (code ${error?.primaryError ?: -1})."
                            )
                        )
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        onPageLoadError(
                            WebLoadError(
                                url = view?.url ?: initialUrl,
                                message = "Browser renderer stopped unexpectedly. Tap Retry to reload."
                            )
                        )
                        return true
                    }
                }

                val restoredList = if (!preferInitialUrl && savedStateBundle != null && !savedStateBundle.isEmpty) {
                    restoreState(savedStateBundle)
                } else {
                    null
                }
                if (restoredList == null || restoredList.size == 0) {
                    when (val decision = urlGuard.evaluate(initialUrl, isMainFrame = true)) {
                        UrlDecision.AllowInApp -> loadUrl(initialUrl)
                        is UrlDecision.ConfirmExternal -> onExternalUrlRequested(decision.url)
                        is UrlDecision.Block -> onBlockedUrl(decision.reason)
                    }
                }
            }
        }.onFailure { onWebViewUnavailable(it) }.getOrNull()
    }

    if (webView != null) {
        DisposableEffect(lifecycleOwner, webView) {
            onControllerReady(WebViewController(webView))
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        webProfileManager.flushCookies(accountId)
                        val b = Bundle()
                        webView.saveState(b)
                        onSaveStateBundle(b)
                        webView.onPause()
                    }
                    Lifecycle.Event.ON_RESUME -> webView.onResume()
                    Lifecycle.Event.ON_STOP -> webProfileManager.flushCookies(accountId)
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                val b = Bundle()
                webView.saveState(b)
                onSaveStateBundle(b)
                webProfileManager.flushCookies(accountId)
                webView.destroy()
            }
        }

        AndroidView(
            factory = { webView },
            modifier = modifier
        )
    }
}
