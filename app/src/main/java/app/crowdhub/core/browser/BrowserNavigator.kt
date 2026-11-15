package app.crowdhub.core.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserOpenRequest(
    val url: String,
    val inNewTab: Boolean = true,
    val switchToTab: Boolean = true
)

class BrowserNavigator {
    private val _pendingRequest = MutableStateFlow<BrowserOpenRequest?>(null)
    val pendingRequest: StateFlow<BrowserOpenRequest?> = _pendingRequest.asStateFlow()

    fun requestInAppOpen(
        url: String,
        inNewTab: Boolean = true,
        switchToTab: Boolean = true
    ) {
        _pendingRequest.value = BrowserOpenRequest(
            url = url,
            inNewTab = inNewTab,
            switchToTab = switchToTab
        )
    }

    fun consumePendingRequest(): BrowserOpenRequest? {
        val current = _pendingRequest.value
        _pendingRequest.value = null
        return current
    }

    fun consumePendingUrl(): String? = consumePendingRequest()?.url

    fun openExternal(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
