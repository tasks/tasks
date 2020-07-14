package org.tasks.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.FileBackupHelper
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.todoroo.astrid.backup.BackupConstants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.io.IOException

class TasksBackupAgent : BackupAgentHelper() {
    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    internal interface TasksBackupAgentEntryPoint {
        val tasksJsonImporter: TasksJsonImporter
    }

    private lateinit var importer: TasksJsonImporter

    override fun onCreate() {
        val hilt = EntryPointAccessors.fromApplication(applicationContext, TasksBackupAgentEntryPoint::class.java)
        importer = hilt.tasksJsonImporter
        addHelper(BACKUP_KEY, FileBackupHelper(this, BackupConstants.INTERNAL_BACKUP))
    }

    @Throws(IOException::class)
    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) {
        super.onRestore(data, appVersionCode, newState)
        val backup = File(String.format(
                "%s/%s", filesDir.absolutePath, BackupConstants.INTERNAL_BACKUP))
        if (backup.exists()) {
            runBlocking {
                importer.importTasks(this@TasksBackupAgent, Uri.fromFile(backup), null)
            }
        } else {
            Timber.w("%s not found", backup.absolutePath)
        }
    }

    override fun onQuotaExceeded(backupDataBytes: Long, quotaBytes: Long) {
        Timber.e("onQuotaExceeded(%s, %s)", backupDataBytes, quotaBytes)
    }

    companion object {
        private const val BACKUP_KEY = "backup"
    }
}