package app.crowdhub.core.alerts

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.crowdhub.MainActivity
import app.crowdhub.R
import app.crowdhub.core.network.CrowLog

class Scheduler(private val context: Context) {

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun scheduleAlarm(
        dedupeKey: String,
        channelId: String,
        title: String,
        body: String,
        fireAtEpochMs: Long,
        deepLink: String?
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = buildAlarmPendingIntent(
            dedupeKey = dedupeKey,
            channelId = channelId,
            title = title,
            body = body,
            deepLink = deepLink
        )
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtEpochMs, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtEpochMs, pi)
            }
        } catch (se: SecurityException) {
            CrowLog.w(message = "Exact alarm permission missing; falling back to inexact", throwable = se)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtEpochMs, pi)
        }
    }

    fun cancelAlarm(dedupeKey: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALERT
            putExtra(AlarmReceiver.EXTRA_DEDUPE_KEY, dedupeKey)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            dedupeKey.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun postNotification(
        dedupeKey: String,
        channelId: String,
        title: String,
        body: String,
        deepLink: String?
    ) {
        if (!hasNotificationPermission()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!deepLink.isNullOrBlank()) {
                putExtra(EXTRA_DEEP_LINK, deepLink)
            }
        }
        val tapPi = PendingIntent.getActivity(
            context,
            dedupeKey.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = when (channelId) {
            NotificationChannels.TIMERS, NotificationChannels.WAR -> NotificationCompat.PRIORITY_HIGH
            NotificationChannels.WATCH_MODE, NotificationChannels.SYSTEM -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .build()

        manager.notify(dedupeKey.hashCode(), notification)
    }

    private fun buildAlarmPendingIntent(
        dedupeKey: String,
        channelId: String,
        title: String,
        body: String,
        deepLink: String?
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALERT
            putExtra(AlarmReceiver.EXTRA_DEDUPE_KEY, dedupeKey)
            putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, channelId)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
            putExtra(AlarmReceiver.EXTRA_DEEP_LINK, deepLink)
        }
        return PendingIntent.getBroadcast(
            context,
            dedupeKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_DEEP_LINK = "crowdhub_deep_link"
    }
}
