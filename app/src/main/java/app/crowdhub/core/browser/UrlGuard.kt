package app.crowdhub.core.browser

import android.net.Uri

sealed class UrlDecision {
    data object AllowInApp : UrlDecision()
    data class ConfirmExternal(val url: String) : UrlDecision()
    data class Block(val reason: String) : UrlDecision()
}

class UrlGuard(private val allowedHosts: Set<String>) {
    fun evaluate(rawUrl: String?): UrlDecision {
        if (rawUrl.isNullOrBlank()) return UrlDecision.Block("Empty URL")
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull()
            ?: return UrlDecision.Block("Malformed URL")

        val scheme = uri.scheme?.lowercase() ?: return UrlDecision.Block("Missing scheme")
        if (scheme == "intent" || scheme == "javascript" || scheme == "file" || scheme == "content" || scheme == "data") {
            return UrlDecision.Block("Scheme '$scheme' is blocked")
        }
        if (scheme != "https") {
            if (scheme == "http") {
                return UrlDecision.Block("Cleartext HTTP is blocked")
            }
            return UrlDecision.Block("Unsupported scheme '$scheme'")
        }

        val host = uri.host?.lowercase() ?: return UrlDecision.Block("Missing host")
        return if (allowedHosts.any { it.equals(host, ignoreCase = true) }) {
            UrlDecision.AllowInApp
        } else {
            UrlDecision.ConfirmExternal(rawUrl)
        }
    }
}
