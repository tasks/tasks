package org.tasks.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.preferences.Preferences
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DeadlineControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    override fun onRowClick() {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            newDateTimePicker(
                    this,
                    REQUEST_DATE,
                    viewModel.dueDate.value,
                    preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
                    .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    override val isClickable = true

    @Composable
    override fun Body() {
        val dueDate = viewModel.dueDate.collectAsStateLifecycleAware().value
        Text(
            text = if (dueDate == 0L) {
                stringResource(id = R.string.no_due_date)
            } else {
                DateUtilities.getRelativeDateTime(
                    LocalContext.current,
                    dueDate,
                    locale,
                    FormatStyle.FULL,
                    preferences.alwaysDisplayFullDate,
                    false
                )
            },
            color = when {
                dueDate == 0L -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                dueDate.isOverdue -> colorResource(id = R.color.overdue)
                else -> MaterialTheme.colors.onSurface
            },
            modifier = Modifier.padding(vertical = 20.dp)
        )
    }

    override val icon = R.drawable.ic_outline_schedule_24px

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

        private val Long.isOverdue: Boolean
            get() = if (hasDueTime(this)) {
                DateTimeUtils.newDateTime(this).isBeforeNow
            } else {
                DateTimeUtils.newDateTime(this).endOfDay().isBeforeNow
            }
    }
}