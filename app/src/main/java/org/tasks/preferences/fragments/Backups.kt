package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.PermissionUtil
import org.tasks.R
import org.tasks.dialogs.ExportTasksDialog
import org.tasks.dialogs.ImportTasksDialog
import org.tasks.drive.DriveLoginActivity
import org.tasks.extensions.Context.toast
import org.tasks.files.FileHelper
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.kmp.org.tasks.time.getFullDateTime
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import javax.inject.Inject

private const val REQUEST_CODE_BACKUP_DIR = 10001
const val REQUEST_DRIVE_BACKUP = 12002
private const val REQUEST_PICKER = 10003
private const val REQUEST_BACKUP_NOW = 10004
private const val FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks"
private const val FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks"

@AndroidEntryPoint
class Backups : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor

    private val viewModel: PreferencesViewModel by activityViewModels()

    override fun getPreferenceXml() = R.xml.preferences_backups

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_backup_dir)
                .setOnPreferenceClickListener {
                    FileHelper.newDirectoryPicker(
                            this, REQUEST_CODE_BACKUP_DIR, preferences.backupDirectory
                    )
                    false
                }

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
                ExportTasksDialog.newExportTasksDialog(this, REQUEST_BACKUP_NOW)
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

        findPreference(R.string.p_backups_android_backup_enabled)
                .setOnPreferenceChangeListener(this@Backups::onAndroidBackupCheckChanged)

        findPreference(R.string.p_backups_ignore_warnings).setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                preferences.setBoolean(R.string.p_backups_ignore_warnings, newValue)
                updateWarnings()
                true
            } else {
                false
            }
        }

        openUrl(R.string.documentation, R.string.url_backups)

        viewModel.lastBackup.observe(this, this::updateLastBackup)
        viewModel.lastDriveBackup.observe(this, this::updateDriveBackup)
        viewModel.lastAndroidBackup.observe(this, this::updateAndroidBackup)
    }

    private fun updateLastBackup(timestamp: Long? = viewModel.lastBackup.value) {
        findPreference(R.string.backup_BAc_export).summary =
                getString(
                        R.string.last_backup,
                        timestamp
                                ?.takeIf { it >= 0 }
                                ?.let { getFullDateTime(it) }
                                ?: getString(R.string.last_backup_never)
                )
    }

    private fun updateDriveBackup(timestamp: Long? = viewModel.lastDriveBackup.value) {
        val pref = findPreference(R.string.google_drive_backup)
        if (viewModel.staleRemoteBackup) {
            pref.setIcon(R.drawable.ic_outline_error_outline_24px)
            tintIcons(pref, requireContext().getColor(R.color.overdue))
        } else {
            pref.icon = null
        }
        pref.summary =
                getString(
                        R.string.last_backup,
                        timestamp
                                ?.takeIf { it >= 0 }
                                ?.let { getFullDateTime(it) }
                                ?: getString(R.string.last_backup_never)
                )
    }

    private fun updateAndroidBackup(timestamp: Long? = viewModel.lastAndroidBackup.value) {
        val pref = findPreference(R.string.p_backups_android_backup_enabled) as SwitchPreferenceCompat
        if (viewModel.staleRemoteBackup) {
            pref.setIcon(R.drawable.ic_outline_error_outline_24px)
            tintIcons(pref, requireContext().getColor(R.color.overdue))
        } else {
            pref.icon = null
        }
        pref.summary =
                getString(
                        R.string.last_backup,
                        timestamp
                                ?.takeIf { it >= 0 }
                                ?.let { getFullDateTime(it) }
                                ?: getString(R.string.last_backup_never)
                )
    }

    override fun onResume() {
        super.onResume()

        updateWarnings()
        updateDriveAccount()

        val driveBackup = findPreference(R.string.google_drive_backup) as SwitchPreferenceCompat
        val driveAccount = viewModel.driveAccount
        driveBackup.isChecked = driveAccount != null
    }

    private fun updateWarnings() {
        updateLastBackup()
        updateDriveBackup()
        updateAndroidBackup()
        updateBackupDirectory()
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
                viewModel.updateLocalBackup()
            }
        } else if (requestCode == REQUEST_PICKER) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.data
                val extension = FileHelper.getExtension(requireContext(), uri!!)
                if (!("json".equals(extension, ignoreCase = true) || "xml".equals(
                        extension,
                        ignoreCase = true
                    ))
                ) {
                    context?.toast(R.string.invalid_backup_file)
                } else {
                    ImportTasksDialog.newImportTasksDialog(uri, extension)
                        .show(parentFragmentManager, FRAG_TAG_IMPORT_TASKS)
                }
            }
        } else if (requestCode == REQUEST_DRIVE_BACKUP) {
            if (resultCode == RESULT_OK) {
                viewModel.updateDriveBackup()
            } else {
                data?.getStringExtra(DriveLoginActivity.EXTRA_ERROR)?.let { context?.toast(it) }
            }
        } else if (requestCode == REQUEST_BACKUP_NOW) {
            if (resultCode == RESULT_OK) {
                viewModel.updateLocalBackup()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onAndroidBackupCheckChanged(preference: Preference, newValue: Any?): Boolean {
        if (newValue is Boolean) {
            (preference as SwitchPreferenceCompat).isChecked = newValue
            updateAndroidBackup()
        }
        return true
    }

    private fun onGoogleDriveCheckChanged(preference: Preference, newValue: Any?) = when {
        newValue as Boolean -> {
            requestGoogleDriveLogin()
            false
        }
        else -> {
            preference.summary = null
            preference.icon = null
            preferences.remove(R.string.p_backups_drive_last)
            preferences.remove(R.string.p_google_drive_backup_account)
            updateDriveAccount()
            viewModel.updateDriveBackup()
            true
        }
    }

    private fun updateDriveAccount() {
        val account = viewModel.driveAccount
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

    private fun updateBackupDirectory() {
        val pref = findPreference(R.string.p_backup_dir)
        val location = FileHelper.uri2String(preferences.backupDirectory)
        pref.summary = location
        if (preferences.showBackupWarnings() && viewModel.usingPrivateStorage) {
            pref.setIcon(R.drawable.ic_outline_error_outline_24px)
            tintIcons(pref, requireContext().getColor(R.color.overdue))
            pref.summary = """
                $location

                ${requireContext().getString(R.string.backup_location_warning, FileHelper.uri2String(preferences.externalStorage))}
            """.trimIndent()
        } else {
            pref.icon = null
            pref.summary = location
        }
    }
}