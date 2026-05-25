package org.tasks.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupEncryptionHelper @Inject constructor() {
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val KEY_SPEC_ALGORITHM = "AES"
        private const val ITERATION_COUNT = 65536
        private const val KEY_LENGTH = 256
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val TAG_LENGTH = 128
    }

    fun encrypt(os: OutputStream, password: String): OutputStream {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)
        val secret = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(TAG_LENGTH, iv))

        os.write(salt)
        os.write(iv)

        return CipherOutputStream(os, cipher)
    }

    fun decrypt(inputStream: InputStream, password: String): InputStream {
        val salt = ByteArray(SALT_SIZE)
        inputStream.read(salt)
        val iv = ByteArray(IV_SIZE)
        inputStream.read(iv)

        val secret = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(TAG_LENGTH, iv))

        return CipherInputStream(inputStream, cipher)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_SPEC_ALGORITHM)
    }
}
