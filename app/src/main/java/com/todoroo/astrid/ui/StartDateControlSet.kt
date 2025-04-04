package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.edit.StartDateRow
import org.tasks.data.entity.Alarm
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.NO_DAY
import org.tasks.dialogs.StartDatePicker.Companion.NO_TIME
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class StartDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences

    private val vm: StartDateViewModel by viewModels()

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            vm.init(
                dueDate = viewModel.dueDate.value,
                startDate = viewModel.startDate.value,
                isNew = viewModel.viewState.value.isNew
            )
        }
        val dueDate = viewModel.dueDate.collectAsStateWithLifecycle().value
        val selectedDay = vm.selectedDay.collectAsStateWithLifecycle().value
        val selectedTime = vm.selectedTime.collectAsStateWithLifecycle().value
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        StartDateRow(
            startDate = viewModel.startDate.collectAsStateWithLifecycle().value,
            selectedDay = selectedDay,
            selectedTime = selectedTime,
            hasStartAlarm = remember (viewState.alarms) {
                viewState.alarms.any { it.type == Alarm.TYPE_REL_START }
            },
            hasDueDate = dueDate > 0,
            printDate = {
                runBlocking {
                    getRelativeDateTime(
                        selectedDay + selectedTime,
                        requireContext().is24HourFormat,
                        DateStyle.FULL,
                        alwaysDisplayFullDate = preferences.alwaysDisplayFullDate
                    )
                }
            },
            onClick = {
                val fragmentManager = parentFragmentManager
                if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
                    StartDatePicker.newDateTimePicker(
                        this@StartDateControlSet,
                        REQUEST_START_DATE,
                        vm.selectedDay.value,
                        vm.selectedTime.value,
                        preferences.getBoolean(
                            R.string.p_auto_dismiss_datetime_edit_screen,
                            false
                        )
                    )
                        .show(fragmentManager, FRAG_TAG_DATE_PICKER)
                }
            }
        )

        LaunchedEffect(dueDate) {
            applySelected()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_START_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                vm.setSelected(
                    selectedDay = data?.getLongExtra(EXTRA_DAY, 0L) ?: NO_DAY,
                    selectedTime = data?.getIntExtra(EXTRA_TIME, 0) ?: NO_TIME
                )
                applySelected()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun applySelected() {
        viewModel.setStartDate(vm.getSelectedValue(viewModel.dueDate.value))
    }

    companion object {
        val TAG = R.string.TEA_ctrl_hide_until_pref
        private const val REQUEST_START_DATE = 11011
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"

        internal fun Context.getRelativeDateString(resId: Int, time: Int) =
            if (time == NO_TIME) {
                getString(resId)
            } else {
                "${getString(resId)} ${getTimeString(currentTimeMillis().withMillisOfDay(time), this.is24HourFormat)}"
            }
    }
}
