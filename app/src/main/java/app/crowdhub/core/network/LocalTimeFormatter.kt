package app.crowdhub.core.network

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formats timestamps strictly in the device's current system local timezone.
 * Reads [ZoneId.systemDefault] dynamically per call so timezone/DST changes apply immediately.
 */
object LocalTimeFormatter {
    private val hmFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val hmsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun systemZone(): ZoneId = ZoneId.systemDefault()

    fun shortZoneLabel(epochMs: Long): String {
        val zone = systemZone()
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone)
        return zone.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            ?: zdt.offset.id
    }

    fun fullZoneLabel(): String = systemZone().id

    fun formatTimeHm(epochMs: Long): String {
        val zone = systemZone()
        return hmFormatter.withZone(zone).format(Instant.ofEpochMilli(epochMs))
    }

    fun formatTimeHms(epochMs: Long): String {
        val zone = systemZone()
        return hmsFormatter.withZone(zone).format(Instant.ofEpochMilli(epochMs))
    }

    fun formatTimeRange(
        startMs: Long,
        endMs: Long,
        includeSeconds: Boolean = false
    ): String {
        val zone = systemZone()
        val fmt = (if (includeSeconds) hmsFormatter else hmFormatter).withZone(zone)
        val startText = fmt.format(Instant.ofEpochMilli(startMs))
        val endText = fmt.format(Instant.ofEpochMilli(endMs))
        return if (startText == endText) startText else "$startText–$endText"
    }

    fun formatDateTime(epochMs: Long): String {
        val zone = systemZone()
        return dateTimeFormatter.withZone(zone).format(Instant.ofEpochMilli(epochMs))
    }

    fun formatDate(epochMs: Long): String {
        val zone = systemZone()
        return dateFormatter.withZone(zone).format(Instant.ofEpochMilli(epochMs))
    }
}
