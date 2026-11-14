package app.crowdhub.connector.ffscouter

import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.CrowLog
import app.crowdhub.core.vault.SecretVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder

data class FfTargetsFilterParams(
    val minLevel: Int? = null,
    val maxLevel: Int? = null,
    val minFf: Double? = null,
    val maxFf: Double? = null,
    val limit: Int = 50,
    val inactiveOnly: Boolean = false,
    val factionlessOnly: Boolean = false
)

class FfApi(
    private val okHttpClient: OkHttpClient,
    private val rateLimiter: FfRateLimiter,
    private val vault: SecretVault,
    private val clock: Clock
) {

    suspend fun resolveApiKey(accountId: String, overrideKey: String? = null): String? {
        val candidate = overrideKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: vault.get(SecretVault.ffApiKeyAlias(accountId))?.trim()?.takeIf { it.isNotEmpty() }
            ?: vault.get(SecretVault.apiKeyAlias(accountId))?.trim()?.takeIf { it.isNotEmpty() }
        if (candidate != null) {
            CrowLog.registerSecret(candidate)
        }
        return candidate
    }

    suspend fun verifyKey(
        accountId: String,
        ownPlayerId: Long?,
        overrideKey: String? = null
    ): ApiResult<List<FfStats>> {
        val probeId = ownPlayerId?.takeIf { it > 0L } ?: PROBE_PLAYER_ID
        return getStats(
            accountId = accountId,
            playerIds = listOf(probeId),
            overrideKey = overrideKey
        )
    }

    suspend fun getStats(
        accountId: String,
        playerIds: List<Long>,
        overrideKey: String? = null
    ): ApiResult<List<FfStats>> {
        if (playerIds.isEmpty()) return ApiResult.Success(emptyList(), clock.nowEpochMs())
        val apiKey = resolveApiKey(accountId, overrideKey)
            ?: return ApiResult.Failure(
                ApiError.InvalidKey("Missing FFScouter or Torn API key")
            )

        rateLimiter.tryAcquire()?.let { return ApiResult.Failure(it) }

        val targetsParam = playerIds.joinToString(",")
        val url = "${FfDefaults.BASE_URL}/get-stats?key=${enc(apiKey)}&targets=${enc(targetsParam)}"

        return execute(url) { body ->
            parseStatsResponse(body, clock.nowEpochMs())
        }
    }

    suspend fun getTargets(
        accountId: String,
        filters: FfTargetsFilterParams,
        overrideKey: String? = null
    ): ApiResult<List<FfTargetCandidate>> {
        val apiKey = resolveApiKey(accountId, overrideKey)
            ?: return ApiResult.Failure(
                ApiError.InvalidKey("Missing FFScouter or Torn API key")
            )

        rateLimiter.tryAcquire()?.let { return ApiResult.Failure(it) }

        val params = buildList {
            add("key=${enc(apiKey)}")
            add("limit=${filters.limit.coerceIn(1, 50)}")
            filters.minLevel?.let { add("min_level=$it") }
            filters.maxLevel?.let { add("max_level=$it") }
            filters.minFf?.let { add("min_ff=$it") }
            filters.maxFf?.let { add("max_ff=$it") }
            if (filters.inactiveOnly) add("inactive_only=true")
            if (filters.factionlessOnly) add("factionless=true")
        }.joinToString("&")

        val url = "${FfDefaults.BASE_URL}/get-targets?$params"
        return execute(url) { body ->
            parseTargetsResponse(body)
        }
    }

    private suspend fun <T> execute(
        url: String,
        parser: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val serverError = extractErrorMessage(body)
                when (resp.code) {
                    400, 401, 403 -> {
                        val msg = serverError?.takeIf { it.isNotBlank() }
                            ?: "Not registered at FFScouter. Enter your FFScouter API key or register at ffscouter.com."
                        return@withContext ApiResult.Failure(ApiError.InvalidKey(msg))
                    }
                    429 -> {
                        rateLimiter.triggerBackoff()
                        return@withContext ApiResult.Failure(
                            ApiError.RateLimited(serverError ?: "FFScouter rate limited")
                        )
                    }
                    in 500..599 -> return@withContext ApiResult.Failure(
                        ApiError.ProviderDown(serverError ?: "FFScouter error ${resp.code}")
                    )
                }
                if (!resp.isSuccessful) {
                    return@withContext ApiResult.Failure(
                        ApiError.Unknown(resp.code, serverError ?: "FFScouter HTTP ${resp.code}")
                    )
                }
                if (serverError != null) {
                    return@withContext ApiResult.Failure(ApiError.InvalidKey(serverError))
                }
                val parsed = parser(body)
                ApiResult.Success(parsed, clock.nowEpochMs())
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout("FFScouter timed out"))
        } catch (ioe: IOException) {
            ApiResult.Failure(ApiError.Network(CrowLog.redact(ioe.message ?: "Network error")))
        } catch (t: Throwable) {
            ApiResult.Failure(ApiError.Parse(CrowLog.redact(t.message ?: "Parse error")))
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val PROBE_PLAYER_ID = 1L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        internal fun extractErrorMessage(raw: String): String? {
            if (raw.isBlank()) return null
            val el = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
            val obj = el as? JsonObject ?: return null
            if (obj["stats"] != null || obj["data"] != null || obj["targets"] != null) return null
            val rawMsg = (obj["error"] as? JsonPrimitive)?.contentOrNull
                ?: (obj["message"] as? JsonPrimitive)?.contentOrNull
                ?: (obj["detail"] as? JsonPrimitive)?.contentOrNull
                ?: return null
            val clean = CrowLog.redact(rawMsg.trim())
            return clean.takeIf { it.isNotEmpty() }
        }

        internal fun parseStatsResponse(raw: String, nowMs: Long): List<FfStats> {
            val el: JsonElement = json.parseToJsonElement(raw)
            val items: List<FfStatItemDto> = when (el) {
                is JsonArray -> el.mapNotNull { runCatching { json.decodeFromJsonElement<FfStatItemDto>(it) }.getOrNull() }
                is JsonObject -> {
                    val wrapper = runCatching { json.decodeFromJsonElement<FfStatsWrapperDto>(el) }.getOrNull()
                    wrapper?.stats ?: wrapper?.data ?: el.entries.mapNotNull { (k, v) ->
                        runCatching {
                            val dto = json.decodeFromJsonElement<FfStatItemDto>(v)
                            if (dto.playerId == null && dto.id == null) dto.copy(playerId = k.toLongOrNull()) else dto
                        }.getOrNull()
                    }
                }
                else -> emptyList()
            }
            return items.mapNotNull { item ->
                val pid = item.playerId ?: item.id ?: return@mapNotNull null
                FfStats(
                    playerId = pid,
                    fairFight = item.fairFight,
                    bsEstimate = item.bsEstimate,
                    bsHuman = item.bsHuman,
                    lastUpdatedSec = item.lastUpdated,
                    fetchedAtMs = nowMs,
                    isStale = false
                )
            }
        }

        internal fun parseTargetsResponse(raw: String): List<FfTargetCandidate> {
            val el: JsonElement = json.parseToJsonElement(raw)
            val items: List<FfTargetItemDto> = when (el) {
                is JsonArray -> el.mapNotNull { runCatching { json.decodeFromJsonElement<FfTargetItemDto>(it) }.getOrNull() }
                is JsonObject -> {
                    val wrapper = runCatching { json.decodeFromJsonElement<FfTargetsWrapperDto>(el) }.getOrNull()
                    wrapper?.targets ?: wrapper?.data ?: emptyList()
                }
                else -> emptyList()
            }
            return items.mapNotNull { item ->
                val pid = item.playerId ?: item.id ?: return@mapNotNull null
                FfTargetCandidate(
                    playerId = pid,
                    name = item.name,
                    level = item.level,
                    fairFight = item.fairFight,
                    bsEstimate = item.bsEstimate,
                    bsHuman = item.bsHuman,
                    lastUpdatedSec = item.lastUpdated ?: item.bssPublicTimestamp,
                    lastActionSec = item.lastAction
                )
            }
        }
    }
}
