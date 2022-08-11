package org.tasks.calendars

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.CalendarPickerList
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.dialogs.DialogBuilder
import javax.inject.Inject

@AndroidEntryPoint
class CalendarPicker : DialogFragment() {
    private val viewModel: CalendarPickerViewModel by viewModels()

    @Inject lateinit var dialogBuilder: DialogBuilder

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setContent {
                val hasPermissions = rememberMultiplePermissionsState(
                    permissions = listOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                    onPermissionsResult = { result ->
                        if (result.values.all { it }) {
                            viewModel.loadCalendars()
                        }
                    }
                )
                if (hasPermissions.allPermissionsGranted) {
                    CalendarPickerList(
                        calendars = viewModel.viewState.collectAsStateLifecycleAware().value.calendars,
                        selected = arguments?.getString(EXTRA_SELECTED),
                        onClick = {
                            targetFragment!!.onActivityResult(
                                targetRequestCode,
                                Activity.RESULT_OK,
                                Intent()
                                    .putExtra(EXTRA_CALENDAR_ID, it?.id)
                                    .putExtra(EXTRA_CALENDAR_NAME, it?.name)
                            )
                            dismiss()
                        },
                    )
                }
                LaunchedEffect(hasPermissions) {
                    if (!hasPermissions.allPermissionsGranted) {
                        hasPermissions.launchMultiplePermissionRequest()
                    }
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val EXTRA_CALENDAR_NAME = "extra_calendar_name"
        private const val EXTRA_SELECTED = "extra_selected"

        fun newCalendarPicker(target: Fragment?, rc: Int, selected: String?): CalendarPicker {
            val arguments = Bundle()
            arguments.putString(EXTRA_SELECTED, selected)
            val fragment = CalendarPicker()
            fragment.arguments = arguments
            fragment.setTargetFragment(target, rc)
            return fragment
        }
    }
}