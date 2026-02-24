package org.tasks.preferences.fragments

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.files.FileHelper
import org.tasks.kmp.org.tasks.time.getFullDateTime
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import javax.inject.Inject

@HiltViewModel
class BackupsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val firebase: Firebase,
) : ViewModel() {

    var backupDirSummary by mutableStateOf("")
        private set
    var showBackupDirWarning by mutableStateOf(false)
        private set
    var lastBackupSummary by mutableStateOf("")
        private set
    var showLocalBackupWarning by mutableStateOf(false)
        private set
    var backupsEnabled by mutableStateOf(preferences.getBoolean(R.string.p_backups_enabled, true))
        private set
    var driveBackupEnabled by mutableStateOf(false)
        private set
    var driveAccountSummary by mutableStateOf("")
        private set
    var driveAccountEnabled by mutableStateOf(false)
        private set
    var lastDriveBackupSummary by mutableStateOf("")
        private set
    var showDriveBackupWarning by mutableStateOf(false)
        private set
    var androidBackupEnabled by mutableStateOf(
        preferences.getBoolean(R.string.p_backups_android_backup_enabled, true)
    )
        private set
    var lastAndroidBackupSummary by mutableStateOf("")
        private set
    var showAndroidBackupWarning by mutableStateOf(false)
        private set
    var ignoreWarnings by mutableStateOf(
        preferences.getBoolean(R.string.p_backups_ignore_warnings, false)
    )
        private set

    val backupDirectory: Uri?
        get() = preferences.backupDirectory

    fun refreshState(preferencesViewModel: PreferencesViewModel) {
        backupsEnabled = preferences.getBoolean(R.string.p_backups_enabled, true)
        androidBackupEnabled = preferences.getBoolean(
            R.string.p_backups_android_backup_enabled, true,
        )
        ignoreWarnings = preferences.getBoolean(R.string.p_backups_ignore_warnings, false)
        refreshDriveState(preferencesViewModel)
        refreshWarnings(preferencesViewModel)
        preferencesViewModel.updateBackups()
    }

    fun updateBackupsEnabled(enabled: Boolean) {
        preferences.setBoolean(R.string.p_backups_enabled, enabled)
        backupsEnabled = enabled
    }

    fun disableDriveBackup(preferencesViewModel: PreferencesViewModel) {
        preferences.remove(R.string.p_backups_drive_last)
        preferences.remove(R.string.p_google_drive_backup_account)
        preferencesViewModel.updateDriveBackup()
        refreshDriveState(preferencesViewModel)
    }

    fun updateAndroidBackup(enabled: Boolean, preferencesViewModel: PreferencesViewModel) {
        preferences.setBoolean(R.string.p_backups_android_backup_enabled, enabled)
        androidBackupEnabled = enabled
        refreshAndroidBackupSummary(preferencesViewModel.lastAndroidBackup.value, preferencesViewModel)
    }

    fun updateIgnoreWarnings(enabled: Boolean, preferencesViewModel: PreferencesViewModel) {
        preferences.setBoolean(R.string.p_backups_ignore_warnings, enabled)
        ignoreWarnings = enabled
        refreshWarnings(preferencesViewModel)
    }

    fun handleBackupDirResult(uri: Uri, preferencesViewModel: PreferencesViewModel) {
        preferences.setUri(R.string.p_backup_dir, uri)
        refreshBackupDirectory(preferencesViewModel)
        preferencesViewModel.updateLocalBackup()
    }

    fun logEvent(type: String) {
        firebase.logEvent(
            R.string.event_settings_click,
            R.string.param_type to type,
        )
    }

    fun refreshDriveState(preferencesViewModel: PreferencesViewModel) {
        val account = preferencesViewModel.driveAccount
        driveBackupEnabled = account != null
        driveAccountEnabled = account != null
        driveAccountSummary = account?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.none)
    }

    fun refreshWarnings(preferencesViewModel: PreferencesViewModel) {
        refreshBackupDirectory(preferencesViewModel)
        refreshLocalBackupSummary(preferencesViewModel.lastBackup.value, preferencesViewModel)
        refreshDriveBackupSummary(preferencesViewModel.lastDriveBackup.value, preferencesViewModel)
        refreshAndroidBackupSummary(preferencesViewModel.lastAndroidBackup.value, preferencesViewModel)
    }

    fun refreshLocalBackupSummary(timestamp: Long?, preferencesViewModel: PreferencesViewModel) {
        lastBackupSummary = formatLastBackup(timestamp)
        showLocalBackupWarning = preferences.showBackupWarnings() && preferencesViewModel.staleLocalBackup
    }

    fun refreshDriveBackupSummary(timestamp: Long?, preferencesViewModel: PreferencesViewModel) {
        lastDriveBackupSummary = formatLastBackup(timestamp)
        showDriveBackupWarning = preferences.showBackupWarnings() && preferencesViewModel.staleRemoteBackup
    }

    fun refreshAndroidBackupSummary(timestamp: Long? = null, preferencesViewModel: PreferencesViewModel) {
        lastAndroidBackupSummary = formatLastBackup(timestamp)
        showAndroidBackupWarning = preferences.showBackupWarnings() && preferencesViewModel.staleRemoteBackup
    }

    private fun refreshBackupDirectory(preferencesViewModel: PreferencesViewModel) {
        val location = FileHelper.uri2String(preferences.backupDirectory) ?: ""
        if (preferences.showBackupWarnings() && preferencesViewModel.usingPrivateStorage) {
            showBackupDirWarning = true
            val warning = context.getString(
                R.string.backup_location_warning,
                FileHelper.uri2String(preferences.appPrivateStorage),
            )
            backupDirSummary = "$location\n\n$warning"
        } else {
            showBackupDirWarning = false
            backupDirSummary = location
        }
    }

    private fun formatLastBackup(timestamp: Long?): String {
        val time = timestamp
            ?.takeIf { it >= 0 }
            ?.let { getFullDateTime(it) }
            ?: context.getString(R.string.last_backup_never)
        return context.getString(R.string.last_backup, time)
    }
}
