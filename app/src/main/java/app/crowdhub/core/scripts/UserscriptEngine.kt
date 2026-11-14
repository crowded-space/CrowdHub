package app.crowdhub.core.scripts

import android.net.Uri
import android.webkit.WebView
import app.crowdhub.core.db.UserscriptEntity
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.vault.SecretVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class MenuCommandItem(
    val scriptId: String,
    val scriptName: String,
    val label: String
)

class UserscriptEngine(
    private val scriptRepository: ScriptRepository,
    private val settingsStore: SettingsStore,
    private val secretVault: SecretVault,
    private val allowedWebHosts: Set<String>,
    private val scope: CoroutineScope
) {
    private val json = Json

    fun buildBootstrapBridgeJs(): String = """
        (function() {
            if (window.__crowdShimInstalled) return;
            window.__crowdShimInstalled = true;
            window.__crowdPending = window.__crowdPending || {};
            window.__crowdMenus = [];
            window.__crowdBridgeDispatch = function(raw) {
                try {
                    var resp = typeof raw === 'string' ? JSON.parse(raw) : raw;
                    var cb = window.__crowdPending[resp.reqId];
                    if (cb) {
                        delete window.__crowdPending[resp.reqId];
                        cb(resp);
                    }
                } catch (e) {}
            };
        })();
    """.trimIndent()

    fun injectForStage(
        webView: WebView,
        accountId: String,
        url: String,
        stage: RunAt
    ) {
        scope.launch {
            val prefs = settingsStore.current()
            if (prefs.scriptsKillSwitch) return@launch

            val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return@launch
            if (allowedWebHosts.none { it.equals(host, ignoreCase = true) }) return@launch

            val scripts = scriptRepository.getEnabledScripts(accountId)
            val matching = scripts.filter { script ->
                RunAt.valueOf(script.runAt) == stage &&
                    UserscriptMetaParser.matchesUrl(
                        url = url,
                        matches = scriptRepository.decodeList(script.matches),
                        excludes = scriptRepository.decodeList(script.excludes)
                    )
            }
            if (matching.isEmpty()) return@launch

            if (stage == RunAt.DOCUMENT_IDLE) {
                delay(500L)
            }

            val apiKey = secretVault.get(SecretVault.apiKeyAlias(accountId))
            for (script in matching) {
                val kv = scriptRepository.getAllKv(script.id)
                val wrapped = wrapScript(script, kv, apiKey)
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(buildBootstrapBridgeJs() + "\n" + wrapped, null)
                }
            }
        }
    }

    private fun wrapScript(
        script: UserscriptEntity,
        initialKv: Map<String, String>,
        apiKey: String?
    ): String {
        val kvJson = json.encodeToString(initialKv)
        val infoJson = json.encodeToString(
            mapOf(
                "script" to mapOf(
                    "name" to script.name,
                    "namespace" to (script.namespace ?: ""),
                    "version" to (script.version ?: "1.0"),
                    "description" to (script.description ?: "")
                ),
                "scriptHandler" to "CrowdHub"
            ).mapValues { it.value.toString() }
        )

        val resolvedSource = if (script.allowApiKey && !apiKey.isNullOrBlank()) {
            script.source.replace("###PDA-APIKEY###", apiKey)
        } else {
            script.source
        }

        val scriptIdLiteral = json.encodeToString(script.id)
        val scriptNameLiteral = json.encodeToString(script.name)

        return """
            (function() {
                var __scriptId = $scriptIdLiteral;
                var __scriptName = $scriptNameLiteral;
                var __kv = $kvJson;
                function __post(obj) {
                    try {
                        if (window.__crowd && window.__crowd.postMessage) {
                            window.__crowd.postMessage(JSON.stringify(obj));
                        }
                    } catch (e) {}
                }
                function GM_getValue(k, defVal) {
                    return Object.prototype.hasOwnProperty.call(__kv, k) ? JSON.parse(__kv[k]) : defVal;
                }
                function GM_setValue(k, v) {
                    var serialized = JSON.stringify(v === undefined ? null : v);
                    __kv[k] = serialized;
                    __post({ type: 'kv_set', scriptId: __scriptId, key: String(k), value: serialized });
                }
                function GM_deleteValue(k) {
                    delete __kv[k];
                    __post({ type: 'kv_del', scriptId: __scriptId, key: String(k) });
                }
                function GM_listValues() {
                    return Object.keys(__kv);
                }
                function GM_addStyle(css) {
                    var style = document.createElement('style');
                    style.textContent = css;
                    (document.head || document.documentElement).appendChild(style);
                    return style;
                }
                function GM_log() {
                    var msg = Array.prototype.slice.call(arguments).map(function(x){ return String(x); }).join(' ');
                    __post({ type: 'log', scriptId: __scriptId, level: 'INFO', message: msg });
                }
                function GM_registerMenuCommand(label, fn) {
                    window.__crowdMenus = window.__crowdMenus || [];
                    window.__crowdMenus.push({ scriptId: __scriptId, scriptName: __scriptName, label: String(label), fn: fn });
                }
                var GM_info = $infoJson;
                var unsafeWindow = window;
                function GM_xmlhttpRequest(details) {
                    var reqId = __scriptId + '_' + Date.now() + '_' + Math.random().toString(36).slice(2);
                    return new Promise(function(resolve, reject) {
                        window.__crowdPending = window.__crowdPending || {};
                        window.__crowdPending[reqId] = function(resp) {
                            var out = {
                                status: resp.status || 0,
                                responseText: resp.responseText || '',
                                response: resp.responseText || '',
                                error: resp.error || null
                            };
                            if (resp.ok) {
                                if (details && details.onload) details.onload(out);
                                resolve(out);
                            } else {
                                if (details && details.onerror) details.onerror(out);
                                reject(out);
                            }
                        };
                        __post({
                            type: 'http',
                            scriptId: __scriptId,
                            reqId: reqId,
                            method: (details && details.method) || 'GET',
                            url: (details && details.url) || '',
                            headers: (details && details.headers) || {},
                            body: (details && (details.data || details.body)) || null
                        });
                    });
                }
                function PDA_httpGet(url, headers) {
                    return GM_xmlhttpRequest({ method: 'GET', url: url, headers: headers || {} });
                }
                function PDA_httpPost(url, headers, body) {
                    return GM_xmlhttpRequest({ method: 'POST', url: url, headers: headers || {}, data: body || '' });
                }
                var GM = {
                    info: GM_info,
                    getValue: function(k, d) { return Promise.resolve(GM_getValue(k, d)); },
                    setValue: function(k, v) { GM_setValue(k, v); return Promise.resolve(); },
                    deleteValue: function(k) { GM_deleteValue(k); return Promise.resolve(); },
                    listValues: function() { return Promise.resolve(GM_listValues()); },
                    xmlHttpRequest: GM_xmlhttpRequest
                };
                try {
                    $resolvedSource
                } catch (err) {
                    __post({ type: 'log', scriptId: __scriptId, level: 'ERROR', message: String(err && err.stack ? err.stack : err) });
                }
            })();
        """.trimIndent()
    }
}
