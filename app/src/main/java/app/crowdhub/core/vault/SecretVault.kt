package app.crowdhub.core.vault

import app.crowdhub.core.db.SecretBlobDao
import app.crowdhub.core.db.SecretBlobEntity
import app.crowdhub.core.network.Clock
import app.crowdhub.core.network.CrowLog
import java.nio.charset.StandardCharsets

sealed class VaultResult<out T> {
    data class Ok<T>(val value: T) : VaultResult<T>()
    data class KeystoreError(val message: String) : VaultResult<Nothing>()
}

class SecretVault(
    private val dao: SecretBlobDao,
    private val cipher: KeystoreCipher,
    private val clock: Clock
) {
    suspend fun put(alias: String, value: String): VaultResult<Unit> {
        val encrypted = cipher.encrypt(value.toByteArray(StandardCharsets.UTF_8))
        return encrypted.fold(
            onSuccess = { payload ->
                dao.put(
                    SecretBlobEntity(
                        alias = alias,
                        iv = payload.iv,
                        ciphertext = payload.ciphertext,
                        updatedAt = clock.nowEpochMs()
                    )
                )
                CrowLog.registerSecret(value)
                VaultResult.Ok(Unit)
            },
            onFailure = {
                CrowLog.e(message = "Keystore encryption failed for alias=$alias")
                VaultResult.KeystoreError("Secure storage unavailable on this device")
            }
        )
    }

    suspend fun get(alias: String): String? {
        val blob = dao.get(alias) ?: return null
        val decrypted = cipher.decrypt(blob.iv, blob.ciphertext)
        return decrypted.fold(
            onSuccess = { bytes ->
                val value = String(bytes, StandardCharsets.UTF_8)
                CrowLog.registerSecret(value)
                value
            },
            onFailure = {
                CrowLog.w(message = "Keystore decryption failed for alias=$alias; treating secret as missing")
                null
            }
        )
    }

    suspend fun delete(alias: String) {
        val current = get(alias)
        if (current != null) {
            CrowLog.unregisterSecret(current)
        }
        dao.delete(alias)
    }

    suspend fun deleteByPrefix(prefix: String) {
        dao.deleteByPrefix(prefix)
    }

    suspend fun has(alias: String): Boolean {
        return dao.count(alias) > 0
    }

    companion object {
        fun apiKeyAlias(accountId: String) = "acct:$accountId:api_key"
        fun ffApiKeyAlias(accountId: String) = "acct:$accountId:ff_api_key"
        fun webUserAlias(accountId: String) = "acct:$accountId:web_user"
        fun webPassAlias(accountId: String) = "acct:$accountId:web_pass"
        fun accountPrefix(accountId: String) = "acct:$accountId:"

        fun maskSecret(secret: String?): String {
            if (secret.isNullOrBlank()) return "Not set"
            val tail = if (secret.length >= 4) secret.takeLast(4) else secret
            return "••••••••$tail"
        }
    }
}
