package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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

private const val FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks"
private const val FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks"

@AndroidEntryPoint
class Backups : Fragment() {

    @Inject lateinit var theme: Theme

    private val preferencesViewModel: PreferencesViewModel by activityViewModels()
    private val viewModel: BackupsViewModel by viewModels()

    private val backupDirLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                requireContext().takePersistableUriPermission(uri)
                viewModel.handleBackupDirResult(uri, preferencesViewModel)
            }
        }
    }

    private val importPickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val extension = FileHelper.getExtension(requireContext(), uri)
                if (!"json".equals(extension, ignoreCase = true)) {
                    context?.toast(R.string.invalid_backup_file)
                } else {
                    ImportTasksDialog.newImportTasksDialog(uri)
                        .show(parentFragmentManager, FRAG_TAG_IMPORT_TASKS)
                }
            }
        }
    }

    private val driveLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            preferencesViewModel.updateDriveBackup()
        } else {
            result.data?.getStringExtra(DriveLoginActivity.EXTRA_ERROR)
                ?.let { context?.toast(it) }
        }
        viewModel.refreshDriveState(preferencesViewModel)
    }

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
                    backupDirLauncher.launch(
                        FileHelper.newDirectoryPickerIntent(
                            context,
                            viewModel.backupDirectory,
                        )
                    )
                },
                onBackupNow = {
                    viewModel.logEvent("backup_now")
                    ExportTasksDialog.newExportTasksDialog()
                        .show(parentFragmentManager, FRAG_TAG_EXPORT_TASKS)
                },
                onImportBackup = {
                    viewModel.logEvent("import_backup")
                    importPickerLauncher.launch(
                        FileHelper.newFilePickerIntent(activity, viewModel.backupDirectory),
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

    private fun requestGoogleDriveLogin() {
        driveLauncher.launch(
            Intent(context, DriveLoginActivity::class.java),
        )
    }
}
