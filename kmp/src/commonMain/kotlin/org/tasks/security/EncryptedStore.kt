package org.tasks.security

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM

class EncryptedStore(
    private val path: Path,
    private val encryption: Encryption,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val logger = Logger.withTag("EncryptedStore")
    private val lock = Mutex()

    suspend fun read(): String? {
        val encrypted = lock.withLock {
            withContext(Dispatchers.IO) {
                if (fileSystem.exists(path)) fileSystem.read(path) { readUtf8() } else null
            }
        } ?: return null
        return encryption.decrypt(encrypted)?.takeIf { it.isNotBlank() }
    }

    suspend fun write(plain: String): Boolean {
        val encrypted = try {
            encryption.encrypt(plain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to encrypt ${path.name}" }
            return false
        }
        if (encrypted == null) {
            logger.e { "Failed to encrypt ${path.name}" }
            return false
        }
        return lock.withLock {
            withContext(Dispatchers.IO) {
                try {
                    path.parent?.let { fileSystem.createDirectories(it) }
                    val tmp = path.parent?.resolve("${path.name}.tmp") ?: path
                    fileSystem.write(tmp) { writeUtf8(encrypted) }
                    if (tmp != path) {
                        fileSystem.atomicMove(tmp, path)
                    }
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Failed to persist ${path.name}" }
                    false
                }
            }
        }
    }

    fun exists(): Boolean = fileSystem.exists(path)

    suspend fun delete(): Boolean = lock.withLock {
        withContext(Dispatchers.IO) {
            try {
                fileSystem.delete(path, mustExist = false)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete ${path.name}" }
                false
            }
        }
    }
}
