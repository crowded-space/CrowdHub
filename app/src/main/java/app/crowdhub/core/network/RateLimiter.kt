package app.crowdhub.core.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong

enum class RequestPriority(val rank: Int) {
    USER(0),
    WATCH(1),
    BACKGROUND(2),
    SCRIPT(3)
}

object RateLimitConfig {
    const val GLOBAL_CAP_PER_MIN = 85
    const val SCRIPT_RESERVED_CAP_PER_MIN = 20
    const val APP_TOTAL_CAP_PER_MIN = 65
    const val USER_CAP_PER_MIN = 65
    const val WATCH_CAP_PER_MIN = 45
    const val BACKGROUND_CAP_PER_MIN = 15
    const val WINDOW_MS = 60_000L
    const val RATE_LIMIT_BACKOFF_MS = 60_000L
    const val IP_BLOCK_PAUSE_MS = 300_000L
}

data class RateLimiterSnapshot(
    val totalLastMinute: Int,
    val userLastMinute: Int,
    val watchLastMinute: Int,
    val backgroundLastMinute: Int,
    val scriptLastMinute: Int,
    val globalBlockedUntilMs: Long,
    val rateLimitBackoffUntilMs: Long
)

sealed class AcquireResult {
    data object Granted : AcquireResult()
    data class Denied(val reason: ApiError) : AcquireResult()
}

