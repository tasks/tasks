package org.tasks.caldav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileStorage(
    rootPath: String
) {
    val root = File(rootPath, "vtodo")

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getFile(vararg segments: String?): File? =
        if (segments.none { it.isNullOrBlank() }) {
            segments.fold(root) { f, p -> File(f, p) }
        } else {
            null
        }

    suspend fun read(file: File?): String? = withContext(Dispatchers.IO) {
        file?.takeIf { it.exists() }?.readText()
    }

    suspend fun write(file: File, data: String?) = withContext(Dispatchers.IO) {
        if (data.isNullOrBlank()) {
            file.delete()
        } else {
            file.writeText(data)
        }
    }
}
