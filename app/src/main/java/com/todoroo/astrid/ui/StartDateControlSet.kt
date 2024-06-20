package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.getTimeString
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.edit.StartDateRow
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.NO_DAY
import org.tasks.dialogs.StartDatePicker.Companion.NO_TIME
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.ui.TaskEditControlFragment
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StartDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale

    private val vm: StartDateViewModel by viewModels()

    override fun createView(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            vm.init(viewModel.dueDate.value, viewModel.startDate.value, viewModel.isNew)
        }
        lifecycleScope.launchWhenResumed {
            viewModel.dueDate.collect {
                applySelected()
            }
        }
    }

    override fun bind(parent: ViewGroup?) =
        (parent as ComposeView).apply {
            setContent {
                TasksTheme {
                    val selectedDay = vm.selectedDay.collectAsStateWithLifecycle().value
                    val selectedTime = vm.selectedTime.collectAsStateWithLifecycle().value
                    StartDateRow(
                        startDate = viewModel.startDate.collectAsStateWithLifecycle().value,
                        selectedDay = selectedDay,
                        selectedTime = selectedTime,
                        hasDueDate = viewModel.dueDate.collectAsStateWithLifecycle().value > 0,
                        printDate = {
                            DateUtilities.getRelativeDateTime(
                                requireContext(),
                                selectedDay + selectedTime,
                                locale,
                                FormatStyle.FULL,
                                preferences.alwaysDisplayFullDate,
                                false
                            )
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
                }
            }
        }

    override fun controlId() = TAG

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
                "${getString(resId)} ${getTimeString(this, newDateTime().withMillisOfDay(time))}"
            }
    }
}
