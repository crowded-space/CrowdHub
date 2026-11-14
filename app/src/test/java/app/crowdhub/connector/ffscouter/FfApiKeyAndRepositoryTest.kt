package app.crowdhub.connector.ffscouter

import app.crowdhub.core.db.ConnectorLinkDao
import app.crowdhub.core.db.ConnectorLinkEntity
import app.crowdhub.core.db.FfCacheDao
import app.crowdhub.core.db.FfCacheEntity
import app.crowdhub.core.db.SecretBlobDao
import app.crowdhub.core.db.SecretBlobEntity
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.network.ApiResult
import app.crowdhub.core.network.Clock
import app.crowdhub.core.vault.EncryptedPayload
import app.crowdhub.core.vault.KeystoreCipher
import app.crowdhub.core.vault.SecretVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FfApiKeyAndRepositoryTest {

    private class FakeClock(private var now: Long = 1_800_000_000_000L) : Clock {
        override fun nowEpochMs(): Long = now
        override fun offsetMs(): Long = 0L
    }

    private class PassThroughCipher : KeystoreCipher("test_alias") {
        override fun encrypt(plaintext: ByteArray): Result<EncryptedPayload> =
            Result.success(EncryptedPayload(iv = byteArrayOf(1, 2, 3), ciphertext = plaintext.copyOf()))

        override fun decrypt(iv: ByteArray, ciphertext: ByteArray): Result<ByteArray> =
            Result.success(ciphertext.copyOf())
    }

    private class InMemorySecretBlobDao : SecretBlobDao {
        private val map = linkedMapOf<String, SecretBlobEntity>()

        override suspend fun get(alias: String): SecretBlobEntity? = map[alias]
        override suspend fun count(alias: String): Int = if (map.containsKey(alias)) 1 else 0
        override suspend fun put(blob: SecretBlobEntity) {
            map[blob.alias] = blob
        }
        override suspend fun delete(alias: String) {
            map.remove(alias)
        }
        override suspend fun deleteByPrefix(prefix: String) {
            val keys = map.keys.filter { it.startsWith(prefix) }
            keys.forEach { map.remove(it) }
        }
    }

    private class InMemoryConnectorLinkDao : ConnectorLinkDao {
        private val state = MutableStateFlow<Map<Pair<String, String>, ConnectorLinkEntity>>(emptyMap())

        override fun observe(accountId: String, connectorId: String): Flow<ConnectorLinkEntity?> =
            state.map { it[accountId to connectorId] }

        override suspend fun get(accountId: String, connectorId: String): ConnectorLinkEntity? =
            state.value[accountId to connectorId]

        override suspend fun upsert(link: ConnectorLinkEntity) {
            state.value = state.value + ((link.accountId to link.connectorId) to link)
        }

        override suspend fun deleteForAccount(accountId: String) {
            state.value = state.value.filterKeys { it.first != accountId }
        }
    }

    private class InMemoryFfCacheDao : FfCacheDao {
        val rows = linkedMapOf<Long, FfCacheEntity>()

        override suspend fun getByIds(playerIds: List<Long>): List<FfCacheEntity> =
            playerIds.mapNotNull { rows[it] }

        override suspend fun upsertAll(entries: List<FfCacheEntity>) {
            entries.forEach { rows[it.playerId] = it }
        }

        override suspend fun clearAll() {
            rows.clear()
        }
    }

    private fun buildClient(handler: (String) -> Pair<Int, String>): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val (code, body) = handler(request.url.toString())
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code in 200..299) "OK" else "Error")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    @Test
    fun `ffApiKeyAlias shares account prefix for cleanup on account deletion`() = runTest {
        val vault = SecretVault(InMemorySecretBlobDao(), PassThroughCipher(), FakeClock())
        vault.put(SecretVault.apiKeyAlias("acct-1"), "torn_key_1234")
        vault.put(SecretVault.ffApiKeyAlias("acct-1"), "ff_key_9999")
        vault.put(SecretVault.ffApiKeyAlias("acct-2"), "ff_other_8888")

        assertTrue(SecretVault.ffApiKeyAlias("acct-1").startsWith(SecretVault.accountPrefix("acct-1")))
        vault.deleteByPrefix(SecretVault.accountPrefix("acct-1"))

        assertNull(vault.get(SecretVault.apiKeyAlias("acct-1")))
        assertNull(vault.get(SecretVault.ffApiKeyAlias("acct-1")))
        assertEquals("ff_other_8888", vault.get(SecretVault.ffApiKeyAlias("acct-2")))
    }

    @Test
    fun `FfApi prefers custom FFScouter key over Torn key and falls back cleanly`() = runTest {
        val clock = FakeClock()
        val vault = SecretVault(InMemorySecretBlobDao(), PassThroughCipher(), clock)
        vault.put(SecretVault.apiKeyAlias("acct-1"), "TORN_KEY_ABC1")

        val requestedKeys = mutableListOf<String?>()
        val client = buildClient { url ->
            val keyParam = Regex("[?&]key=([^&]+)").find(url)?.groupValues?.get(1)
            requestedKeys.add(keyParam)
            200 to """[{"player_id":3962657,"fair_fight":3.54,"bs_estimate":321502,"bs_estimate_human":"322k"}]"""
        }
        val api = FfApi(client, FfRateLimiter(clock), vault, clock)

        // Falls back to Torn key when no custom FF key is set
        val fallbackRes = api.getStats("acct-1", listOf(3962657L))
        assertTrue(fallbackRes is ApiResult.Success)
        assertEquals("TORN_KEY_ABC1", requestedKeys.last())

        // Prefers custom FFScouter key once stored
        vault.put(SecretVault.ffApiKeyAlias("acct-1"), "FF_CUSTOM_XYZ9")
        val customRes = api.getStats("acct-1", listOf(3962657L))
        assertTrue(customRes is ApiResult.Success)
        assertEquals("FF_CUSTOM_XYZ9", requestedKeys.last())

        // Override key wins over both stored keys
        val overrideRes = api.getStats("acct-1", listOf(3962657L), overrideKey = "OVERRIDE_5555")
        assertTrue(overrideRes is ApiResult.Success)
        assertEquals("OVERRIDE_5555", requestedKeys.last())
    }

    @Test
    fun `extractErrorMessage parses error payloads and ignores valid data responses`() {
        assertEquals(
            "Invalid API key",
            FfApi.extractErrorMessage("""{"error":"Invalid API key"}""")
        )
        assertEquals(
            "Not registered",
            FfApi.extractErrorMessage("""{"message":"Not registered"}""")
        )
        assertNull(
            FfApi.extractErrorMessage("""{"targets":[{"player_id":1}],"error":null}""")
        )
        assertNull(
            FfApi.extractErrorMessage("""[{"player_id":1,"fair_fight":2.1}]""")
        )
    }

    @Test
    fun `saveCustomKeyAndEnable validates before persisting and updates key info`() = runTest {
        val clock = FakeClock()
        val vault = SecretVault(InMemorySecretBlobDao(), PassThroughCipher(), clock)
        vault.put(SecretVault.apiKeyAlias("acct-1"), "TORN_KEY_YeVX")
        val linkDao = InMemoryConnectorLinkDao()
        val cacheDao = InMemoryFfCacheDao()

        val client = buildClient { url ->
            val keyParam = Regex("[?&]key=([^&]+)").find(url)?.groupValues?.get(1)
            when (keyParam) {
                "VALID_FF_KEY_7788" ->
                    200 to """[{"player_id":3962657,"fair_fight":3.54,"bs_estimate":321502,"bs_estimate_human":"322k"}]"""
                "SOFT_ERROR_KEY" ->
                    200 to """{"error":"API key is not registered with FFScouter"}"""
                else ->
                    401 to """{"error":"Not registered at FFScouter"}"""
            }
        }
        val api = FfApi(client, FfRateLimiter(clock), vault, clock)
        val repo = FfRepository(linkDao, cacheDao, api, vault, clock)

        // Rejected 401 key does NOT save or enable
        val badRes = repo.saveCustomKeyAndEnable("acct-1", 3962657L, "BAD_KEY")
        assertTrue(badRes is ApiResult.Failure)
        assertTrue((badRes as ApiResult.Failure).error is ApiError.InvalidKey)
        assertFalse(repo.isConsentedAndEnabled("acct-1"))
        assertFalse(repo.getKeyInfo("acct-1").hasCustomKey)

        // HTTP 200 error payload also rejected cleanly
        val softErrRes = repo.saveCustomKeyAndEnable("acct-1", 3962657L, "SOFT_ERROR_KEY")
        assertTrue(softErrRes is ApiResult.Failure)
        assertEquals(
            "API key is not registered with FFScouter",
            (softErrRes as ApiResult.Failure).error.message
        )
        assertFalse(repo.isConsentedAndEnabled("acct-1"))

        // Valid custom key saves, enables, caches own stats, and updates FfKeyInfo
        val okRes = repo.saveCustomKeyAndEnable("acct-1", 3962657L, " VALID_FF_KEY_7788 ")
        assertTrue(okRes is ApiResult.Success)
        assertTrue(repo.isConsentedAndEnabled("acct-1"))
        val info = repo.getKeyInfo("acct-1")
        assertTrue(info.hasCustomKey)
        assertEquals("••••••••7788", info.maskedCustomKey)
        assertEquals("••••••••7788", info.maskedEffectiveKey)
        assertEquals(3.54, cacheDao.rows[3962657L]?.fairFight ?: 0.0, 0.001)

        // Removing custom key reverts cleanly to Torn fallback
        repo.removeCustomKey("acct-1")
        val afterRemove = repo.getKeyInfo("acct-1")
        assertFalse(afterRemove.hasCustomKey)
        assertTrue(afterRemove.hasTornFallback)
        assertEquals("••••••••YeVX", afterRemove.maskedEffectiveKey)
        assertTrue(cacheDao.rows.isEmpty())
    }
}
