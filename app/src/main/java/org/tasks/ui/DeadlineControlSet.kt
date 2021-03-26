package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetDeadlineBinding
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import java.time.format.FormatStyle
import javax.inject.Inject

@AndroidEntryPoint
class DeadlineControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    private lateinit var dueDate: TextView
    private lateinit var callback: DueDateChangeListener

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as DueDateChangeListener
    }

    override fun createView(savedInstanceState: Bundle?) {
        refreshDisplayView()
    }

    override fun onRowClick() {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            newDateTimePicker(
                    this,
                    REQUEST_DATE,
                    viewModel.dueDate!!,
                    preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
                    .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    override val isClickable = true

    override fun bind(parent: ViewGroup?) =
        ControlSetDeadlineBinding.inflate(layoutInflater, parent, true).let {
            dueDate = it.dueDate
            it.root
        }

    override val icon = R.drawable.ic_outline_schedule_24px

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.dueDate = data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L)
                callback.dueDateChanged()
            }
            refreshDisplayView()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun refreshDisplayView() {
        val date = viewModel.dueDate!!
        if (date == 0L) {
            dueDate.text = ""
            setTextColor(false)
        } else {
            dueDate.text = DateUtilities.getRelativeDateTime(
                    activity,
                    date,
                    locale.locale,
                    FormatStyle.FULL,
                    preferences.alwaysDisplayFullDate,
                    false
            )
            setTextColor(if (hasDueTime(date)) {
                DateTimeUtils.newDateTime(date).isBeforeNow
            } else {
                DateTimeUtils.newDateTime(date).endOfDay().isBeforeNow
            })
        }
    }

    private fun setTextColor(overdue: Boolean) {
        dueDate.setTextColor(
                activity.getColor(if (overdue) R.color.overdue else R.color.text_primary))
    }

    interface DueDateChangeListener {
        fun dueDateChanged()
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_when_pref
        private const val REQUEST_DATE = 504
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
    }
}