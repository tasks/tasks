package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.api.services.drive.DriveScopes
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import org.tasks.PermissionUtil
import org.tasks.R
import org.tasks.dialogs.ExportTasksDialog
import org.tasks.dialogs.ImportTasksDialog
import org.tasks.drive.DriveLoginActivity
import org.tasks.files.FileHelper
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import org.tasks.ui.Toaster
import javax.inject.Inject

private const val REQUEST_CODE_BACKUP_DIR = 10001
private const val REQUEST_DRIVE_BACKUP = 10002
private const val REQUEST_PICKER = 10003
private const val FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks"
private const val FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks"

class Backups : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var googleAccountManager: GoogleAccountManager

    override fun getPreferenceXml() = R.xml.preferences_backups

    override fun setupPreferences(savedInstanceState: Bundle?) {
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
    }

    override fun onResume() {
        super.onResume()

        updateGoogleDriveCheckbox()
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
        } else if (requestCode == REQUEST_DRIVE_BACKUP) {
            val success = resultCode == RESULT_OK
            preferences.setBoolean(R.string.p_google_drive_backup, success)
            updateGoogleDriveCheckbox()
            if (!success && data != null) {
                toaster.longToast(data.getStringExtra(GtasksLoginActivity.EXTRA_ERROR))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateGoogleDriveCheckbox() {
        val account = preferences.getStringValue(R.string.p_google_drive_backup_account)
        val pref = findPreference(R.string.google_drive_backup) as SwitchPreferenceCompat
        pref.isChecked = preferences.getBoolean(R.string.p_google_drive_backup, false)
                && googleAccountManager.canAccessAccount(account)
                && !preferences.alreadyNotified(account, DriveScopes.DRIVE_FILE)
        pref.summary = if (pref.isChecked) account else null
        findPreference(R.string.google_drive_backup)
                .setOnPreferenceChangeListener(this@Backups::onGoogleDriveCheckChanged)
    }

    private fun onGoogleDriveCheckChanged(preference: Preference, newValue: Any?) = when {
        newValue as Boolean -> {
            if (permissionRequestor.requestAccountPermissions()) {
                requestGoogleDriveLogin()
            }
            false
        }
        else -> {
            preference.summary = null
            true
        }
    }

    private fun requestGoogleDriveLogin() {
        startActivityForResult(
            Intent(context, DriveLoginActivity::class.java),
            REQUEST_DRIVE_BACKUP
        )
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

    override fun inject(component: FragmentComponent) = component.inject(this)
}