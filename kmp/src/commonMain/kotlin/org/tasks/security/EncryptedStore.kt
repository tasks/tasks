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

enum class WriteResult { WRITTEN, ENCRYPT_FAILED, WRITE_FAILED }

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
        return withContext(Dispatchers.IO) {
            encryption.decrypt(encrypted)?.takeIf { it.isNotBlank() }
        }
    }

    suspend fun write(plain: String): WriteResult {
        val encrypted = try {
            withContext(Dispatchers.IO) { encryption.encrypt(plain) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to encrypt ${path.name}" }
            return WriteResult.ENCRYPT_FAILED
        }
        if (encrypted == null) {
            logger.e { "Failed to encrypt ${path.name}" }
            return WriteResult.ENCRYPT_FAILED
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
                    WriteResult.WRITTEN
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Failed to persist ${path.name}" }
                    WriteResult.WRITE_FAILED
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
