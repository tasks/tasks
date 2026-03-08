package org.tasks.backup

import androidx.documentfile.provider.DocumentFile
import com.google.api.services.drive.model.File
import org.tasks.time.DateTime
import java.util.regex.Pattern

object BackupConstants {
    const val ENCRYPTED_FILE_EXTENSION = "crypt"
    const val INTERNAL_BACKUP = "backup.json"
    const val EXPORT_FILE_NAME = "user.%s.json"
    const val EXPORT_FILE_NAME_CRYPT = "user.%s.$ENCRYPTED_FILE_EXTENSION"
    const val BACKUP_FILE_NAME = "auto.%s.json"
    const val BACKUP_FILE_NAME_CRYPT = "auto.%s.$ENCRYPTED_FILE_EXTENSION"

    private val MATCHER = Pattern.compile(
        """(auto|user)\.(\d{2,4})(\d{2})(\d{2})[T-](\d{2})(\d{2})\.(json|crypt)"""
    )
    val BACKUP_CLEANUP_MATCHER = Regex("""auto\.\d{6,8}[T-]\d{4}\.(json|crypt)""")

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
                            it.group(2)!!.toInt().let { y -> if (y > 2000) y else y + 2000 },
                            it.group(3)!!.toInt(),
                            it.group(4)!!.toInt(),
                            it.group(5)!!.toInt(),
                            it.group(6)!!.toInt())
                            .millis
                }
    }
}