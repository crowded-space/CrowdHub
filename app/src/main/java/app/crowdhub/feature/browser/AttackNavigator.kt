package app.crowdhub.feature.browser

import android.content.Context
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.settings.AttackBrowserMode
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.provider.torn.TornUrls

class AttackNavigator(
    private val browserNavigator: BrowserNavigator,
    private val settingsStore: SettingsStore
) {
    suspend fun open(
        context: Context,
        playerId: Long,
        onNavigateToBrowser: () -> Unit
    ) {
        val url = TornUrls.attackFromRawId(playerId.toString()) ?: return
        val prefs = settingsStore.current()
        when (prefs.openAttacksMode) {
            AttackBrowserMode.IN_APP -> {
                browserNavigator.requestInAppOpen(url)
                onNavigateToBrowser()
            }
            AttackBrowserMode.EXTERNAL -> {
                browserNavigator.openExternal(context, url)
            }
        }
    }
}
