package app.crowdhub.core.browser

import android.net.Uri

sealed class UrlDecision {
    data object AllowInApp : UrlDecision()
    data class ConfirmExternal(val url: String) : UrlDecision()
    data class Block(val reason: String) : UrlDecision()
}

class UrlGuard(
    private val allowedHosts: Set<String>,
    private val challengeHosts: Set<String> = emptySet()
) {
    fun evaluate(rawUrl: String?, isMainFrame: Boolean = true): UrlDecision {
        if (rawUrl.isNullOrBlank()) return UrlDecision.Block("Empty URL")
        val trimmed = rawUrl.trim()
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
            ?: return UrlDecision.Block("Malformed URL")

        val scheme = uri.scheme?.lowercase() ?: return UrlDecision.Block("Missing scheme")
        if (scheme == "intent" || scheme == "javascript" || scheme == "file" || scheme == "content") {
            return UrlDecision.Block("Scheme '$scheme' is blocked")
        }
        if (!isMainFrame && (scheme == "about" || scheme == "blob" || scheme == "data")) {
            return UrlDecision.AllowInApp
        }
        if (scheme == "data") {
            return UrlDecision.Block("Scheme 'data' is blocked")
        }
        if (scheme != "https") {
            if (scheme == "http") {
                return UrlDecision.Block("Cleartext HTTP is blocked")
            }
            return UrlDecision.Block("Unsupported scheme '$scheme'")
        }

        val host = uri.host?.lowercase()?.trimEnd('.') ?: return UrlDecision.Block("Missing host")

        if (!isMainFrame) {
            return UrlDecision.AllowInApp
        }

        val allowed = matchesDomainSet(host, allowedHosts) || matchesDomainSet(host, challengeHosts)
        return if (allowed) {
            UrlDecision.AllowInApp
        } else {
            UrlDecision.ConfirmExternal(trimmed)
        }
    }

    private fun matchesDomainSet(host: String, domainSet: Set<String>): Boolean {
        return domainSet.any { rawPattern ->
            val pattern = rawPattern.lowercase().trimStart('.')
            val baseDomain = pattern.removePrefix("www.")
            host == pattern || host == baseDomain || host.endsWith(".$baseDomain")
        }
    }
}
