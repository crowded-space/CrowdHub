package app.crowdhub.core.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray
)

class KeystoreCipher(
    private val keyAlias: String = MASTER_KEY_ALIAS
) {
    fun encrypt(plaintext: ByteArray): Result<EncryptedPayload> = runCatching {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(plaintext)
        EncryptedPayload(iv = cipher.iv, ciphertext = ciphertext)
    }

    fun decrypt(iv: ByteArray, ciphertext: ByteArray): Result<ByteArray> = runCatching {
        val secretKey = getExistingMasterKey()
            ?: error("Master key missing in AndroidKeyStore")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(ciphertext)
    }

    private fun getExistingMasterKey(): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(keyAlias, null) as? SecretKey
    }

    private fun getOrCreateMasterKey(): SecretKey {
        getExistingMasterKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "crowdhub_master_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
