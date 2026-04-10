package org.tasks.security

import co.touchlab.kermit.Logger
import com.github.javakeyring.Keyring
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class DesktopKeyProvider(
    private val serviceName: String,
    private val accountName: String,
    private val fallbackKeyFile: File,
) : KeyProvider {
    private val logger = Logger.withTag("DesktopKeyProvider")

    @Volatile
    private var cachedKey: SecretKey? = null

    override fun getKey(): SecretKey {
        cachedKey?.let { return it }
        return synchronized(this) {
            cachedKey ?: loadKey().also { cachedKey = it }
        }
    }

    private fun loadKey(): SecretKey {
        val encoded = readFromKeychain()
            ?: readFromFile()
            ?: generateKey().also { storeKey(it) }
        return SecretKeySpec(Base64.getDecoder().decode(encoded), "AES")
    }

    private fun readFromKeychain(): String? = try {
        Keyring.create().use { it.getPassword(serviceName, accountName) }
    } catch (e: Exception) {
        logger.w(e) { "Failed to read from OS keychain" }
        null
    }

    private fun readFromFile(): String? =
        fallbackKeyFile.takeIf { it.exists() }?.readText()?.trim()

    private fun generateKey(): String =
        ByteArray(32)
            .also { SecureRandom().nextBytes(it) }
            .let { Base64.getEncoder().encodeToString(it) }

    private fun storeKey(key: String) {
        try {
            Keyring.create().use { it.setPassword(serviceName, accountName, key) }
        } catch (e: Exception) {
            logger.w(e) { "OS keychain unavailable, falling back to file" }
            fallbackKeyFile.parentFile?.mkdirs()
            fallbackKeyFile.writeText(key)
            fallbackKeyFile.setReadable(false, false)
            fallbackKeyFile.setReadable(true, true)
            fallbackKeyFile.setWritable(false, false)
            fallbackKeyFile.setWritable(true, true)
            fallbackKeyFile.setExecutable(false, false)
        }
    }
}
