package org.tasks.backup

import android.app.backup.BackupDataInputStream
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.File

class TasksFileBackupHelper(
        private val context: Context
) : FileBackupHelper(context, BackupConstants.INTERNAL_BACKUP) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface TasksFileBackupHelperEntryPoint {
        val tasksJsonImporter: TasksJsonImporter
        val preferences: Preferences
        val localBroadcastManager: LocalBroadcastManager
    }

    override fun performBackup(
            oldState: ParcelFileDescriptor?,
            data: BackupDataOutput?,
            newState: ParcelFileDescriptor?
    ) {
        val preferences = hilt.preferences
        if (!preferences.androidBackupServiceEnabled()) {
            Timber.d("Android backup service disabled")
            return
        }
        file
                ?.let {
                    Timber.d("Backing up ${it.absolutePath}")
                    super.performBackup(oldState, data, newState)
                    preferences.setLong(R.string.p_backups_android_backup_last, it.lastModified())
                    hilt.localBroadcastManager.broadcastPreferenceRefresh()
                }
                ?: Timber.e("$path not found")
    }

    private val path: String
        get() = "${context.filesDir.absolutePath}/${BackupConstants.INTERNAL_BACKUP}"

    private val file: File?
        get() = File(path).takeIf { it.exists() }

    override fun restoreEntity(data: BackupDataInputStream?) {
        super.restoreEntity(data)

        file
                ?.let {
                    runBlocking {
                        Timber.d("Restoring ${it.absolutePath}")
                        hilt.tasksJsonImporter.importTasks(context, Uri.fromFile(it), null)
                    }
                }
                ?: Timber.e("$path not found")
    }

    private val hilt: TasksFileBackupHelperEntryPoint
        get() = EntryPointAccessors
                .fromApplication(context, TasksFileBackupHelperEntryPoint::class.java)
}