package org.tasks.preferences

import android.content.ContentResolver
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.backup.BackupConstants
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.googleapis.InvokerFactory
import org.tasks.gtasks.GoogleAccountManager
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    invokers: InvokerFactory,
    private val googleAccountManager: GoogleAccountManager,
    private val caldavDao: CaldavDao,
) : ViewModel() {
    private val driveInvoker = invokers.getDriveInvoker()
    val lastBackup = MutableLiveData<Long?>()
    val lastDriveBackup = MutableLiveData<Long?>()
    val lastAndroidBackup = MutableLiveData<Long>()
    val caldavAccounts: Flow<List<CaldavAccount>>
        get() = caldavDao.watchAccounts()

    private fun isStale(timestamp: Long?) =
            timestamp != null
                    && preferences.showBackupWarnings()
                    && timestamp < newDateTime().startOfDay().minusDays(2).millis

    val staleLocalBackup: Boolean
        get() = isStale(lastBackup.value)

    val staleRemoteBackup: Boolean
        get() = isStale(lastDriveBackup.value) && isStale(lastAndroidBackup.value)

    val usingPrivateStorage: Boolean
        get() = preferences.backupDirectory.let {
            it == null || it.toString().startsWith(preferences.externalStorage.toString())
        }

    val driveAccount: String?
        get() {
            val account = preferences.getStringValue(R.string.p_google_drive_backup_account)
            val enabled = !account.isNullOrBlank()
                    && preferences.getBoolean(R.string.p_google_drive_backup, false)
                    && googleAccountManager.canAccessAccount(account)
                    && !preferences.alreadyNotified(account, DriveScopes.DRIVE_FILE)
            return if (enabled) account else null
        }

    suspend fun tasksAccount(): CaldavAccount? =
        caldavDao.getAccounts(CaldavAccount.TYPE_TASKS).firstOrNull()

    fun updateDriveBackup() = viewModelScope.launch {
        if (driveAccount.isNullOrBlank()) {
            lastDriveBackup.value = -1L
            return@launch
        }
        lastDriveBackup.value = preferences
                .getStringValue(R.string.p_google_drive_backup_folder)
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    try {
                        driveInvoker.getFilesByPrefix(it, "auto.", "user.")
                                .firstOrNull()
                                ?.let(BackupConstants::getTimestamp)
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                }
                ?: preferences.getLong(R.string.p_backups_drive_last, -1L)
    }

    fun updateLocalBackup() = viewModelScope.launch {
        val uri = preferences.backupDirectory
        val timestamps: List<Long>? = withContext(Dispatchers.IO) {
            when (uri?.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    DocumentFile.fromTreeUri(context, uri)
                            ?.listFiles()
                            ?.filter { BackupConstants.isBackupFile(it.name) }
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
        lastBackup.value = timestamps?.maxOrNull() ?: -1L
    }

    private fun updateAndroidBackup() {
        lastAndroidBackup.value = preferences
                .getLong(R.string.p_backups_android_backup_last, -1L)
    }

    fun updateBackups() {
        updateLocalBackup()
        updateDriveBackup()
        updateAndroidBackup()
    }
}