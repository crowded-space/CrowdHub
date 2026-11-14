package app.crowdhub.core.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
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

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
@Composable
fun CrowWebView(
    accountId: String,
    initialUrl: String,
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
                webProfileManager.attach(this, accountId)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setSupportMultipleWindows(false)
                    setGeolocationEnabled(false)
                    databaseEnabled = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                setDownloadListener(DownloadListener { _, _, _, _, _ ->
                    onBlockedUrl("File downloads are disabled in Crowd Hub")
                })

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
                        return when (val decision = urlGuard.evaluate(target)) {
                            UrlDecision.AllowInApp -> false
                            is UrlDecision.ConfirmExternal -> {
                                onExternalUrlRequested(decision.url)
                                true
                            }
                            is UrlDecision.Block -> {
                                onBlockedUrl(decision.reason)
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (view != null && !url.isNullOrBlank()) {
                            onUrlChanged(url)
                            sessionMonitor.onPageVisited(url)
                            onPageStartedScripts(view, url)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view != null && !url.isNullOrBlank()) {
                            onUrlChanged(url)
                            sessionMonitor.onPageVisited(url)
                            webProfileManager.flushCookies()
                            onPageFinishedScripts(view, url)
                        }
                    }
                }

                if (savedStateBundle != null && !savedStateBundle.isEmpty) {
                    restoreState(savedStateBundle)
                } else {
                    when (val decision = urlGuard.evaluate(initialUrl)) {
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
                        webProfileManager.flushCookies()
                        val b = Bundle()
                        webView.saveState(b)
                        onSaveStateBundle(b)
                        webView.onPause()
                    }
                    Lifecycle.Event.ON_RESUME -> webView.onResume()
                    Lifecycle.Event.ON_STOP -> webProfileManager.flushCookies()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                val b = Bundle()
                webView.saveState(b)
                onSaveStateBundle(b)
                webProfileManager.flushCookies()
                webView.destroy()
            }
        }

        AndroidView(
            factory = { webView },
            modifier = modifier
        )
    }
}
