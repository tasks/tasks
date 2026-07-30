package org.tasks.http

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.tasks.security.KeyStoreEncryption
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal class EncryptedFile(
    private val file: File,
    private val encryption: KeyStoreEncryption,
) {
    private val logger = Logger.withTag("EncryptedFile")

    private val lock = lockFor(file)

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        val encrypted = synchronized(lock) {
            if (file.exists()) file.readText() else null
        } ?: return@withContext null
        encryption.decrypt(encrypted)?.takeIf { it.isNotBlank() }
    }

    suspend fun write(plain: String): Boolean {
        val encrypted = encryption.encrypt(plain)
        if (encrypted == null) {
            logger.e { "Failed to encrypt ${file.name}" }
            return false
        }
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                try {
                    val dir = file.parentFile?.also { it.mkdirs() }
                    val tmp = File.createTempFile("${file.name}.", ".tmp", dir)
                    try {
                        tmp.writeText(encrypted)
                        restrictPermissions(tmp)
                        try {
                            Files.move(
                                tmp.toPath(),
                                file.toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE,
                            )
                        } catch (_: Exception) {
                            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        true
                    } finally {
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to persist ${file.name}" }
                    false
                }
            }
        }
    }

    suspend fun <T> readList(serializer: KSerializer<T>): List<T> {
        val decoded = read() ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(serializer), decoded)
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse ${file.name}" }
            emptyList()
        }
    }

    suspend fun <T> writeList(serializer: KSerializer<T>, items: List<T>): Boolean =
        write(json.encodeToString(ListSerializer(serializer), items))

    private fun restrictPermissions(f: File) {
        f.setReadable(false, false)
        f.setReadable(true, true)
        f.setWritable(false, false)
        f.setWritable(true, true)
    }

    companion object {
        private val locks = ConcurrentHashMap<String, Any>()
        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        private fun lockFor(file: File): Any {
            val key = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            return locks.computeIfAbsent(key) { Any() }
        }
    }
}

internal fun cookieFile(dir: File, key: String?, prefix: String): File {
    val name = if (key.isNullOrEmpty()) prefix else "${prefix}_${key.toFileToken()}"
    return File(dir, "$name.enc")
}

private fun String.toFileToken(): String {
    val safe = buildString {
        this@toFileToken.forEach { c ->
            append(if (c.isLetterOrDigit() || c == '-' || c == '_') c else '_')
        }
    }
    return "${safe.take(32)}_${shortHash()}"
}

private fun String.shortHash(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .take(8)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
