package app.crowdhub.core.alerts

import app.crowdhub.core.db.ScheduledAlertDao
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope

interface AlertHost {
    val alertEngine: AlertEngine
    val scheduler: Scheduler
    val scheduledAlertDao: ScheduledAlertDao
    val settingsStore: SettingsStore
    val clock: Clock
    val appScope: CoroutineScope
    suspend fun runStockCleanup()
}
