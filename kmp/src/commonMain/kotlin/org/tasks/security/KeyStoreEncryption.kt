package org.tasks.security

import co.touchlab.kermit.Logger
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.spec.GCMParameterSpec

open class KeyStoreEncryption(
    private val keyProvider: KeyProvider
) {
    protected val logger = Logger.withTag("KeyStoreEncryption")

    fun encrypt(text: String): String? {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = getCipher(Cipher.ENCRYPT_MODE, iv)
        return try {
            val output = cipher.doFinal(text.toByteArray(ENCODING))
            val result = ByteArray(iv.size + output.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(output, 0, result, iv.size, output.size)
            Base64.getEncoder().encodeToString(result)
        } catch (e: IllegalBlockSizeException) {
            logger.e(e) { "Failed to encrypt data" }
            null
        } catch (e: BadPaddingException) {
            logger.e(e) { "Failed to encrypt data" }
            null
        }
    }

    fun decrypt(text: String?): String? {
        if (text.isNullOrBlank()) {
            return null
        }
        val decoded = decodeBase64(text)
        if (decoded.isEmpty()) {
            return ""
        }
        val iv = decoded.copyOfRange(0, GCM_IV_LENGTH)
        val cipher = getCipher(Cipher.DECRYPT_MODE, iv)
        return try {
            val decrypted = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.size - GCM_IV_LENGTH)
            String(decrypted, ENCODING)
        } catch (e: IllegalBlockSizeException) {
            logger.e(e) { "Failed to decrypt data" }
            ""
        } catch (e: BadPaddingException) {
            logger.e(e) { "Failed to decrypt data" }
            ""
        }
    }

    protected open fun decodeBase64(text: String): ByteArray {
        return try {
            Base64.getDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            logger.e(e) { "Failed to decode Base64 data" }
            ByteArray(0)
        }
    }

    private fun getCipher(cipherMode: Int, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(cipherMode, keyProvider.getKey(), GCMParameterSpec(GCM_TAG_LENGTH * java.lang.Byte.SIZE, iv))
        return cipher
    }

    companion object {
        private val ENCODING = StandardCharsets.UTF_8
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
}
