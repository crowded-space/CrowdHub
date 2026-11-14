package app.crowdhub.core.scripts

import android.net.Uri
import android.webkit.WebView
import app.crowdhub.core.network.AcquireResult
import app.crowdhub.core.network.CrowLog
import app.crowdhub.core.network.RateLimiter
import app.crowdhub.core.network.RequestPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
private data class BridgeMessage(
    val type: String,
    val scriptId: String,
    val reqId: String? = null,
    val key: String? = null,
    val value: String? = null,
    val level: String? = null,
    val message: String? = null,
    val method: String? = null,
    val url: String? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null
)

@Serializable
private data class BridgeHttpResponse(
    val reqId: String,
    val ok: Boolean,
    val status: Int,
    val responseText: String = "",
    val error: String? = null
)

class GmBridge(
    private val scriptRepository: ScriptRepository,
    private val rateLimiter: RateLimiter,
    private val okHttpClient: OkHttpClient,
    private val blockedWebHosts: Set<String>,
    private val apiHost: String = "api.torn.com",
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val bridgeClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun handleWebMessage(view: WebView, accountId: String, rawPayload: String) {
        val msg = runCatching { json.decodeFromString<BridgeMessage>(rawPayload) }.getOrNull() ?: return
        scope.launch {
            when (msg.type) {
                "kv_set" -> {
                    val k = msg.key ?: return@launch
                    val v = msg.value ?: ""
                    scriptRepository.setKv(msg.scriptId, k, v)
                }
                "kv_del" -> {
                    val k = msg.key ?: return@launch
                    scriptRepository.deleteKv(msg.scriptId, k)
                }
                "log" -> {
                    scriptRepository.appendLog(
                        scriptId = msg.scriptId,
                        level = msg.level ?: "INFO",
                        message = CrowLog.redact(msg.message ?: "")
                    )
                }
                "http" -> {
                    val reqId = msg.reqId ?: return@launch
                    val resp = executeHttpRequest(accountId, msg)
                    val encoded = json.encodeToString(resp)
                    withContext(Dispatchers.Main) {
                        val js = "window.__crowdBridgeDispatch && window.__crowdBridgeDispatch(${json.encodeToString(encoded)});"
                        view.evaluateJavascript(js, null)
                    }
                }
            }
        }
    }

    private suspend fun executeHttpRequest(accountId: String, msg: BridgeMessage): BridgeHttpResponse {
        val reqId = msg.reqId ?: ""
        val rawUrl = msg.url?.trim() ?: return BridgeHttpResponse(reqId, false, 0, error = "Missing URL")
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull()
            ?: return BridgeHttpResponse(reqId, false, 0, error = "Invalid URL")

        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return BridgeHttpResponse(reqId, false, 0, error = "https_only")
        }

        val host = uri.host?.lowercase()
            ?: return BridgeHttpResponse(reqId, false, 0, error = "Missing host")

        if (blockedWebHosts.any { it.equals(host, ignoreCase = true) }) {
            return BridgeHttpResponse(reqId, false, 0, error = "blocked_host")
        }

        val script = scriptRepository.getById(msg.scriptId)
            ?: return BridgeHttpResponse(reqId, false, 0, error = "script_not_found")
        val connects = scriptRepository.decodeList(script.connects).map { it.lowercase() }
        val hostAllowed = host.equals(apiHost, ignoreCase = true) ||
            connects.contains("*") ||
            connects.any { it == host || host.endsWith(".$it") }
        if (!hostAllowed) {
            return BridgeHttpResponse(reqId, false, 0, error = "connect_denied")
        }

        if (host.equals(apiHost, ignoreCase = true)) {
            when (rateLimiter.acquire(accountId, RequestPriority.SCRIPT)) {
                is AcquireResult.Granted -> {}
                is AcquireResult.Denied -> {
                    return BridgeHttpResponse(reqId, false, 429, responseText = """{"error":"rate_limited"}""", error = "rate_limited")
                }
            }
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val method = (msg.method ?: "GET").uppercase()
                val reqBuilder = Request.Builder().url(rawUrl)
                msg.headers?.forEach { (k, v) ->
                    if (!k.equals("Host", ignoreCase = true)) {
                        reqBuilder.header(k, v)
                    }
                }
                if (method == "POST" || method == "PUT" || method == "PATCH") {
                    val mediaType = msg.headers?.entries
                        ?.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                        ?.value
                        ?.toMediaTypeOrNull()
                        ?: "application/json; charset=utf-8".toMediaTypeOrNull()
                    reqBuilder.method(method, (msg.body ?: "").toRequestBody(mediaType))
                } else {
                    reqBuilder.get()
                }

                bridgeClient.newCall(reqBuilder.build()).execute().use { response ->
                    val bodyStream = response.body?.byteStream()
                    val bytes = bodyStream?.let { stream ->
                        val buffer = ByteArray(8192)
                        val out = java.io.ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = stream.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > MAX_RESPONSE_BYTES) {
                                return@use BridgeHttpResponse(
                                    reqId = reqId,
                                    ok = false,
                                    status = response.code,
                                    error = "response_too_large"
                                )
                            }
                            out.write(buffer, 0, read)
                        }
                        out.toString("UTF-8")
                    } ?: ""

                    BridgeHttpResponse(
                        reqId = reqId,
                        ok = response.isSuccessful,
                        status = response.code,
                        responseText = bytes
                    )
                }
            }.getOrElse { err ->
                BridgeHttpResponse(
                    reqId = reqId,
                    ok = false,
                    status = 0,
                    error = CrowLog.redact(err.message ?: "network_error")
                )
            }
        }
    }

    companion object {
        const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }
}
