package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.AdvancedScreen
import org.tasks.extensions.Context.takePersistableUriPermission
import org.tasks.extensions.Context.toast
import org.tasks.files.FileHelper
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class Advanced : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: AdvancedViewModel by viewModels()

    private val filesDirLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                requireContext().takePersistableUriPermission(uri)
                viewModel.handleFilesDirResult(uri)
            }
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
            AdvancedScreen(
                astridSortEnabled = viewModel.astridSortEnabled,
                attachmentDirSummary = viewModel.attachmentDirSummary,
                calendarEndAtDueTime = viewModel.calendarEndAtDueTime,
                onAstridSort = { viewModel.updateAstridSort(it) },
                onAttachmentDir = {
                    filesDirLauncher.launch(
                        FileHelper.newDirectoryPickerIntent(
                            context,
                            viewModel.attachmentsDirectory,
                        )
                    )
                },
                onCalendarEndAtDueTime = { viewModel.updateCalendarEndAtDueTime(it) },
                onDeleteCompletedEvents = { viewModel.openDeleteCompletedDialog() },
                onDeleteAllEvents = { viewModel.openDeleteAllDialog() },
                onResetPreferences = { viewModel.openResetDialog() },
                onDeleteTaskData = { viewModel.openDeleteDataDialog() },
            )

            if (viewModel.showDeleteCompletedDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteCompletedDialog() },
                    text = {
                        Text(stringResource(R.string.EPr_manage_delete_completed_gcal_message))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.dismissDeleteCompletedDialog()
                            viewModel.deleteCompletedEvents { count ->
                                context?.toast(R.string.EPr_manage_delete_gcal_status, count)
                            }
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissDeleteCompletedDialog() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            if (viewModel.showDeleteAllDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteAllDialog() },
                    text = {
                        Text(stringResource(R.string.EPr_manage_delete_all_gcal_message))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.dismissDeleteAllDialog()
                            viewModel.deleteAllCalendarEvents { count ->
                                context?.toast(R.string.EPr_manage_delete_gcal_status, count)
                            }
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissDeleteAllDialog() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            if (viewModel.showResetDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissResetDialog() },
                    text = {
                        Text(stringResource(R.string.EPr_reset_preferences_warning))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.dismissResetDialog()
                            viewModel.resetPreferences()
                        }) {
                            Text(stringResource(R.string.EPr_reset_preferences))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissResetDialog() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            if (viewModel.showDeleteDataDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteDataDialog() },
                    text = {
                        Text(stringResource(R.string.EPr_delete_task_data_warning))
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.dismissDeleteDataDialog()
                            viewModel.deleteTaskData()
                        }) {
                            Text(stringResource(R.string.EPr_delete_task_data))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissDeleteDataDialog() }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
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

}
