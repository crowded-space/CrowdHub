package app.crowdhub.core.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import app.crowdhub.core.network.CrowLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebProfileManager {
    fun isMultiProfileSupported(): Boolean {
        return runCatching {
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        }.getOrDefault(false)
    }

    @SuppressLint("RequiresFeature")
    fun attach(webView: WebView, accountId: String) {
        if (!isMultiProfileSupported()) return
        runCatching {
            val profileName = profileName(accountId)
            ProfileStore.getInstance().getOrCreateProfile(profileName)
            WebViewCompat.setProfile(webView, profileName)
        }.onFailure {
            CrowLog.w(message = "Failed to attach WebView profile; using default", throwable = it)
        }
    }

    fun flushCookies() {
        runCatching {
            CookieManager.getInstance().flush()
        }
    }

    @SuppressLint("RequiresFeature")
    suspend fun wipe(accountId: String) = withContext(Dispatchers.Main) {
        if (isMultiProfileSupported()) {
            runCatching {
                ProfileStore.getInstance().deleteProfile(profileName(accountId))
            }.onFailure {
                CrowLog.w(message = "Failed to delete WebView profile", throwable = it)
            }
        } else {
            runCatching {
                val cm = CookieManager.getInstance()
                cm.removeAllCookies(null)
                cm.flush()
            }
        }
    }

    private fun profileName(accountId: String): String {
        val safe = accountId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "acct_$safe"
    }
}
