package app.crowdhub.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeClock(var nowMs: Long = 1_000_000L) : Clock {
    override fun nowEpochMs(): Long = nowMs
    override fun offsetMs(): Long = 0L
}

class RateLimiterTest {
    @Test
    fun `background requests respect 15 per minute cap and drop immediately`() = runTest {
        val clock = FakeClock(100_000L)
        val limiter = RateLimiter(clock) { clock.nowMs += it }

        repeat(15) {
            val res = limiter.acquire("acct-1", RequestPriority.BACKGROUND)
            assertTrue("Expected BG request $it to be granted", res is AcquireResult.Granted)
        }
        val denied = limiter.acquire("acct-1", RequestPriority.BACKGROUND)
        assertTrue(denied is AcquireResult.Denied)

        // Advance past 60s window
        clock.nowMs += 60_001L
        val afterWindow = limiter.acquire("acct-1", RequestPriority.BACKGROUND)
        assertTrue(afterWindow is AcquireResult.Granted)
    }

    @Test
    fun `script class has reserved 20 req budget even when app classes fill 65`() = runTest {
        val clock = FakeClock(500_000L)
        val limiter = RateLimiter(clock) { clock.nowMs += it }

        repeat(65) {
            assertTrue(limiter.acquire("acct-1", RequestPriority.USER) is AcquireResult.Granted)
        }
        // Background should drop because app total reached 65
        assertTrue(limiter.acquire("acct-1", RequestPriority.BACKGROUND) is AcquireResult.Denied)

        // Script should still be allowed up to 20 (65 + 20 = 85 total)
        repeat(20) {
            assertTrue(limiter.acquire("acct-1", RequestPriority.SCRIPT) is AcquireResult.Granted)
        }

        val snap = limiter.snapshot("acct-1")
        assertEquals(85, snap.totalLastMinute)
        assertEquals(65, snap.userLastMinute)
        assertEquals(20, snap.scriptLastMinute)
    }

    @Test
    fun `ip block pauses all requests globally`() = runTest {
        val clock = FakeClock(200_000L)
        val limiter = RateLimiter(clock) { clock.nowMs += it }

        limiter.triggerIpBlockPause()
        val res = limiter.acquire("acct-1", RequestPriority.USER)
        assertTrue(res is AcquireResult.Denied)
        assertTrue((res as AcquireResult.Denied).reason is ApiError.IpBlocked)
    }
}
