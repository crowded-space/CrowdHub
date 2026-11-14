package app.crowdhub.provider.torn

import android.net.Uri

object TornUrls {
    const val BASE_WEB = "https://www.torn.com"
    const val HOME = "https://www.torn.com/index.php"
    const val HOSPITAL_LIST = "https://www.torn.com/hospitalview.php"
    const val API_DOCS = "https://www.torn.com/api.html"
    // VERIFY: Torn API key creation tab anchor
    const val API_KEY_PREFS = "https://www.torn.com/preferences.php#tab=api"

    val ALLOWED_WEB_HOSTS = setOf("www.torn.com", "torn.com")
    val ALLOWED_ORIGIN_RULES = setOf("https://www.torn.com", "https://torn.com")
    val ALLOWED_CHALLENGE_HOSTS = setOf(
        "challenges.cloudflare.com",
        "hcaptcha.com",
        "newassets.hcaptcha.com",
        "recaptcha.net",
        "www.recaptcha.net"
    )

    fun isAllowedWebHost(host: String?): Boolean {
        val h = host?.trim()?.lowercase()?.trimEnd('.') ?: return false
        return ALLOWED_WEB_HOSTS.any { it.equals(h, ignoreCase = true) } || h.endsWith(".torn.com")
    }

    fun profile(playerId: Long): String {
        require(playerId > 0L) { "Invalid playerId" }
        return "https://www.torn.com/profiles.php?XID=$playerId"
    }

    fun attack(playerId: Long): String {
        require(playerId > 0L) { "Invalid playerId" }
        return "https://www.torn.com/loader.php?sid=attack&user2ID=$playerId"
    }

    fun attackFromRawId(rawId: String): String? {
        val digits = rawId.trim()
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
        val id = digits.toLongOrNull() ?: return null
        if (id <= 0L) return null
        return attack(id)
    }

    fun isLoginUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in ALLOWED_WEB_HOSTS) return false
        val path = uri.path?.lowercase().orEmpty()
        val sid = runCatching { uri.getQueryParameter("sid") }.getOrNull()?.lowercase()
        return path.contains("login") || path.contains("authenticate") || sid == "auth"
    }

    fun isHomeUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in ALLOWED_WEB_HOSTS) return false
        val path = uri.path?.lowercase().orEmpty()
        return path.isEmpty() || path == "/" || path == "/index.php"
    }

    fun isHospitalListUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in ALLOWED_WEB_HOSTS) return false
        return uri.path?.lowercase()?.contains("hospitalview") == true
    }
}
