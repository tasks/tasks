package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.BackupsScreen
import org.tasks.dialogs.ExportTasksDialog
import org.tasks.dialogs.ImportTasksDialog
import org.tasks.drive.DriveLoginActivity
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.takePersistableUriPermission
import org.tasks.extensions.Context.toast
import org.tasks.files.FileHelper
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

private const val REQUEST_CODE_BACKUP_DIR = 10001
const val REQUEST_DRIVE_BACKUP = 12002
private const val REQUEST_PICKER = 10003
private const val FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks"
const val FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks"

@AndroidEntryPoint
class Backups : Fragment() {

    @Inject lateinit var theme: Theme

    private val preferencesViewModel: PreferencesViewModel by activityViewModels()
    private val viewModel: BackupsViewModel by viewModels()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesViewModel.lastBackup.observe(this) {
            viewModel.refreshLocalBackupSummary(it, preferencesViewModel)
        }
        preferencesViewModel.lastDriveBackup.observe(this) {
            viewModel.refreshDriveBackupSummary(it, preferencesViewModel)
        }
        preferencesViewModel.lastAndroidBackup.observe(this) {
            viewModel.refreshAndroidBackupSummary(it, preferencesViewModel)
        }
        parentFragmentManager.setFragmentResultListener(
            ExportTasksDialog.REQUEST_KEY, this
        ) { _, _ ->
            preferencesViewModel.updateLocalBackup()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            BackupsScreen(
                backupDirSummary = viewModel.backupDirSummary,
                showBackupDirWarning = viewModel.showBackupDirWarning,
                lastBackupSummary = viewModel.lastBackupSummary,
                showLocalBackupWarning = viewModel.showLocalBackupWarning,
                backupsEnabled = viewModel.backupsEnabled,
                driveBackupEnabled = viewModel.driveBackupEnabled,
                driveAccountSummary = viewModel.driveAccountSummary,
                driveAccountEnabled = viewModel.driveAccountEnabled,
                lastDriveBackupSummary = viewModel.lastDriveBackupSummary,
                showDriveBackupWarning = viewModel.showDriveBackupWarning,
                androidBackupEnabled = viewModel.androidBackupEnabled,
                lastAndroidBackupSummary = viewModel.lastAndroidBackupSummary,
                showAndroidBackupWarning = viewModel.showAndroidBackupWarning,
                ignoreWarnings = viewModel.ignoreWarnings,
                onDocumentation = {
                    requireContext().openUri(R.string.url_backups)
                },
                onBackupDir = {
                    FileHelper.newDirectoryPicker(
                        this@Backups,
                        REQUEST_CODE_BACKUP_DIR,
                        viewModel.backupDirectory,
                    )
                },
                onBackupNow = {
                    viewModel.logEvent("backup_now")
                    ExportTasksDialog.newExportTasksDialog()
                        .show(parentFragmentManager, FRAG_TAG_EXPORT_TASKS)
                },
                onImportBackup = {
                    viewModel.logEvent("import_backup")
                    startActivityForResult(
                        FileHelper.newFilePickerIntent(activity, viewModel.backupDirectory),
                        REQUEST_PICKER,
                    )
                },
                onBackupsEnabled = { viewModel.updateBackupsEnabled(it) },
                onDriveBackup = { enabled ->
                    if (enabled) {
                        requestGoogleDriveLogin()
                    } else {
                        viewModel.disableDriveBackup(preferencesViewModel)
                    }
                },
                onDriveAccount = {
                    requestGoogleDriveLogin()
                },
                onAndroidBackup = { enabled ->
                    viewModel.updateAndroidBackup(enabled, preferencesViewModel)
                },
                onDeviceSettings = {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                onIgnoreWarnings = { enabled ->
                    viewModel.updateIgnoreWarnings(enabled, preferencesViewModel)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState(preferencesViewModel)
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_BACKUP_DIR -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data!!
                    requireContext().takePersistableUriPermission(uri)
                    viewModel.handleBackupDirResult(uri, preferencesViewModel)
                }
            }
            REQUEST_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val uri = data!!.data
                    val extension = FileHelper.getExtension(requireContext(), uri!!)
                    if (!"json".equals(extension, ignoreCase = true)) {
                        context?.toast(R.string.invalid_backup_file)
                    } else {
                        ImportTasksDialog.newImportTasksDialog(uri)
                            .show(parentFragmentManager, FRAG_TAG_IMPORT_TASKS)
                    }
                }
            }
            REQUEST_DRIVE_BACKUP -> {
                if (resultCode == RESULT_OK) {
                    preferencesViewModel.updateDriveBackup()
                } else {
                    data?.getStringExtra(DriveLoginActivity.EXTRA_ERROR)
                        ?.let { context?.toast(it) }
                }
                viewModel.refreshDriveState(preferencesViewModel)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requestGoogleDriveLogin() {
        startActivityForResult(
            Intent(context, DriveLoginActivity::class.java),
            REQUEST_DRIVE_BACKUP,
        )
    }
}
