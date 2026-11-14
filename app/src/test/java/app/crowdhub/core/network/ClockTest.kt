package app.crowdhub.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClockTest {
    @Test
    fun `median of last five server Date headers smooths outlier`() {
        var deviceMs = 1_700_000_000_000L
        val clock = ServerClock { deviceMs }

        // Reference HTTP date is 2023-11-14T22:13:20Z -> 1700000000000 ms
        // Offsets: +10s, +12s, +600s (outlier), +11s, +13s -> sorted: 10, 11, 12, 13, 600 -> median +12s
        clock.recordServerDateHeader("Tue, 14 Nov 2023 22:13:30 GMT") // +10s
        clock.recordServerDateHeader("Tue, 14 Nov 2023 22:13:32 GMT") // +12s
        clock.recordServerDateHeader("Tue, 14 Nov 2023 22:23:20 GMT") // +600s outlier
        clock.recordServerDateHeader("Tue, 14 Nov 2023 22:13:31 GMT") // +11s
        clock.recordServerDateHeader("Tue, 14 Nov 2023 22:13:33 GMT") // +13s

        assertEquals(12_000L, clock.offsetMs())
        assertEquals(deviceMs + 12_000L, clock.nowEpochMs())
    }

    @Test
    fun `parses standard HTTP date header`() {
        val parsed = ServerClock.parseHttpDateMs("Thu, 01 Jan 1970 00:01:00 GMT")
        assertNotNull(parsed)
        assertEquals(60_000L, parsed)
    }
}
