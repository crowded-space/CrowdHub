package app.crowdhub.feature.browser

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import app.crowdhub.core.browser.WebLoadError
import app.crowdhub.core.browser.WebViewController
import java.net.URI
import java.net.URLDecoder

data class BrowserTabRuntime(
    val id: String,
    val url: String,
    val title: String = "Torn",
    val progress: Int = 100,
    val loadError: WebLoadError? = null,
    val controller: WebViewController? = null,
    val savedBundle: Bundle? = null,
    val lastActiveAtMs: Long = 0L,
    val spawnedFromLink: Boolean = false
)

data class BrowserTabVisual(
    val icon: ImageVector,
    val contentDescription: String
)

object BrowserIconResolver {
    fun resolve(url: String, title: String = ""): BrowserTabVisual {
        val uri = runCatching { URI(url) }.getOrNull()
        val path = uri?.path?.lowercase().orEmpty()
        val sid = extractQueryParam(uri?.rawQuery, "sid")?.lowercase().orEmpty()
        val lowerTitle = title.lowercase()

        return when {
            sid == "hospital" || path.contains("hospital") -> BrowserTabVisual(Icons.Outlined.LocalHospital, "Hospital")
            path.contains("faction") -> BrowserTabVisual(Icons.Outlined.Groups, "Faction")
            sid == "attack" || path.contains("attack") -> BrowserTabVisual(Icons.Outlined.GpsFixed, "Attack")
            path.contains("profiles") -> BrowserTabVisual(Icons.Outlined.Person, "Profile")
            sid == "gym" || path.contains("gym") -> BrowserTabVisual(Icons.Outlined.FitnessCenter, "Gym")
            sid == "crimes" || path.contains("crimes") -> BrowserTabVisual(Icons.Outlined.Psychology, "Crimes")
            sid == "stocks" || path.contains("stockexchange") -> BrowserTabVisual(Icons.Outlined.ShowChart, "Stocks")
            sid == "travel" || path.contains("travelagency") -> BrowserTabVisual(Icons.Outlined.FlightTakeoff, "Travel")
            sid == "itemmarket" || path.contains("imarket") || path.contains("bazaar") ->
                BrowserTabVisual(Icons.Outlined.Storefront, "Market")
            path.contains("city") -> BrowserTabVisual(Icons.Outlined.LocationCity, "City")
            path.contains("messages") -> BrowserTabVisual(Icons.Outlined.MailOutline, "Messages")
            path.contains("events") -> BrowserTabVisual(Icons.Outlined.Notifications, "Events")
            path.contains("forums") -> BrowserTabVisual(Icons.Outlined.Forum, "Forums")
            path.contains("preferences") -> BrowserTabVisual(Icons.Outlined.Settings, "Preferences")
            path.isEmpty() || path == "/" || path.contains("index.php") ->
                BrowserTabVisual(Icons.Outlined.Home, "Torn Home")
            lowerTitle.contains("hospital") -> BrowserTabVisual(Icons.Outlined.LocalHospital, "Hospital")
            lowerTitle.contains("faction") -> BrowserTabVisual(Icons.Outlined.Groups, "Faction")
            else -> BrowserTabVisual(Icons.Outlined.Language, title.ifBlank { "Torn Tab" })
        }
    }

    private fun extractQueryParam(rawQuery: String?, key: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        return rawQuery.split('&').firstNotNullOfOrNull { pair ->
            val idx = pair.indexOf('=')
            val k = if (idx >= 0) pair.substring(0, idx) else pair
            if (k.equals(key, ignoreCase = true) && idx >= 0) {
                runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrNull()
            } else {
                null
            }
        }
    }
}
