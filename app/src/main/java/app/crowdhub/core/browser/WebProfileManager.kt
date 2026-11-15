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
    private var lastDefaultProfileId: String? = null

    fun isMultiProfileSupported(): Boolean {
        return runCatching {
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        }.getOrDefault(false)
    }

    @SuppressLint("RequiresFeature")
    fun attach(webView: WebView, profileId: String) {
        val resolvedId = profileId.ifBlank { "default" }
        val cookieManager = if (isMultiProfileSupported()) {
            runCatching {
                val profileName = profileName(resolvedId)
                val profile = ProfileStore.getInstance().getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profileName)
                attachedProfiles[resolvedId] = profile
                profile.cookieManager
            }.onFailure {
                CrowLog.w(message = "Failed to attach WebView profile; using default", throwable = it)
            }.getOrNull() ?: CookieManager.getInstance()
        } else {
            val previous = lastDefaultProfileId
            if (previous != null && previous != resolvedId) {
                runCatching {
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                }
            }
            lastDefaultProfileId = resolvedId
            CookieManager.getInstance()
        }

        runCatching {
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }.onFailure {
            CrowLog.w(message = "Failed to configure WebView cookies", throwable = it)
        }
    }

    fun flushCookies(profileId: String? = null) {
        runCatching {
            if (profileId != null) {
                attachedProfiles[profileId]?.cookieManager?.flush()
            } else {
                attachedProfiles.values.forEach { it.cookieManager.flush() }
            }
            CookieManager.getInstance().flush()
        }
    }

    suspend fun clearSessionCookies(profileId: String) = withContext(Dispatchers.Main) {
        runCatching {
            val profileCm = attachedProfiles[profileId]?.cookieManager
            if (profileCm != null) {
                profileCm.removeSessionCookies(null)
                profileCm.flush()
            } else if (!isMultiProfileSupported()) {
                val defaultCm = CookieManager.getInstance()
                defaultCm.removeSessionCookies(null)
                defaultCm.flush()
            }
        }
    }

    @SuppressLint("RequiresFeature")
    suspend fun wipe(profileId: String) = withContext(Dispatchers.Main) {
        attachedProfiles.remove(profileId)
        if (isMultiProfileSupported()) {
            runCatching {
                ProfileStore.getInstance().deleteProfile(profileName(profileId))
            }.onFailure {
                CrowLog.w(message = "Failed to delete WebView profile", throwable = it)
            }
        } else if (lastDefaultProfileId == profileId) {
            runCatching {
                val cm = CookieManager.getInstance()
                cm.removeAllCookies(null)
                cm.flush()
            }
            lastDefaultProfileId = null
        }
    }

    private fun profileName(profileId: String): String {
        val safe = profileId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "prof_$safe"
    }
}
