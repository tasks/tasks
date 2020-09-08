package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.api.services.drive.DriveScopes
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.PermissionUtil
import org.tasks.R
import org.tasks.dialogs.ExportTasksDialog
import org.tasks.dialogs.ImportTasksDialog
import org.tasks.drive.DriveInvoker
import org.tasks.drive.DriveLoginActivity
import org.tasks.files.FileHelper
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import org.tasks.ui.Toaster
import java.util.*
import javax.inject.Inject

private const val REQUEST_CODE_BACKUP_DIR = 10001
const val REQUEST_DRIVE_BACKUP = 12002
private const val REQUEST_PICKER = 10003
private const val FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks"
private const val FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks"

@AndroidEntryPoint
class Backups : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var googleAccountManager: GoogleAccountManager
    @Inject lateinit var locale: Locale
    @Inject lateinit var driveInvoker: DriveInvoker

    override fun getPreferenceXml() = R.xml.preferences_backups

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        initializeBackupDirectory()

        findPreference(R.string.backup_BAc_import)
            .setOnPreferenceClickListener {
                startActivityForResult(
                    FileHelper.newFilePickerIntent(activity, preferences.backupDirectory),
                    REQUEST_PICKER
                )
                false
            }

        findPreference(R.string.backup_BAc_export)
            .setOnPreferenceClickListener {
                ExportTasksDialog.newExportTasksDialog()
                    .show(parentFragmentManager, FRAG_TAG_EXPORT_TASKS)
                false
            }

        findPreference(R.string.google_drive_backup)
                .setOnPreferenceChangeListener(this@Backups::onGoogleDriveCheckChanged)
        findPreference(R.string.p_google_drive_backup_account)
                .setOnPreferenceClickListener {
                    requestGoogleDriveLogin()
                    false
                }
    }

    override fun onResume() {
        super.onResume()

        updateDriveAccount()

        val driveBackup = findPreference(R.string.google_drive_backup) as SwitchPreferenceCompat
        driveBackup.isChecked = driveAccount != null
        if (driveAccount != null) {
            lifecycleScope.launch {
                val folder = preferences.getStringValue(R.string.p_google_drive_backup_folder)
                val files = driveInvoker.getFilesByPrefix(folder, "auto.", "user.")
                driveBackup.summary = getString(
                        R.string.last_backup,
                        if (files.isEmpty()) {
                            getString(R.string.last_backup_never)
                        } else {
                            DateUtilities.getLongDateStringWithTime(files[0].modifiedTime.value, locale)
                        })
            }
        }

        val lastBackup = preferences.getLong(R.string.p_backups_android_backup_last, 0L)
        findPreference(R.string.p_backups_android_backup_enabled).summary =
                getString(
                        R.string.last_backup,
                        if (lastBackup == 0L) {
                            getString(R.string.last_backup_never)
                        } else {
                            DateUtilities.getLongDateStringWithTime(lastBackup, locale)
                        }
                )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionRequestor.REQUEST_GOOGLE_ACCOUNTS) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                requestGoogleDriveLogin()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_BACKUP_DIR) {
            if (resultCode == RESULT_OK && data != null) {
                val uri = data.data!!
                context?.contentResolver
                    ?.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                preferences.setUri(R.string.p_backup_dir, uri)
                updateBackupDirectory()
            }
        } else if (requestCode == REQUEST_PICKER) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.data
                val extension = FileHelper.getExtension(activity, uri)
                if (!("json".equals(extension, ignoreCase = true) || "xml".equals(
                        extension,
                        ignoreCase = true
                    ))
                ) {
                    toaster.longToast(R.string.invalid_backup_file)
                } else {
                    ImportTasksDialog.newImportTasksDialog(uri, extension)
                        .show(parentFragmentManager, FRAG_TAG_IMPORT_TASKS)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val driveAccount: String?
        get() {
            val account = preferences.getStringValue(R.string.p_google_drive_backup_account)
            val enabled = !account.isNullOrBlank()
                    && preferences.getBoolean(R.string.p_google_drive_backup, false)
                    && googleAccountManager.canAccessAccount(account)
                    && !preferences.alreadyNotified(account, DriveScopes.DRIVE_FILE)
            return if (enabled) account else null
        }

    private fun onGoogleDriveCheckChanged(preference: Preference, newValue: Any?) = when {
        newValue as Boolean -> {
            requestGoogleDriveLogin()
            false
        }
        else -> {
            preference.summary = null
            preferences.setString(R.string.p_google_drive_backup_account, null)
            updateDriveAccount()
            true
        }
    }

    private fun updateDriveAccount() {
        val account = driveAccount
        val pref = findPreference(R.string.p_google_drive_backup_account)
        pref.isEnabled = account != null
        pref.summary =
                account
                        ?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.none)
    }

    private fun requestGoogleDriveLogin() {
        if (permissionRequestor.requestAccountPermissions()) {
            startActivityForResult(
                    Intent(context, DriveLoginActivity::class.java),
                    REQUEST_DRIVE_BACKUP
            )
        }
    }

    private fun initializeBackupDirectory() {
        findPreference(R.string.p_backup_dir)
            .setOnPreferenceClickListener {
                FileHelper.newDirectoryPicker(
                    this, REQUEST_CODE_BACKUP_DIR, preferences.backupDirectory
                )
                false
            }
        updateBackupDirectory()
    }

    private fun updateBackupDirectory() {
        findPreference(R.string.p_backup_dir).summary =
            FileHelper.uri2String(preferences.backupDirectory)
    }
}