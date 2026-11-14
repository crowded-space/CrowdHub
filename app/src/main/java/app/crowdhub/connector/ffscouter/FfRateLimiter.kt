package app.crowdhub.connector.ffscouter

import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FfRateLimiter(
    private val clock: Clock,
    private val maxPerMin: Int = FfDefaults.RATE_LIMIT_PER_MIN
) {
    private val mutex = Mutex()
    private val calls = ArrayDeque<Long>()
    private var backoffUntilMs: Long = 0L

    suspend fun tryAcquire(): ApiError? = mutex.withLock {
        val now = clock.nowEpochMs()
        if (now < backoffUntilMs) {
            return@withLock ApiError.RateLimited("FFScouter rate limit backoff")
        }
        val cutoff = now - 60_000L
        while (calls.isNotEmpty() && calls.first() <= cutoff) {
            calls.removeFirst()
        }
        if (calls.size >= maxPerMin) {
            return@withLock ApiError.RateLimited("FFScouter limit reached ($maxPerMin/min)")
        }
        calls.addLast(now)
        null
    }

    suspend fun triggerBackoff() {
        mutex.withLock {
            backoffUntilMs = clock.nowEpochMs() + FfDefaults.BACKOFF_429_MS
        }
    }
}
