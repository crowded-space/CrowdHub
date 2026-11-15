package app.crowdhub.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class LocalTimeFormatterTest {
    @Test
    fun `uses dynamic system timezone for arrival time and range formatting`() {
        val prev = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo")) // UTC+9
            // 2026-01-01T00:00:00Z -> 09:00:00 in Asia/Tokyo
            val startMs = 1767225600_000L
            val endMs = startMs + 15 * 60_000L
            assertEquals("09:00", LocalTimeFormatter.formatTimeHm(startMs))
            assertEquals("09:00:00", LocalTimeFormatter.formatTimeHms(startMs))
            assertEquals("09:00–09:15", LocalTimeFormatter.formatTimeRange(startMs, endMs))
            assertEquals("Asia/Tokyo", LocalTimeFormatter.fullZoneLabel())

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("00:00", LocalTimeFormatter.formatTimeHm(startMs))
            assertTrue(LocalTimeFormatter.formatDate(startMs).contains("2026"))
        } finally {
            TimeZone.setDefault(prev)
        }
    }
}
