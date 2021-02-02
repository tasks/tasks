/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.dialogs.DialogBuilder
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.RepeatRuleToString
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.ui.HiddenTopArrayAdapter
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import javax.inject.Inject

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class RepeatControlSet : TaskEditControlFragment() {
    private val repeatTypes: MutableList<String> = ArrayList()

    @Inject lateinit var activity: Activity
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
    
    private lateinit var typeAdapter: HiddenTopArrayAdapter<String>

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_RECURRENCE) {
            if (resultCode == RESULT_OK) {
                viewModel.rrule = data
                        ?.getStringExtra(BasicRecurrenceDialog.EXTRA_RRULE)
                        ?.let { RRule(it) }
                refreshDisplayView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onDueDateChanged() {
        viewModel.rrule?.let {
            if (it.freq == Frequency.MONTHLY && it.byDay.isNotEmpty()) {
                val weekdayNum = it.byDay[0]
                val dateTime = DateTime(this.dueDate)
                val num: Int
                val dayOfWeekInMonth = dateTime.dayOfWeekInMonth
                num = if (weekdayNum.num == -1 || dayOfWeekInMonth == 5) {
                    if (dayOfWeekInMonth == dateTime.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
                } else {
                    dayOfWeekInMonth
                }
                it.byDay = listOf((WeekdayNum(num, dateTime.weekday)))
                viewModel.rrule = it
                refreshDisplayView()
            }
        }
    }

    override fun createView(savedInstanceState: Bundle?) {
        repeatTypes.add("")
        repeatTypes.addAll(listOf(*resources.getStringArray(R.array.repeat_type)))
        typeAdapter = object : HiddenTopArrayAdapter<String>(activity, 0, repeatTypes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var selectedItemPosition = position
                if (parent is AdapterView<*>) {
                    selectedItemPosition = parent.selectedItemPosition
                }
                val tv = activity.layoutInflater.inflate(android.R.layout.simple_spinner_item, parent, false) as TextView
                tv.setPadding(0, 0, 0, 0)
                tv.text = repeatTypes[selectedItemPosition]
                return tv
            }
        }
        val drawable = activity.getDrawable(R.drawable.textfield_underline_black)!!.mutate()
        drawable.setTint(activity.getColor(R.color.text_primary))
        typeSpinner.setBackgroundDrawable(drawable)
        typeSpinner.adapter = typeAdapter
        typeSpinner.setSelection(if (viewModel.repeatAfterCompletion!!) TYPE_COMPLETION_DATE else TYPE_DUE_DATE)
        refreshDisplayView()
    }

    @OnItemSelected(R.id.repeatType)
    fun onRepeatTypeChanged(position: Int) {
        viewModel.repeatAfterCompletion = position == TYPE_COMPLETION_DATE
        repeatTypes[0] = if (viewModel.repeatAfterCompletion!!) repeatTypes[2] else repeatTypes[1]
        typeAdapter.notifyDataSetChanged()
    }

    private val dueDate: Long
        get() = viewModel.dueDate!!.let { if (it > 0) it else currentTimeMillis() }

    override fun onRowClick() {
        BasicRecurrenceDialog.newBasicRecurrenceDialog(
                this, REQUEST_RECURRENCE, viewModel.rrule, dueDate)
                .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
    }

    override val isClickable = true

    override val layout = R.layout.control_set_repeat_display

    override val icon = R.drawable.ic_outline_repeat_24px

    override fun controlId() = TAG

    private fun refreshDisplayView() {
        viewModel.rrule.let {
            if (it == null) {
                displayView.text = null
                repeatTypeContainer.visibility = View.GONE
            } else {
                displayView.text = repeatRuleToString.toString(it.toIcal())
                repeatTypeContainer.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_repeat_pref
        private const val TYPE_DUE_DATE = 1
        private const val TYPE_COMPLETION_DATE = 2
        private const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
        private const val REQUEST_RECURRENCE = 10000
    }
}