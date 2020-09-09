package org.tasks.backup

import android.app.backup.BackupAgentHelper
import timber.log.Timber

class TasksBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        addHelper(BACKUP_KEY, TasksFileBackupHelper(this))
    }

    override fun onQuotaExceeded(backupDataBytes: Long, quotaBytes: Long) {
        Timber.e("onQuotaExceeded(backupDataBytes = $backupDataBytes, quotaBytes = $quotaBytes)")
    }

    companion object {
        private const val BACKUP_KEY = "backup"
    }
}