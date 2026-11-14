package app.crowdhub.core.network

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

private class TestClock(var now: Long = 10_000L) : Clock {
    override fun nowEpochMs(): Long = now
    override fun offsetMs(): Long = 0L
}

class ResponseCacheTest {
    @Test
    fun `returns cached response within TTL and coalesces concurrent callers`() = runTest {
        val clock = TestClock(10_000L)
        val cache = ResponseCache(clock, defaultTtlMs = 30_000L)
        val calls = AtomicInteger(0)
        val key = CacheKey("acct", "user/basic")

        val a = async {
            cache.getOrFetch(key) {
                calls.incrementAndGet()
                delay(50)
                ApiResult.Success("""{"ok":true}""", clock.nowEpochMs())
            }
        }
        val b = async {
            cache.getOrFetch(key) {
                calls.incrementAndGet()
                delay(50)
                ApiResult.Success("""{"ok":true}""", clock.nowEpochMs())
            }
        }

        val resA = a.await()
        val resB = b.await()
        assertTrue(resA is ApiResult.Success && resB is ApiResult.Success)
        assertEquals(1, calls.get())

        // Subsequent call inside 30s TTL hits memory cache
        clock.now += 15_000L
        val resC = cache.getOrFetch(key) {
            calls.incrementAndGet()
            ApiResult.Success("""{"ok":2}""", clock.nowEpochMs())
        }
        assertTrue(resC is ApiResult.Success && resC.fromCache)
        assertEquals(1, calls.get())
    }

    @Test
    fun `returns stale data on failure after TTL expires`() = runTest {
        val clock = TestClock(10_000L)
        val cache = ResponseCache(clock, defaultTtlMs = 30_000L)
        val key = CacheKey("acct", "user/bars")

        cache.getOrFetch(key) { ApiResult.Success("""{"energy":100}""", clock.nowEpochMs()) }
        clock.now += 40_000L

        val failure = cache.getOrFetch(key) {
            ApiResult.Failure(ApiError.Network("Offline"))
        }
        assertTrue(failure is ApiResult.Failure)
        val fail = failure as ApiResult.Failure
        assertEquals("""{"energy":100}""", fail.staleData)
        assertEquals(10_000L, fail.staleFetchedAtMs)
    }
}
