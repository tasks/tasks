package org.tasks.backup

import android.content.ContentResolver
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.todoroo.astrid.backup.BackupConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.preferences.Preferences
import java.io.File
import javax.inject.Inject

class BackupHelper @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences
) {
    suspend fun getLastBackup(): Long? {
        val uri = preferences.backupDirectory
        val timestamps: List<Long>? = withContext(Dispatchers.IO) {
            when (uri?.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    DocumentFile.fromTreeUri(context, uri)
                            ?.listFiles()
                            ?.filter { BackupConstants.isBackupFile(it.name!!) }
                            ?.map { BackupConstants.getTimestamp(it) }
                }
                ContentResolver.SCHEME_FILE -> {
                    File(uri.path!!)
                            .listFiles()
                            ?.filter { BackupConstants.isBackupFile(it.name) }
                            ?.map { BackupConstants.getTimestamp(it) }
                }
                else -> emptyList()
            }
        }
        return timestamps?.maxOrNull()
    }
}