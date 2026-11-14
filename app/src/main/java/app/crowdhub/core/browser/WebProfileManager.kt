package app.crowdhub.core.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import app.crowdhub.core.network.CrowLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebProfileManager {
    private val attachedProfiles = mutableMapOf<String, Profile>()

    fun isMultiProfileSupported(): Boolean {
        return runCatching {
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        }.getOrDefault(false)
    }

    @SuppressLint("RequiresFeature")
    fun attach(webView: WebView, accountId: String) {
        val cookieManager = if (isMultiProfileSupported()) {
            runCatching {
                val profileName = profileName(accountId)
                val profile = ProfileStore.getInstance().getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profileName)
                attachedProfiles[accountId] = profile
                profile.cookieManager
            }.onFailure {
                CrowLog.w(message = "Failed to attach WebView profile; using default", throwable = it)
            }.getOrNull() ?: CookieManager.getInstance()
        } else {
            CookieManager.getInstance()
        }

        runCatching {
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }.onFailure {
            CrowLog.w(message = "Failed to configure WebView cookies", throwable = it)
        }
    }

    fun flushCookies(accountId: String? = null) {
        runCatching {
            if (accountId != null) {
                attachedProfiles[accountId]?.cookieManager?.flush()
            } else {
                attachedProfiles.values.forEach { it.cookieManager.flush() }
            }
            CookieManager.getInstance().flush()
        }
    }

    suspend fun clearSessionCookies(accountId: String) = withContext(Dispatchers.Main) {
        runCatching {
            attachedProfiles[accountId]?.cookieManager?.let { cm ->
                cm.removeSessionCookies(null)
                cm.flush()
            }
            val defaultCm = CookieManager.getInstance()
            defaultCm.removeSessionCookies(null)
            defaultCm.flush()
        }
    }

    @SuppressLint("RequiresFeature")
    suspend fun wipe(accountId: String) = withContext(Dispatchers.Main) {
        attachedProfiles.remove(accountId)
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
