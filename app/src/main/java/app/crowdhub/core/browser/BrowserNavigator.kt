package app.crowdhub.core.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserNavigator {
    private val _pendingInAppUrl = MutableStateFlow<String?>(null)
    val pendingInAppUrl: StateFlow<String?> = _pendingInAppUrl.asStateFlow()

    fun requestInAppOpen(url: String) {
        _pendingInAppUrl.value = url
    }

    fun consumePendingUrl(): String? {
        val current = _pendingInAppUrl.value
        _pendingInAppUrl.value = null
        return current
    }

    fun openExternal(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
