package app.crowdhub.core.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val host = context.applicationContext as? AlertHost ?: return
        val pending = goAsync()
        host.appScope.launch {
            try {
                host.alertEngine.rescheduleAllOnBoot()
                PollWorker.enqueue(context)
            } finally {
                pending.finish()
            }
        }
    }
}
