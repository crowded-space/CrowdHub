package app.crowdhub.core.scripts

import app.crowdhub.core.db.ScriptKvEntity
import app.crowdhub.core.db.ScriptLogEntity
import app.crowdhub.core.db.UserscriptDao
import app.crowdhub.core.db.UserscriptEntity
import app.crowdhub.core.network.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

sealed class ScriptInstallResult {
    data class Success(val script: UserscriptEntity, val warnings: List<String>) : ScriptInstallResult()
    data class Error(val message: String) : ScriptInstallResult()
}

class ScriptRepository(
    private val dao: UserscriptDao,
    private val okHttpClient: OkHttpClient,
    private val clock: Clock
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeScripts(accountId: String): Flow<List<UserscriptEntity>> =
        dao.observeForAccount(accountId)

    fun observeLogs(scriptId: String): Flow<List<ScriptLogEntity>> =
        dao.observeLogs(scriptId)

    suspend fun getEnabledScripts(accountId: String): List<UserscriptEntity> =
        dao.getEnabledForAccount(accountId)

    suspend fun getById(id: String): UserscriptEntity? = dao.getById(id)

    suspend fun fetchSourceFromUrl(rawUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = rawUrl.trim()
            require(clean.startsWith("https://", ignoreCase = true)) {
                "Only HTTPS script URLs are supported"
            }
            val request = Request.Builder().url(clean).get().build()
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code} fetching script")
                }
                val body = resp.body?.string() ?: error("Empty response body")
                require(body.toByteArray().size <= MAX_SCRIPT_BYTES) {
                    "Script exceeds 500 KB limit"
                }
                body
            }
        }
    }

    suspend fun installOrUpdate(
        existingId: String?,
        accountId: String,
        source: String,
        allowApiKey: Boolean,
        installedFromUrl: String?,
        allowedHosts: Set<String>
    ): ScriptInstallResult {
        if (source.toByteArray().size > MAX_SCRIPT_BYTES) {
            return ScriptInstallResult.Error("Script exceeds 500 KB size limit")
        }
        val meta = UserscriptMetaParser.parse(source, allowedHosts)
        val now = clock.nowEpochMs()
        val existing = existingId?.let { dao.getById(it) }
        val entity = UserscriptEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            accountId = accountId,
            name = meta.name,
            namespace = meta.namespace,
            version = meta.version,
            description = meta.description,
            source = source,
            matches = json.encodeToString(meta.matches),
            excludes = json.encodeToString(meta.excludes),
            runAt = meta.runAt.name,
            grants = json.encodeToString(meta.grants),
            connects = json.encodeToString(meta.connects),
            allowApiKey = allowApiKey,
            enabled = existing?.enabled ?: true,
            installedFromUrl = installedFromUrl ?: existing?.installedFromUrl,
            installedAt = existing?.installedAt ?: now,
            updatedAt = now,
            lastError = null
        )
        dao.upsert(entity)
        return ScriptInstallResult.Success(entity, meta.warnings)
    }

    suspend fun setEnabled(scriptId: String, enabled: Boolean) {
        dao.setEnabled(scriptId, enabled, clock.nowEpochMs())
    }

    suspend fun deleteScript(scriptId: String) {
        dao.clearLogsForScript(scriptId)
        dao.clearKvForScript(scriptId)
        dao.deleteById(scriptId)
    }

    suspend fun clearScriptStorage(scriptId: String) {
        dao.clearKvForScript(scriptId)
    }

    suspend fun getAllKv(scriptId: String): Map<String, String> {
        return dao.getAllKv(scriptId).associate { it.key to it.value }
    }

    suspend fun setKv(scriptId: String, key: String, value: String) {
        dao.putKv(ScriptKvEntity(scriptId, key, value))
    }

    suspend fun deleteKv(scriptId: String, key: String) {
        dao.deleteKv(scriptId, key)
    }

    suspend fun appendLog(scriptId: String, level: String, message: String) {
        val now = clock.nowEpochMs()
        dao.insertLog(ScriptLogEntity(scriptId = scriptId, ts = now, level = level, message = message))
        dao.trimLogs(scriptId)
        if (level.equals("ERROR", ignoreCase = true)) {
            dao.setLastError(scriptId, message.take(240), now)
        }
    }

    fun decodeList(jsonStr: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(jsonStr) }.getOrDefault(emptyList())

    companion object {
        const val MAX_SCRIPT_BYTES = 500 * 1024
    }
}
