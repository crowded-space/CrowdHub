package app.crowdhub.core.network

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface Clock {
    fun nowEpochMs(): Long
    fun offsetMs(): Long
}

class ServerClock(
    private val deviceTimeMs: () -> Long = { System.currentTimeMillis() }
) : Clock {
    private val lock = Any()
    private val recentOffsets = ArrayDeque<Long>(MAX_SAMPLES)
    @Volatile
    private var cachedMedianOffsetMs: Long = 0L

    @Volatile
    var lastServerDateHeader: String? = null
        private set

    override fun nowEpochMs(): Long = deviceTimeMs() + cachedMedianOffsetMs

    override fun offsetMs(): Long = cachedMedianOffsetMs

    fun recordServerDateHeader(dateHeader: String?) {
        if (dateHeader.isNullOrBlank()) return
        val serverMs = parseHttpDateMs(dateHeader) ?: return
        val offset = serverMs - deviceTimeMs()
        synchronized(lock) {
            lastServerDateHeader = dateHeader
            if (recentOffsets.size >= MAX_SAMPLES) {
                recentOffsets.removeFirst()
            }
            recentOffsets.addLast(offset)
            val sorted = recentOffsets.sorted()
            cachedMedianOffsetMs = sorted[sorted.size / 2]
        }
    }

    companion object {
        private const val MAX_SAMPLES = 5

        private val DATE_FORMATS = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM d HH:mm:ss yyyy"
        )

        fun parseHttpDateMs(header: String): Long? {
            val trimmed = header.trim()
            for (pattern in DATE_FORMATS) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("GMT")
                        isLenient = false
                    }
                    val parsed = sdf.parse(trimmed)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}
