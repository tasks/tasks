package org.tasks.preferences

import android.content.ContentResolver
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.DriveScopes
import com.todoroo.astrid.backup.BackupConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.drive.DriveInvoker
import org.tasks.gtasks.GoogleAccountManager
import timber.log.Timber
import java.io.File

class PreferencesViewModel @ViewModelInject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val driveInvoker: DriveInvoker,
        private val googleAccountManager: GoogleAccountManager,
) : ViewModel() {
    val lastBackup = MutableLiveData<Long?>()
    val lastDriveBackup = MutableLiveData<Long?>()
    val lastAndroidBackup = MutableLiveData<Long>()

    val driveAccount: String?
        get() {
            val account = preferences.getStringValue(R.string.p_google_drive_backup_account)
            val enabled = !account.isNullOrBlank()
                    && preferences.getBoolean(R.string.p_google_drive_backup, false)
                    && googleAccountManager.canAccessAccount(account)
                    && !preferences.alreadyNotified(account, DriveScopes.DRIVE_FILE)
            return if (enabled) account else null
        }


    private fun updateDriveBackup() = viewModelScope.launch {
        if (driveAccount.isNullOrBlank()) {
            lastDriveBackup.value = null
            return@launch
        }
        val files = preferences.getStringValue(R.string.p_google_drive_backup_folder)
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    try {
                        driveInvoker.getFilesByPrefix(it, "auto.", "user.")
                    } catch (e: GoogleJsonResponseException) {
                        Timber.e(e)
                        null
                    }
                }
                ?: emptyList()
        lastDriveBackup.value = files.firstOrNull()?.let { BackupConstants.getTimestamp(it) }
    }

    fun updateLocalBackup() = viewModelScope.launch {
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
        lastBackup.value = timestamps?.maxOrNull()
    }

    private fun updateAndroidBackup() {
        lastAndroidBackup.value = preferences
                .getLong(R.string.p_backups_android_backup_last, -1L)
                .takeIf { it >= 0 }
    }

    init {
        updateLocalBackup()
        updateDriveBackup()
        updateAndroidBackup()
    }
}