class RateLimiter(
    private val clock: Clock,
    private val sleepFn: suspend (Long) -> Unit = { ms -> delay(ms) }
) {
    private val mutex = Mutex()
    private val seqGen = AtomicLong(0)

    private data class TimestampedCall(val atMs: Long, val priority: RequestPriority)
    private data class Waiter(val priority: RequestPriority, val seq: Long) : Comparable<Waiter> {
        override fun compareTo(other: Waiter): Int {
            val p = priority.rank.compareTo(other.priority.rank)
            return if (p != 0) p else seq.compareTo(other.seq)
        }
    }

    private val accountWindows = mutableMapOf<String, ArrayDeque<TimestampedCall>>()
    private val waitingQueue = PriorityBlockingQueue<Waiter>()

    @Volatile
    private var globalIpBlockUntilMs: Long = 0L
    @Volatile
    private var rateLimitBackoffUntilMs: Long = 0L
    @Volatile
    private var ipProbeInFlight: Boolean = false

    suspend fun acquire(accountId: String, priority: RequestPriority): AcquireResult {
        val waiter = Waiter(priority, seqGen.incrementAndGet())
        if (priority != RequestPriority.BACKGROUND) {
            waitingQueue.add(waiter)
        }
        try {
            while (true) {
                var waitTimeMs = 0L
                val result: AcquireResult? = mutex.withLock {
                    val now = clock.nowEpochMs()

                    if (now < globalIpBlockUntilMs) {
                        return@withLock AcquireResult.Denied(ApiError.IpBlocked())
                    }

                    if (now < rateLimitBackoffUntilMs && priority != RequestPriority.USER) {
                        if (priority == RequestPriority.BACKGROUND) {
                            return@withLock AcquireResult.Denied(ApiError.RateLimited())
                        }
                        waitTimeMs = (rateLimitBackoffUntilMs - now).coerceAtLeast(250L)
                        return@withLock null
                    }

                    val window = accountWindows.getOrPut(accountId) { ArrayDeque() }
                    pruneOld(window, now)

                    val canServe = canGrant(window, priority)
                    val isHighestWaiting = priority == RequestPriority.BACKGROUND ||
                        waitingQueue.peek()?.let { it.priority.rank >= priority.rank } ?: true

                    if (canServe && isHighestWaiting) {
                        window.addLast(TimestampedCall(now, priority))
                        return@withLock AcquireResult.Granted
                    }

                    if (priority == RequestPriority.BACKGROUND) {
                        return@withLock AcquireResult.Denied(
                            ApiError.RateLimited("Background request dropped to preserve API budget")
                        )
                    }

                    if (priority == RequestPriority.SCRIPT && !canGrant(window, priority)) {
                        val oldest = window.firstOrNull()?.atMs ?: now
                        val retryMs = (oldest + RateLimitConfig.WINDOW_MS - now).coerceIn(100L, 1500L)
                        waitTimeMs = retryMs
                        return@withLock null
                    }

                    val oldestRelevant = window.firstOrNull()?.atMs ?: now
                    waitTimeMs = (oldestRelevant + RateLimitConfig.WINDOW_MS - now).coerceIn(150L, 2_000L)
                    null
                }

                if (result != null) {
                    return result
                }
                sleepFn(waitTimeMs)
            }
        } finally {
            waitingQueue.remove(waiter)
        }
    }

    suspend fun triggerRateLimitBackoff() {
        mutex.withLock {
            val until = clock.nowEpochMs() + RateLimitConfig.RATE_LIMIT_BACKOFF_MS
            if (until > rateLimitBackoffUntilMs) {
                rateLimitBackoffUntilMs = until
            }
        }
    }

    suspend fun triggerIpBlockPause() {
        mutex.withLock {
            val until = clock.nowEpochMs() + RateLimitConfig.IP_BLOCK_PAUSE_MS
            if (until > globalIpBlockUntilMs) {
                globalIpBlockUntilMs = until
                ipProbeInFlight = false
            }
        }
    }

    suspend fun snapshot(accountId: String): RateLimiterSnapshot = mutex.withLock {
        val now = clock.nowEpochMs()
        val window = accountWindows[accountId] ?: ArrayDeque()
        pruneOld(window, now)
        RateLimiterSnapshot(
            totalLastMinute = window.size,
            userLastMinute = window.count { it.priority == RequestPriority.USER },
            watchLastMinute = window.count { it.priority == RequestPriority.WATCH },
            backgroundLastMinute = window.count { it.priority == RequestPriority.BACKGROUND },
            scriptLastMinute = window.count { it.priority == RequestPriority.SCRIPT },
            globalBlockedUntilMs = globalIpBlockUntilMs,
            rateLimitBackoffUntilMs = rateLimitBackoffUntilMs
        )
    }

    private fun pruneOld(window: ArrayDeque<TimestampedCall>, nowMs: Long) {
        val cutoff = nowMs - RateLimitConfig.WINDOW_MS
        while (window.isNotEmpty() && window.first().atMs <= cutoff) {
            window.removeFirst()
        }
    }

    private fun canGrant(window: ArrayDeque<TimestampedCall>, priority: RequestPriority): Boolean {
        val total = window.size
        if (total >= RateLimitConfig.GLOBAL_CAP_PER_MIN) return false

        val scriptCount = window.count { it.priority == RequestPriority.SCRIPT }
        val userCount = window.count { it.priority == RequestPriority.USER }
        val watchCount = window.count { it.priority == RequestPriority.WATCH }
        val bgCount = window.count { it.priority == RequestPriority.BACKGROUND }
        val appTotal = userCount + watchCount + bgCount

        return when (priority) {
            RequestPriority.SCRIPT -> scriptCount < RateLimitConfig.SCRIPT_RESERVED_CAP_PER_MIN
            RequestPriority.USER ->
                appTotal < RateLimitConfig.APP_TOTAL_CAP_PER_MIN &&
                    userCount < RateLimitConfig.USER_CAP_PER_MIN
            RequestPriority.WATCH ->
                appTotal < RateLimitConfig.APP_TOTAL_CAP_PER_MIN &&
                    watchCount < RateLimitConfig.WATCH_CAP_PER_MIN
            RequestPriority.BACKGROUND ->
                appTotal < RateLimitConfig.APP_TOTAL_CAP_PER_MIN &&
                    bgCount < RateLimitConfig.BACKGROUND_CAP_PER_MIN
        }
    }
}
