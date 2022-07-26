package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.DueDateRow
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DateTimePicker
import org.tasks.preferences.Preferences
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DeadlineControlSet : TaskEditControlFragment() {
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    override fun bind(parent: ViewGroup?) =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    val dueDate = viewModel.dueDate.collectAsStateLifecycleAware().value
                    DueDateRow(
                        dueDate = if (dueDate == 0L) {
                            null
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
                        overdue = dueDate.isOverdue,
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
