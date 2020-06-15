package org.tasks.security

import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.tasks.Strings.isNullOrEmpty
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreEncryption @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

    fun encrypt(text: String): String? {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = getCipher(Cipher.ENCRYPT_MODE, iv)
        return try {
            val output = cipher.doFinal(text.toByteArray(ENCODING))
            val result = ByteArray(iv.size + output.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(output, 0, result, iv.size, output.size)
            Base64.encodeToString(result, Base64.DEFAULT)
        } catch (e: IllegalBlockSizeException) {
            Timber.e(e)
            null
        } catch (e: BadPaddingException) {
            Timber.e(e)
            null
        }
    }

    fun decrypt(text: String?): String? {
        if (isNullOrEmpty(text)) {
            return null
        }
        val decoded = Base64.decode(text, Base64.DEFAULT)
        val iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH)
        val cipher = getCipher(Cipher.DECRYPT_MODE, iv)
        return try {
            val decrypted = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.size - GCM_IV_LENGTH)
            String(decrypted, ENCODING)
        } catch (e: IllegalBlockSizeException) {
            Timber.e(e)
            ""
        } catch (e: BadPaddingException) {
            Timber.e(e)
            ""
        }
    }

    private fun getCipher(cipherMode: Int, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(cipherMode, secretKey, GCMParameterSpec(GCM_TAG_LENGTH * java.lang.Byte.SIZE, iv))
        return cipher
    }

    private val secretKey: SecretKey
        get() {
            val entry: KeyStore.Entry? = keyStore.getEntry(ALIAS, null)
            return (entry as KeyStore.SecretKeyEntry?)?.secretKey ?: generateNewKey()
        }

    @SuppressLint("TrulyRandom")
    private fun generateNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
                KeyGenParameterSpec.Builder(
                        ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setRandomizedEncryptionRequired(false)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build())
        return keyGenerator.generateKey()
    }

    init {
        keyStore.load(null)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "passwords"
        private val ENCODING = StandardCharsets.UTF_8
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
}