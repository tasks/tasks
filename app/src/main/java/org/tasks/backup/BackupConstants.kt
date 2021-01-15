package org.tasks.backup

import androidx.documentfile.provider.DocumentFile
import com.google.api.services.drive.model.File
import org.tasks.time.DateTime
import java.util.regex.Pattern

object BackupConstants {
    const val INTERNAL_BACKUP = "backup.json"
    const val EXPORT_FILE_NAME = "user.%s.json"
    const val BACKUP_FILE_NAME = "auto.%s.json"

    private val MATCHER = Pattern.compile("""(auto|user)\.(\d{2})(\d{2})(\d{2})-(\d{2})(\d{2})\.json""")

    fun isBackupFile(name: String?) = name?.let { MATCHER.matcher(it).matches() } ?: false

    fun getTimestamp(file: java.io.File): Long {
        return getTimestampFromFilename(file.name) ?: file.lastModified()
    }

    fun getTimestamp(file: File): Long? {
        return getTimestampFromFilename(file.name) ?: file.modifiedTime?.value
    }

    fun getTimestamp(file: DocumentFile): Long {
        return file.name?.let { getTimestampFromFilename(it) } ?: file.lastModified()
    }

    internal fun getTimestampFromFilename(name: String): Long? {
        return MATCHER
                .matcher(name)
                .takeIf { it.matches() }
                ?.let {
                    DateTime(
                            2000 + it.group(2)!!.toInt(),
                            it.group(3)!!.toInt(),
                            it.group(4)!!.toInt(),
                            it.group(5)!!.toInt(),
                            it.group(6)!!.toInt())
                            .millis
                }
    }
}