package app.crowdhub.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CacheKey(
    val accountId: String,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val profileId: String = ""
) {
    fun canonicalKey(): String {
        val sortedParams = queryParams.entries
            .filterNot { it.key.equals("key", ignoreCase = true) }
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        val base = if (sortedParams.isEmpty()) "$accountId|$path" else "$accountId|$path?$sortedParams"
        return if (profileId.isNotBlank()) "$profileId|$base" else base
    }
}

data class CachedResponse(
    val body: String,
    val fetchedAtMs: Long
)

interface PersistentCacheStore {
    suspend fun read(key: String): CachedResponse?
    suspend fun write(key: String, body: String, fetchedAtMs: Long, ttlMs: Long)
    suspend fun deleteForProfile(profileId: String) {}
    suspend fun clearAll()
}

class ResponseCache(
    private val clock: Clock,
    private val defaultTtlMs: Long = DEFAULT_TTL_MS,
    private val persistentStore: PersistentCacheStore? = null
) {
    private val mutex = Mutex()
    private val memory = mutableMapOf<String, CachedResponse>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<ApiResult<String>>>()

    suspend fun getOrFetch(
        key: CacheKey,
        ttlMs: Long = defaultTtlMs,
        forceRefresh: Boolean = false,
        fetcher: suspend () -> ApiResult<String>
    ): ApiResult<String> {
        val canonical = key.canonicalKey()

        if (!forceRefresh) {
            val cached = mutex.withLock { memory[canonical] }
            if (cached != null && (clock.nowEpochMs() - cached.fetchedAtMs) < ttlMs) {
                return ApiResult.Success(
                    data = cached.body,
                    fetchedAtMs = cached.fetchedAtMs,
                    fromCache = true,
                    isStale = false
                )
            }
            val persisted = persistentStore?.read(canonical)
            if (persisted != null && (clock.nowEpochMs() - persisted.fetchedAtMs) < ttlMs) {
                mutex.withLock {
                    memory[canonical] = persisted
                }
                return ApiResult.Success(
                    data = persisted.body,
                    fetchedAtMs = persisted.fetchedAtMs,
                    fromCache = true,
                    isStale = false
                )
            }
        }

        val (deferred, isLeader) = mutex.withLock {
            val existing = inFlight[canonical]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<ApiResult<String>>()
                inFlight[canonical] = created
                created to true
            }
        }

        if (!isLeader) {
            return deferred.await()
        }

        return try {
            val result = fetcher()
            val finalResult = when (result) {
                is ApiResult.Success -> {
                    mutex.withLock {
                        memory[canonical] = CachedResponse(result.data, result.fetchedAtMs)
                    }
                    persistentStore?.write(canonical, result.data, result.fetchedAtMs, ttlMs)
                    result
                }
                is ApiResult.Failure -> {
                    val stale = mutex.withLock { memory[canonical] }
                        ?: persistentStore?.read(canonical)
                    if (stale != null) {
                        ApiResult.Failure(
                            error = result.error,
                            staleData = stale.body,
                            staleFetchedAtMs = stale.fetchedAtMs
                        )
                    } else {
                        result
                    }
                }
            }
            deferred.complete(finalResult)
            finalResult
        } catch (t: Throwable) {
            val failure = ApiResult.Failure<String>(
                error = ApiError.Unknown(null, CrowLog.redact(t.message ?: "Unexpected error"))
            )
            deferred.complete(failure)
            failure
        } finally {
            mutex.withLock {
                inFlight.remove(canonical)
            }
        }
    }

    suspend fun invalidateAccount(accountId: String) {
        mutex.withLock {
            val legacyPrefix = "$accountId|"
            val scopedToken = "|$accountId|"
            memory.keys.removeAll { it.startsWith(legacyPrefix) || it.contains(scopedToken) }
        }
    }

    suspend fun invalidateProfile(profileId: String) {
        if (profileId.isBlank()) return
        mutex.withLock {
            val prefix = "$profileId|"
            memory.keys.removeAll { it.startsWith(prefix) }
        }
        persistentStore?.deleteForProfile(profileId)
    }

    suspend fun clearAll() {
        mutex.withLock { memory.clear() }
        persistentStore?.clearAll()
    }

    companion object {
        const val DEFAULT_TTL_MS = 30_000L
    }
}
