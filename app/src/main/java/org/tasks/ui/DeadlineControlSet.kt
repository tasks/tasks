package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditIcon
import org.tasks.compose.TaskEditRow
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DateTimePicker
import org.tasks.preferences.Preferences
import org.tasks.ui.DeadlineControlSet.Companion.isOverdue
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DeadlineControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    override fun bind(parent: ViewGroup?) =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    DueDateRow(
                        viewModel = viewModel,
                        locale = locale,
                        displayFullDate = preferences.alwaysDisplayFullDate,
                        onClick = {
                            val fragmentManager = parentFragmentManager
                            if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
                                DateTimePicker.newDateTimePicker(
                                    this@DeadlineControlSet,
                                    REQUEST_DATE,
                                    viewModel.dueDate.value,
                                    preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
                                    .show(fragmentManager, FRAG_TAG_DATE_PICKER)
                            }
                        }
                    )
                }
            }
        }

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.setDueDate(data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_when_pref
        private const val REQUEST_DATE = 504
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"

        val Long.isOverdue: Boolean
            get() = if (hasDueTime(this)) {
                DateTimeUtils.newDateTime(this).isBeforeNow
            } else {
                DateTimeUtils.newDateTime(this).endOfDay().isBeforeNow
            }
    }
}

@Composable
fun DueDateRow(
    viewModel: TaskEditViewModel,
    locale: Locale,
    displayFullDate: Boolean,
    onClick: () -> Unit,
) {
    TaskEditRow(
        icon = {
            TaskEditIcon(
                id = R.drawable.ic_outline_schedule_24px,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 20.dp,
                    end = 32.dp,
                    bottom = 20.dp
                )
            )
        },
        content = {
            DueDate(
                dueDate = viewModel.dueDate.collectAsStateLifecycleAware().value,
                locale = locale,
                displayFullDate = displayFullDate,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun DueDate(dueDate: Long, locale: Locale, displayFullDate: Boolean) {
    if (dueDate == 0L) {
        DisabledText(
            text = stringResource(id = R.string.no_due_date),
            modifier = Modifier.padding(vertical = 20.dp)
        )
    } else {
        Text(
            text = DateUtilities.getRelativeDateTime(
                LocalContext.current,
                dueDate,
                locale,
                FormatStyle.FULL,
                displayFullDate,
                false
            ),
            color = if (dueDate.isOverdue) {
                colorResource(id = R.color.overdue)
            } else {
                MaterialTheme.colors.onSurface
            },
            modifier = Modifier.padding(vertical = 20.dp)
        )
    }
}