package app.crowdhub.provider.torn

import app.crowdhub.core.network.AcquireResult
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.CacheKey
import app.crowdhub.core.network.CrowLog
import app.crowdhub.core.network.RateLimiter
import app.crowdhub.core.network.RequestPriority
import app.crowdhub.core.network.ResponseCache
import app.crowdhub.core.network.ServerClock
import app.crowdhub.core.profile.AccountStatus
import app.crowdhub.core.vault.SecretVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException

data class ProbeResponse(
    val path: String,
    val rawRedactedJson: String,
    val httpDateHeader: String?,
    val clockOffsetMs: Long,
    val error: ApiError? = null
)

class TornApi(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: RateLimiter,
    private val responseCache: ResponseCache,
    private val clock: ServerClock,
    private val vault: SecretVault,
    private val onKeyRejected: suspend (accountId: String, status: AccountStatus, reason: String) -> Unit
) {
    suspend fun validateKey(accountId: String, rawSecret: String): ApiResult<TornBasicInfo> {
        val basicRaw = executeDirect(
            accountId = accountId,
            apiKey = rawSecret,
            version = ApiVersion.V2,
            path = "user/basic",
            queryParams = emptyMap(),
            priority = RequestPriority.USER,
            notifyOnKeyError = false
        )
        val basicInfo = when (basicRaw) {
            is ApiResult.Success -> TornMappers.mapBasic(basicRaw.data)
                ?: return ApiResult.Failure(ApiError.Parse("Unexpected user/basic response"))
            is ApiResult.Failure -> return ApiResult.Failure(basicRaw.error)
        }

        // Probe Limited-access selection (bars) to ensure key is at least Limited
        val barsProbe = executeDirect(
            accountId = accountId,
            apiKey = rawSecret,
            version = ApiVersion.V2,
            path = "user/bars",
            queryParams = emptyMap(),
            priority = RequestPriority.USER,
            notifyOnKeyError = false
        )
        if (barsProbe is ApiResult.Failure && barsProbe.error is ApiError.AccessLevel) {
            return ApiResult.Failure(barsProbe.error)
        }

        return ApiResult.Success(basicInfo, clock.nowEpochMs())
    }

    suspend fun getBasic(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornBasicInfo> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/basic",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapBasic(raw) }
    }

    suspend fun getBars(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornBars> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/bars",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapBars(raw) }
    }

    suspend fun getCooldowns(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornCooldowns> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/cooldowns",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapCooldowns(raw) }
    }

    suspend fun getTravel(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornTravel> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/travel",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapTravel(raw) }
    }

    suspend fun getMoney(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornMoney> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/money",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapMoney(raw) }
    }

    suspend fun getNotifications(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornNotifications> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/notifications",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapNotifications(raw) }
    }

    suspend fun getStocks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornStock>> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "torn/stocks",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapStocks(raw) }
    }

    suspend fun getUserStocks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornUserStock>> {
        val v2Result = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/stocks",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapUserStocks(raw) }

        if (v2Result is ApiResult.Success) return v2Result

        val err = (v2Result as ApiResult.Failure).error
        val shouldFallbackToV1 = err is ApiError.Unknown || err is ApiError.Parse
        if (!shouldFallbackToV1) return v2Result

        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = "user/",
            queryParams = mapOf("selections" to "stocks"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapUserStocks(raw) }
    }

    suspend fun getOwnFactionId(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<Long?> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = "user/",
            queryParams = mapOf("selections" to "profile"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw ->
            val info = TornMappers.mapBasic(raw)
            info?.factionId
        }
    }

    suspend fun getRankedWars(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornRankedWar>> {
        val nowSec = clock.nowEpochMs() / 1_000L
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/$factionId/rankedwars",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapRankedWars(raw, factionId, nowSec) }
    }

    suspend fun getFactionMembers(
        accountId: String,
        factionId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornFactionMember>> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/$factionId/members",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionMembers(raw) }
    }

    suspend fun getFactionBasic(
        accountId: String,
        factionId: Long? = null,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornFactionBasic> {
        val v2Path = if (factionId != null && factionId > 0L) "faction/$factionId/basic" else "faction/basic"
        val v2Res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = v2Path,
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionBasic(raw) }

        if (v2Res is ApiResult.Success) return v2Res
        val err = (v2Res as ApiResult.Failure).error
        if (err !is ApiError.Unknown && err !is ApiError.Parse) return v2Res

        val v1Path = if (factionId != null && factionId > 0L) "faction/$factionId" else "faction/"
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = v1Path,
            queryParams = mapOf("selections" to "basic"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionBasic(raw) }
    }

    suspend fun getFactionChain(
        accountId: String,
        factionId: Long? = null,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornFactionChain> {
        val v2Path = if (factionId != null && factionId > 0L) "faction/$factionId/chain" else "faction/chain"
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = v2Path,
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionChain(raw) }
    }

    suspend fun getFactionAttacks(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornFactionAttack>> {
        val v2Res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/attacks",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionAttacks(raw) }

        if (v2Res is ApiResult.Success) return v2Res
        val err = (v2Res as ApiResult.Failure).error
        if (err !is ApiError.Unknown && err !is ApiError.Parse) return v2Res

        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = "faction/",
            queryParams = mapOf("selections" to "attacks"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionAttacks(raw) }
    }

    suspend fun getFactionArmory(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornArmoryItem>> {
        val v2Res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/inventory",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionArmory(raw) }

        if (v2Res is ApiResult.Success && v2Res.data.isNotEmpty()) return v2Res
        if (v2Res is ApiResult.Failure) {
            val err = v2Res.error
            if (err is ApiError.AccessLevel || err is ApiError.InvalidKey || err is ApiError.KeyPaused) {
                return v2Res
            }
        }

        val v1Res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = "faction/",
            queryParams = mapOf("selections" to "weapons,armor,medical,drugs,boosters,temporary,cesium"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionArmory(raw) }

        return if (v1Res is ApiResult.Success || v2Res !is ApiResult.Success) v1Res else v2Res
    }

    suspend fun getFactionBalance(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornFactionBalance> {
        val v2Res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/balance",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionBalance(raw) }

        if (v2Res is ApiResult.Success) return v2Res
        val err = (v2Res as ApiResult.Failure).error
        if (err !is ApiError.Unknown && err !is ApiError.Parse) return v2Res

        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V1,
            path = "faction/",
            queryParams = mapOf("selections" to "currency"),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionBalance(raw) }
    }

    suspend fun getFactionCrimes(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<List<TornOrganizedCrime>> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "faction/crimes",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapFactionCrimes(raw) }
    }

    suspend fun getUserOrganizedCrime(
        accountId: String,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornOrganizedCrime?> {
        val res = fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/organizedcrime",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapUserOrganizedCrime(raw) }
        return when (res) {
            is ApiResult.Success -> ApiResult.Success(
                data = res.data.crime,
                fetchedAtMs = res.fetchedAtMs,
                fromCache = res.fromCache,
                isStale = res.isStale
            )
            is ApiResult.Failure -> ApiResult.Failure(
                error = res.error,
                staleData = res.staleData?.crime,
                staleFetchedAtMs = res.staleFetchedAtMs
            )
        }
    }

    suspend fun getOtherProfile(
        accountId: String,
        targetPlayerId: Long,
        priority: RequestPriority,
        forceRefresh: Boolean = false
    ): ApiResult<TornOtherProfile> {
        return fetchAndMap(
            accountId = accountId,
            version = ApiVersion.V2,
            path = "user/$targetPlayerId/profile",
            queryParams = emptyMap(),
            priority = priority,
            forceRefresh = forceRefresh
        ) { raw -> TornMappers.mapOtherProfile(raw, targetPlayerId) }
    }

    suspend fun probeSelection(
        accountId: String,
        selection: String,
        paramId: String?
    ): ProbeResponse {
        val (version, path, query) = when (selection) {
            "user/basic" -> Triple(ApiVersion.V2, "user/basic", emptyMap<String, String>())
            "user/bars" -> Triple(ApiVersion.V2, "user/bars", emptyMap())
            "user/cooldowns" -> Triple(ApiVersion.V2, "user/cooldowns", emptyMap())
            "user/travel" -> Triple(ApiVersion.V2, "user/travel", emptyMap())
            "user/money" -> Triple(ApiVersion.V2, "user/money", emptyMap())
            "user/notifications" -> Triple(ApiVersion.V2, "user/notifications", emptyMap())
            "user/stocks" -> Triple(ApiVersion.V2, "user/stocks", emptyMap())
            "torn/stocks" -> Triple(ApiVersion.V2, "torn/stocks", emptyMap())
            "faction/{id}/rankedwars" -> {
                val id = paramId?.trim()?.takeIf { it.all(Char::isDigit) && it.isNotEmpty() } ?: "1"
                Triple(ApiVersion.V2, "faction/$id/rankedwars", emptyMap())
            }
            "faction/{id}/members" -> {
                val id = paramId?.trim()?.takeIf { it.all(Char::isDigit) && it.isNotEmpty() } ?: "1"
                Triple(ApiVersion.V2, "faction/$id/members", emptyMap())
            }
            "faction/basic" -> Triple(ApiVersion.V2, "faction/basic", emptyMap())
            "faction/chain" -> Triple(ApiVersion.V2, "faction/chain", emptyMap())
            "faction/attacks" -> Triple(ApiVersion.V2, "faction/attacks", emptyMap())
            "faction/inventory" -> Triple(ApiVersion.V2, "faction/inventory", emptyMap())
            "faction/balance" -> Triple(ApiVersion.V2, "faction/balance", emptyMap())
            "faction/crimes" -> Triple(ApiVersion.V2, "faction/crimes", emptyMap())
            "user/organizedcrime" -> Triple(ApiVersion.V2, "user/organizedcrime", emptyMap())
            "user/{id}/profile" -> {
                val id = paramId?.trim()?.takeIf { it.all(Char::isDigit) && it.isNotEmpty() } ?: "1"
                Triple(ApiVersion.V2, "user/$id/profile", emptyMap())
            }
            else -> Triple(ApiVersion.V2, "user/basic", emptyMap())
        }

        val apiKey = vault.get(SecretVault.apiKeyAlias(accountId))
            ?: return ProbeResponse(path, "", clock.lastServerDateHeader, clock.offsetMs(), ApiError.InvalidKey())

        return when (val res = executeDirect(accountId, apiKey, version, path, query, RequestPriority.USER, true)) {
            is ApiResult.Success -> ProbeResponse(
                path = path,
                rawRedactedJson = CrowLog.redact(res.data),
                httpDateHeader = clock.lastServerDateHeader,
                clockOffsetMs = clock.offsetMs()
            )
            is ApiResult.Failure -> ProbeResponse(
                path = path,
                rawRedactedJson = "",
                httpDateHeader = clock.lastServerDateHeader,
                clockOffsetMs = clock.offsetMs(),
                error = res.error
            )
        }
    }

    private suspend fun <T> fetchAndMap(
        accountId: String,
        version: ApiVersion,
        path: String,
        queryParams: Map<String, String>,
        priority: RequestPriority,
        forceRefresh: Boolean,
        mapper: (String) -> T?
    ): ApiResult<T> {
        val apiKey = vault.get(SecretVault.apiKeyAlias(accountId))
            ?: return ApiResult.Failure(ApiError.InvalidKey("Missing API key"))

        val key = CacheKey(accountId, "${version.name}/$path", queryParams)
        val rawResult = responseCache.getOrFetch(key = key, forceRefresh = forceRefresh) {
            executeDirect(accountId, apiKey, version, path, queryParams, priority, notifyOnKeyError = true)
        }

        return when (rawResult) {
            is ApiResult.Success -> {
                val mapped = mapper(rawResult.data)
                if (mapped != null) {
                    ApiResult.Success(
                        data = mapped,
                        fetchedAtMs = rawResult.fetchedAtMs,
                        fromCache = rawResult.fromCache,
                        isStale = rawResult.isStale
                    )
                } else {
                    ApiResult.Failure(ApiError.Parse())
                }
            }
            is ApiResult.Failure -> {
                val staleMapped = rawResult.staleData?.let { runCatching { mapper(it) }.getOrNull() }
                ApiResult.Failure(
                    error = rawResult.error,
                    staleData = staleMapped,
                    staleFetchedAtMs = rawResult.staleFetchedAtMs
                )
            }
        }
    }

    private suspend fun executeDirect(
        accountId: String,
        apiKey: String,
        version: ApiVersion,
        path: String,
        queryParams: Map<String, String>,
        priority: RequestPriority,
        notifyOnKeyError: Boolean
    ): ApiResult<String> {
        CrowLog.registerSecret(apiKey)

        when (val permit = rateLimiter.acquire(accountId, priority)) {
            is AcquireResult.Granted -> {}
            is AcquireResult.Denied -> return ApiResult.Failure(permit.reason)
        }

        val maxAttempts = if (priority == RequestPriority.USER) 2 else 1
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt++
            val result = performHttpCall(apiKey, version, path, queryParams)
            when (result) {
                is ApiResult.Success -> return result
                is ApiResult.Failure -> {
                    val err = result.error
                    if (err is ApiError.InvalidKey || err is ApiError.KeyPaused) {
                        if (notifyOnKeyError) {
                            val status = if (err is ApiError.KeyPaused) AccountStatus.PAUSED else AccountStatus.INVALID
                            onKeyRejected(accountId, status, err.message)
                        }
                        return result
                    }
                    if (err is ApiError.RateLimited) {
                        rateLimiter.triggerRateLimitBackoff()
                        return result
                    }
                    if (err is ApiError.IpBlocked) {
                        rateLimiter.triggerIpBlockPause()
                        return result
                    }
                    if (err is ApiError.AccessLevel || err is ApiError.ProviderDown || err is ApiError.Parse) {
                        return result
                    }
                    if ((err is ApiError.Network || err is ApiError.Timeout) && attempt < maxAttempts) {
                        delay(2_000L)
                        continue
                    }
                    return result
                }
            }
        }
        return ApiResult.Failure(ApiError.Network())
    }

    private suspend fun performHttpCall(
        apiKey: String,
        version: ApiVersion,
        path: String,
        queryParams: Map<String, String>
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val baseUrl = when (version) {
            ApiVersion.V2 -> "https://api.torn.com/v2/$path"
            ApiVersion.V1 -> "https://api.torn.com/$path"
        }

        val urlBuilder = StringBuilder(baseUrl)
        val params = queryParams.toMutableMap()
        if (version == ApiVersion.V1) {
            params["key"] = apiKey
        }
        if (params.isNotEmpty()) {
            urlBuilder.append("?")
            urlBuilder.append(
                params.entries.joinToString("&") { (k, v) ->
                    "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
                }
            )
        }

        val reqBuilder = Request.Builder().url(urlBuilder.toString()).get()
        if (version == ApiVersion.V2) {
            // VERIFY: Torn API v2 Authorization: ApiKey header
            reqBuilder.header("Authorization", "ApiKey $apiKey")
        }

        try {
            okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val errDto = runCatching {
                    TornMappers.json.decodeFromString<TornErrorWrapperDto>(bodyStr)
                }.getOrNull()?.error

                if (errDto?.code != null) {
                    return@withContext ApiResult.Failure(mapTornErrorCode(errDto.code, errDto.error))
                }
                if (!response.isSuccessful) {
                    val mapped = when (response.code) {
                        401, 403 -> ApiError.InvalidKey("HTTP ${response.code}")
                        429 -> ApiError.RateLimited()
                        in 500..599 -> ApiError.ProviderDown("HTTP ${response.code}")
                        else -> ApiError.Unknown(response.code, "HTTP ${response.code}")
                    }
                    return@withContext ApiResult.Failure(mapped)
                }

                clock.recordServerDateHeader(response.header("Date"))
                ApiResult.Success(bodyStr, clock.nowEpochMs())
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout())
        } catch (ioe: IOException) {
            ApiResult.Failure(ApiError.Network(CrowLog.redact(ioe.message ?: "Network error")))
        } catch (t: Throwable) {
            ApiResult.Failure(ApiError.Unknown(null, CrowLog.redact(t.message ?: "Unexpected error")))
        }
    }

    companion object {
        enum class ApiVersion { V1, V2 }

        fun mapTornErrorCode(code: Int, rawMessage: String?): ApiError {
            val msg = CrowLog.redact(rawMessage ?: "Torn error $code")
            return when (code) {
                2, 13 -> ApiError.InvalidKey(msg)
                5 -> ApiError.RateLimited(msg)
                8 -> ApiError.IpBlocked(msg)
                9, 17 -> ApiError.ProviderDown(msg)
                10, 18 -> ApiError.KeyPaused(msg)
                16 -> ApiError.AccessLevel(msg)
                else -> ApiError.Unknown(code, msg)
            }
        }
    }
}
