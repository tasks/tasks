package org.tasks.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import org.tasks.R
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DateTimePicker
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import java.time.format.FormatStyle
import javax.inject.Inject

class DeadlineControlSet : TaskEditControlFragment() {
    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    @BindView(R.id.due_date)
    lateinit var dueDate: TextView

    private lateinit var callback: DueDateChangeListener
    private var date: Long = 0

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as DueDateChangeListener
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        date = savedInstanceState?.getLong(EXTRA_DATE) ?: task.dueDate
        refreshDisplayView()
        return view
    }

    override fun onRowClick() {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            newDateTimePicker(
                    this,
                    REQUEST_DATE,
                    dueDateTime,
                    preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
                    .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    override val isClickable: Boolean
        get() = true

    override val layout: Int
        get() = R.layout.control_set_deadline

    override val icon: Int
        get() = R.drawable.ic_outline_schedule_24px

    override fun controlId() = TAG

    override fun hasChanges(original: Task): Boolean {
        return original.dueDate != dueDateTime
    }

    override fun apply(task: Task) {
        val dueDate = dueDateTime
        if (dueDate != task.dueDate) {
            task.reminderSnooze = 0L
        }
        task.dueDate = dueDate
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                val timestamp = data!!.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L)
                val dateTime = DateTime(timestamp)
                date = dateTime.millis
                callback.dueDateChanged(dueDateTime)
            }
            refreshDisplayView()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val dueDateTime: Long
        get() = if (date == 0L) 0 else createDueDate(
                if (hasDueTime(date)) Task.URGENCY_SPECIFIC_DAY_TIME else Task.URGENCY_SPECIFIC_DAY,
                date)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_DATE, date)
    }

    private fun refreshDisplayView() {
        if (date == 0L) {
            dueDate.text = ""
            setTextColor(false)
        } else {
            dueDate.text = DateUtilities.getRelativeDateTime(activity, date, locale.locale, FormatStyle.FULL)
            setTextColor(
                    if (hasDueTime(date)) DateTimeUtils.newDateTime(date).isBeforeNow else DateTimeUtils.newDateTime(date).endOfDay().isBeforeNow)
        }
    }

    private fun setTextColor(overdue: Boolean) {
        dueDate.setTextColor(
                activity.getColor(if (overdue) R.color.overdue else R.color.text_primary))
    }

    interface DueDateChangeListener {
        fun dueDateChanged(dateTime: Long)
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_when_pref
        private const val REQUEST_DATE = 504
        private const val EXTRA_DATE = "extra_date"
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
    }
}