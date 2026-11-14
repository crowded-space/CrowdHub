package app.crowdhub.core.alerts

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.crowdhub.MainActivity
import app.crowdhub.R
import app.crowdhub.core.network.CrowLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WatchModeService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCleanly(stoppedBySystem = false)
            return START_NOT_STICKY
        }

        val host = applicationContext as? AlertHost
        if (host == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (watchJob?.isActive != true) {
            watchJob = serviceScope.launch {
                host.settingsStore.setWatchModeActive(true)
                val prefs = host.settingsStore.current()
                val durationMs = prefs.watchModeDurationHours.coerceIn(1, 6) * 3_600_000L
                val stopAtMs = host.clock.nowEpochMs() + durationMs

                while (isActive && host.clock.nowEpochMs() < stopAtMs) {
                    try {
                        host.alertEngine.runCycle(AlertTriggerMode.WATCH)
                        host.settingsStore.recordSyncStatus(
                            epochMs = host.clock.nowEpochMs(),
                            summary = "Watch Mode 30s sync"
                        )
                    } catch (t: Throwable) {
                        CrowLog.w(message = "WatchMode poll error", throwable = t)
                    }
                    delay(POLL_INTERVAL_MS)
                }

                if (isActive) {
                    stopCleanly(stoppedBySystem = false)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopCleanly(stoppedBySystem = true)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopCleanly(stoppedBySystem = true)
    }

    private fun stopCleanly(stoppedBySystem: Boolean) {
        watchJob?.cancel()
        val host = applicationContext as? AlertHost
        if (host != null) {
            host.appScope.launch {
                host.settingsStore.setWatchModeActive(false)
                if (stoppedBySystem) {
                    host.scheduler.postNotification(
                        dedupeKey = "system:watch_mode_timeout:${host.clock.nowEpochMs() / 60_000L}",
                        channelId = NotificationChannels.SYSTEM,
                        title = "Watch Mode",
                        body = getString(R.string.watch_mode_stopped_system),
                        deepLink = "dashboard"
                    )
                }
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildOngoingNotification() = run {
        val stopIntent = Intent(this, WatchModeService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this,
            102,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, NotificationChannels.WATCH_MODE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.watch_mode_active))
            .setContentText(getString(R.string.watch_mode_subtitle))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPi)
            .addAction(0, getString(R.string.watch_mode_stop), stopPi)
            .build()
    }

    override fun onDestroy() {
        watchJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 7001
        private const val POLL_INTERVAL_MS = 30_000L
        const val ACTION_START = "app.crowdhub.WATCH_START"
        const val ACTION_STOP = "app.crowdhub.WATCH_STOP"

        fun start(context: Context) {
            val intent = Intent(context, WatchModeService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WatchModeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
