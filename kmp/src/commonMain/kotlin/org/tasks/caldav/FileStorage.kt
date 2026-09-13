package org.tasks.caldav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

class FileStorage(
    rootPath: String
) {
    val root: Path = rootPath.toPath() / "vtodo"

    fun getFile(vararg segments: String?): Path? =
        if (segments.none { it.isNullOrBlank() }) {
            segments.fold(root) { f, p -> f / p!! }
        } else {
            null
        }

    suspend fun read(file: Path?): String? = withContext(Dispatchers.IO) {
        file?.takeIf { FileSystem.SYSTEM.exists(it) }?.let { path ->
            FileSystem.SYSTEM.read(path) { readUtf8() }
        }
    }

    suspend fun write(file: Path, data: String?) = withContext(Dispatchers.IO) {
        if (data.isNullOrBlank()) {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        } else {
            FileSystem.SYSTEM.write(file) { writeUtf8(data) }
        }
    }

    fun mkdirs(directory: Path): Path = directory.also { FileSystem.SYSTEM.createDirectories(it) }

    fun delete(path: Path): Boolean =
        FileSystem.SYSTEM.exists(path).also { FileSystem.SYSTEM.delete(path, mustExist = false) }

    fun deleteRecursively(path: Path): Boolean =
        FileSystem.SYSTEM.exists(path).also { FileSystem.SYSTEM.deleteRecursively(path, mustExist = false) }
}
