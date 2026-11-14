package app.crowdhub.feature.browser

import app.crowdhub.core.settings.QuickLinkItem
import app.crowdhub.provider.torn.TornUrls

object QuickLinks {
    fun defaults(playerId: String?): List<QuickLinkItem> {
        val pid = playerId?.toLongOrNull()
        return buildList {
            add(QuickLinkItem("Home", TornUrls.HOME))
            if (pid != null && pid > 0L) {
                add(QuickLinkItem("Profile", TornUrls.profile(pid)))
            }
            add(QuickLinkItem("Hospital", TornUrls.HOSPITAL_LIST))
        }
    }
}
