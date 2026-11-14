package app.crowdhub.core.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val TIMERS = "timers"
    const val WAR = "war"
    const val STOCKS = "stocks"
    const val WATCH_MODE = "watch_mode"
    const val SYSTEM = "system"

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                TIMERS,
                "Timers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bars full, cooldown ready, hospital/jail release, and own landing timers"
            },
            NotificationChannel(
                WAR,
                "War & Targets",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ranked war stakeout, enemy landings, and target release alerts"
            },
            NotificationChannel(
                STOCKS,
                "Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Stock target price and percent-move warnings"
            },
            NotificationChannel(
                WATCH_MODE,
                "Watch Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing status notification while Watch Mode is running"
                setShowBadge(false)
            },
            NotificationChannel(
                SYSTEM,
                "System",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "API key status and Watch Mode system notices"
            }
        )
        manager.createNotificationChannels(channels)
    }
}
