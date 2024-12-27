package org.tasks.caldav

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FileStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    val root = File(context.filesDir, "vtodo")

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
