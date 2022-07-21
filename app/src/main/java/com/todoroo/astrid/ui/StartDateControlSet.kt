package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.getTimeString
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.TaskEditIcon
import org.tasks.compose.TaskEditRow
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.NO_DAY
import org.tasks.dialogs.StartDatePicker.Companion.NO_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlComposeFragment
import org.tasks.ui.TaskEditViewModel
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class StartDateControlSet : TaskEditControlComposeFragment() {
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
                MdcTheme {
                    StartDateRow(
                        viewModel = viewModel,
                        vm = vm,
                        preferences = preferences,
                        locale = locale,
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
        const val TAG = R.string.TEA_ctrl_hide_until_pref
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

@Composable
fun StartDateRow(
    viewModel: TaskEditViewModel,
    vm: StartDateViewModel,
    preferences: Preferences,
    locale: Locale,
    onClick: () -> Unit,
) {
    TaskEditRow(
        icon = {
            TaskEditIcon(
                id = R.drawable.ic_pending_actions_24px,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 20.dp,
                        end = 32.dp,
                        bottom = 20.dp
                    )
            )
        },
        content = {
            StartDate(
                startDate = viewModel.startDate.collectAsStateLifecycleAware().value,
                selectedDay = vm.selectedDay.collectAsStateLifecycleAware().value,
                selectedTime = vm.selectedTime.collectAsStateLifecycleAware().value,
                displayFullDate = preferences.alwaysDisplayFullDate,
                locale = locale,
                hasDueDate = viewModel.dueDate.collectAsStateLifecycleAware().value > 0
            )
        },
        onClick = onClick
    )
}

@Composable
fun StartDate(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    displayFullDate: Boolean,
    locale: Locale = Locale.getDefault(),
    currentTime: Long = now(),
    hasDueDate: Boolean,
) {
    val context = LocalContext.current
    Text(
        text = when (selectedDay) {
            DUE_DATE -> context.getRelativeDateString(R.string.due_date, selectedTime)
            DUE_TIME -> context.getString(R.string.due_time)
            DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, selectedTime)
            WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, selectedTime)
            in 1..Long.MAX_VALUE -> DateUtilities.getRelativeDateTime(
                LocalContext.current,
                selectedDay + selectedTime,
                locale,
                FormatStyle.FULL,
                displayFullDate,
                false
            )
            else -> stringResource(id = R.string.no_start_date)
        },
        color = when {
            selectedDay < 0 && !hasDueDate -> colorResource(id = R.color.overdue)
            startDate == 0L -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
            startDate < currentTime -> colorResource(id = R.color.overdue)
            else -> MaterialTheme.colors.onSurface
        },
        modifier = Modifier
            .padding(vertical = 20.dp)
            .height(24.dp),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoStartDate() {
    MdcTheme {
        StartDate(
            startDate = 0L,
            selectedDay = NO_DAY,
            selectedTime = NO_TIME,
            displayFullDate = false,
            currentTime = 1657080392000L,
            hasDueDate = false,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FutureStartDate() {
    MdcTheme {
        StartDate(
            startDate = 1657080392000L,
            selectedDay = DUE_DATE,
            selectedTime = NO_TIME,
            displayFullDate = false,
            currentTime = 1657080392000L,
            hasDueDate = false,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PastStartDate() {
    MdcTheme {
        StartDate(
            startDate = 1657080392000L,
            selectedDay = DUE_TIME,
            selectedTime = NO_TIME,
            displayFullDate = false,
            currentTime = 1657080392001L,
            hasDueDate = false,
        )
    }
}
