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

    @Test
    fun `reads persistent disk cache on cold start inside TTL and isolates profiles`() = runTest {
        val clock = TestClock(100_000L)
        val disk = mutableMapOf<String, Pair<CachedResponse, Long>>()
        val store = object : PersistentCacheStore {
            override suspend fun read(key: String): CachedResponse? = disk[key]?.first
            override suspend fun write(key: String, body: String, fetchedAtMs: Long, ttlMs: Long) {
                disk[key] = CachedResponse(body, fetchedAtMs) to ttlMs
            }
            override suspend fun deleteForProfile(profileId: String) {
                disk.keys.removeAll { it.startsWith("$profileId|") }
            }
            override suspend fun clearAll() {
                disk.clear()
            }
        }

        // First instance writes profile A and profile B entries
        val cache1 = ResponseCache(clock, defaultTtlMs = 60_000L, persistentStore = store)
        val keyA = CacheKey("acct-1", "faction/12/members", profileId = "prof-a")
        val keyB = CacheKey("acct-2", "faction/12/members", profileId = "prof-b")
        cache1.getOrFetch(keyA, ttlMs = 60_000L) { ApiResult.Success("""{"p":"A"}""", clock.nowEpochMs()) }
        cache1.getOrFetch(keyB, ttlMs = 60_000L) { ApiResult.Success("""{"p":"B"}""", clock.nowEpochMs()) }

        // Cold-start new ResponseCache (empty memory) hits disk within TTL without network fetch
        clock.now += 20_000L
        val cache2 = ResponseCache(clock, defaultTtlMs = 60_000L, persistentStore = store)
        val networkCalls = AtomicInteger(0)
        val coldA = cache2.getOrFetch(keyA, ttlMs = 60_000L) {
            networkCalls.incrementAndGet()
            ApiResult.Success("""{"p":"A-net"}""", clock.nowEpochMs())
        }
        assertTrue(coldA is ApiResult.Success && coldA.fromCache)
        assertEquals("""{"p":"A"}""", (coldA as ApiResult.Success).data)
        assertEquals(0, networkCalls.get())

        // Invalidating profile A removes only profile A while keeping profile B intact
        cache2.invalidateProfile("prof-a")
        val afterInvalidA = cache2.getOrFetch(keyA, ttlMs = 60_000L) {
            networkCalls.incrementAndGet()
            ApiResult.Success("""{"p":"A-fresh"}""", clock.nowEpochMs())
        }
        val intactB = cache2.getOrFetch(keyB, ttlMs = 60_000L) {
            networkCalls.incrementAndGet()
            ApiResult.Success("""{"p":"B-net"}""", clock.nowEpochMs())
        }
        assertEquals(1, networkCalls.get())
        assertEquals("""{"p":"A-fresh"}""", (afterInvalidA as ApiResult.Success).data)
        assertEquals("""{"p":"B"}""", (intactB as ApiResult.Success).data)
        assertTrue(intactB.fromCache)
    }
}
