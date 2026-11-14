package app.crowdhub.core.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALERT) return
        val dedupeKey = intent.getStringExtra(EXTRA_DEDUPE_KEY) ?: return
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: NotificationChannels.TIMERS
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Crowd Hub"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val deepLink = intent.getStringExtra(EXTRA_DEEP_LINK)

        val host = context.applicationContext as? AlertHost ?: return
        val pending = goAsync()
        host.appScope.launch {
            try {
                val existing = host.scheduledAlertDao.getByKey(dedupeKey)
                if (existing?.state == PersistedAlertState.CANCELLED.name) {
                    return@launch
                }
                host.scheduler.postNotification(dedupeKey, channelId, title, body, deepLink)
                host.scheduledAlertDao.updateState(
                    dedupeKey = dedupeKey,
                    state = PersistedAlertState.FIRED.name,
                    updatedAt = host.clock.nowEpochMs()
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_ALERT = "app.crowdhub.ACTION_FIRE_ALERT"
        const val EXTRA_DEDUPE_KEY = "dedupe_key"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_DEEP_LINK = "deep_link"
    }
}
