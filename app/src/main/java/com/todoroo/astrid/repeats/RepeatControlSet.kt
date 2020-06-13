/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import butterknife.BindView
import butterknife.OnItemSelected
import com.google.ical.values.Frequency
import com.google.ical.values.RRule
import com.google.ical.values.WeekdayNum
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.dialogs.DialogBuilder
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RepeatRuleToString
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils
import org.tasks.ui.HiddenTopArrayAdapter
import org.tasks.ui.TaskEditControlFragment
import java.text.ParseException
import java.util.*
import javax.inject.Inject

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class RepeatControlSet : TaskEditControlFragment() {
    private val repeatTypes: MutableList<String> = ArrayList()

    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var theme: Theme
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var repeatRuleToString: RepeatRuleToString

    @BindView(R.id.display_row_edit)
    lateinit var displayView: TextView

    @BindView(R.id.repeatType)
    lateinit var typeSpinner: Spinner

    @BindView(R.id.repeatTypeContainer)
    lateinit var repeatTypeContainer: LinearLayout
    
    private var rrule: RRule? = null
    private lateinit var typeAdapter: HiddenTopArrayAdapter<String>
    private var dueDate: Long = 0
    private var repeatAfterCompletion = false
    
    fun onSelected(rrule: RRule?) {
        this.rrule = rrule
        refreshDisplayView()
    }

    fun onDueDateChanged(dueDate: Long) {
        this.dueDate = if (dueDate > 0) dueDate else DateTimeUtils.currentTimeMillis()
        if (rrule != null && rrule!!.freq == Frequency.MONTHLY && rrule!!.byDay.isNotEmpty()) {
            val weekdayNum = rrule!!.byDay[0]
            val dateTime = DateTime(this.dueDate)
            val num: Int
            val dayOfWeekInMonth = dateTime.dayOfWeekInMonth
            num = if (weekdayNum.num == -1 || dayOfWeekInMonth == 5) {
                if (dayOfWeekInMonth == dateTime.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
            } else {
                dayOfWeekInMonth
            }
            rrule!!.byDay = listOf((WeekdayNum(num, dateTime.weekday)))
            refreshDisplayView()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            repeatAfterCompletion = task.repeatAfterCompletion()
            dueDate = task.dueDate
            if (dueDate <= 0) {
                dueDate = DateTimeUtils.currentTimeMillis()
            }
            val recurrenceWithoutFrom = task.getRecurrenceWithoutFrom()
            if (isNullOrEmpty(recurrenceWithoutFrom)) {
                rrule = null
            } else {
                try {
                    rrule = RRule(recurrenceWithoutFrom)
                    rrule!!.until = DateTime(task.repeatUntil).toDateValue()
                } catch (e: ParseException) {
                    rrule = null
                }
            }
        } else {
            val recurrence = savedInstanceState.getString(EXTRA_RECURRENCE)
            dueDate = savedInstanceState.getLong(EXTRA_DUE_DATE)
            rrule = if (isNullOrEmpty(recurrence)) {
                null
            } else {
                try {
                    RRule(recurrence)
                } catch (e: ParseException) {
                    null
                }
            }
            repeatAfterCompletion = savedInstanceState.getBoolean(EXTRA_REPEAT_AFTER_COMPLETION)
        }
        repeatTypes.add("")
        repeatTypes.addAll(listOf(*resources.getStringArray(R.array.repeat_type)))
        typeAdapter = object : HiddenTopArrayAdapter<String>(activity, 0, repeatTypes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var selectedItemPosition = position
                if (parent is AdapterView<*>) {
                    selectedItemPosition = parent.selectedItemPosition
                }
                val tv = inflater.inflate(android.R.layout.simple_spinner_item, parent, false) as TextView
                tv.setPadding(0, 0, 0, 0)
                tv.text = repeatTypes[selectedItemPosition]
                return tv
            }
        }
        val drawable = activity.getDrawable(R.drawable.textfield_underline_black)!!.mutate()
        drawable.setTint(activity.getColor(R.color.text_primary))
        typeSpinner.setBackgroundDrawable(drawable)
        typeSpinner.adapter = typeAdapter
        typeSpinner.setSelection(if (repeatAfterCompletion) TYPE_COMPLETION_DATE else TYPE_DUE_DATE)
        refreshDisplayView()
        return view
    }

    @OnItemSelected(R.id.repeatType)
    fun onRepeatTypeChanged(position: Int) {
        repeatAfterCompletion = position == TYPE_COMPLETION_DATE
        repeatTypes[0] = if (repeatAfterCompletion) repeatTypes[2] else repeatTypes[1]
        typeAdapter.notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_RECURRENCE, if (rrule == null) "" else rrule!!.toIcal())
        outState.putBoolean(EXTRA_REPEAT_AFTER_COMPLETION, repeatAfterCompletion)
        outState.putLong(EXTRA_DUE_DATE, dueDate)
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onRowClick() {
        BasicRecurrenceDialog.newBasicRecurrenceDialog(this, rrule, dueDate)
                .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
    }

    override val isClickable: Boolean
        get() = true

    override val layout: Int
        get() = R.layout.control_set_repeat_display

    override val icon: Int
        get() = R.drawable.ic_outline_repeat_24px

    override fun controlId() = TAG

    override fun hasChanges(original: Task): Boolean {
        val repeatUntil = rrule?.let { DateTime.from(it.until).millis } ?: 0
        return recurrenceValue != original.recurrence.orEmpty()
                || original.repeatUntil != repeatUntil
    }

    override fun apply(task: Task) {
        task.repeatUntil = if (rrule == null) 0 else DateTime.from(rrule!!.until).millis
        task.recurrence = recurrenceValue
    }

    private val recurrenceValue: String
        get() {
            if (rrule == null) {
                return ""
            }
            val copy: RRule = try {
                RRule(rrule!!.toIcal())
            } catch (e: ParseException) {
                return ""
            }
            copy.until = null
            var result = copy.toIcal()
            if (repeatAfterCompletion && !isNullOrEmpty(result)) {
                result += ";FROM=COMPLETION" // $NON-NLS-1$
            }
            return result
        }

    private fun refreshDisplayView() {
        if (rrule == null) {
            displayView.text = null
            repeatTypeContainer.visibility = View.GONE
        } else {
            displayView.text = repeatRuleToString.toString(rrule)
            repeatTypeContainer.visibility = View.VISIBLE
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_repeat_pref
        private const val TYPE_DUE_DATE = 1
        private const val TYPE_COMPLETION_DATE = 2
        private const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
        private const val EXTRA_RECURRENCE = "extra_recurrence"
        private const val EXTRA_DUE_DATE = "extra_due_date"
        private const val EXTRA_REPEAT_AFTER_COMPLETION = "extra_repeat_after_completion"
    }
}