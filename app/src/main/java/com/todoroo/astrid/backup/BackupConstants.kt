/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup

import androidx.documentfile.provider.DocumentFile
import com.google.api.services.drive.model.File
import org.tasks.time.DateTime
import java.util.regex.Pattern

/**
 * Constants for backup XML attributes and nodes.
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
object BackupConstants {
    // Do NOT edit the constants in this file! You will break compatibility with old backups
    // --- general xml
    /** Tag containing Astrid backup data  */
    const val ASTRID_TAG = "astrid"

    /** Attribute indicating backup file format  */
    const val ASTRID_ATTR_FORMAT = "format"
    // --- format 2
    /** Tag containing a task  */
    const val TASK_TAG = "task"

    /** Tag containing a comment item  */
    const val COMMENT_TAG = "comment"

    /** Tag containing a metadata item  */
    const val METADATA_TAG = "metadata"

    /** Tag containing a tagdata item  */
    const val TAGDATA_TAG = "tagdata"

    // --- general
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