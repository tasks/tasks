/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeBase
import org.tasks.ui.HiddenTopArrayAdapter
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import javax.inject.Inject

/**
 * Control set for specifying when a task should be hidden
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class HideUntilControlSet : TaskEditControlFragment(), OnItemSelectedListener {
    private val spinnerItems: MutableList<HideUntilValue> = ArrayList()

    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale

    @BindView(R.id.hideUntil)
    lateinit var spinner: Spinner

    @BindView(R.id.clear)
    lateinit var clearButton: ImageView

    private lateinit var adapter: ArrayAdapter<HideUntilValue>
    private var previousSetting = Task.HIDE_UNTIL_NONE
    private var selection = 0
    private var existingDate = EXISTING_TIME_UNSET.toLong()
    private var selectedValue: HideUntilValue? = null

    @OnClick(R.id.clear)
    fun clearHideUntil() {
        updateSpinnerOptions(0)
        selection = 0
        spinner.setSelection(selection)
        refreshDisplayView()
    }

    override fun onRowClick() {
        spinner.performClick()
    }

    override val isClickable: Boolean
        get() = true

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        adapter = object : HiddenTopArrayAdapter<HideUntilValue>(
                activity, android.R.layout.simple_spinner_item, spinnerItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var selectedItemPosition = position
                if (parent is AdapterView<*>) {
                    selectedItemPosition = parent.selectedItemPosition
                }
                val tv = inflater.inflate(android.R.layout.simple_spinner_item, parent, false) as TextView
                tv.setPadding(0, 0, 0, 0)
                val value = getItem(selectedItemPosition)
                if (value!!.setting == Task.HIDE_UNTIL_NONE) {
                    clearButton.visibility = View.GONE
                    tv.text = value.labelDisplay
                    tv.setTextColor(activity.getColor(R.color.text_tertiary))
                } else {
                    val display = value.labelDisplay
                    tv.text = getString(R.string.TEA_hideUntil_display, display)
                    tv.setTextColor(activity.getColor(R.color.text_primary))
                }
                return tv
            }
        }
        if (savedInstanceState == null) {
            val dueDate = task.dueDate
            var hideUntil = task.hideUntil
            val dueDay = DateTimeUtils.newDateTime(dueDate)
                    .withHourOfDay(0)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0)

            // For the hide until due case, we need the time component
            val dueTime = dueDate / 1000L * 1000L
            if (hideUntil <= 0) {
                selection = 0
                hideUntil = 0
                if (task.isNew) {
                    when (preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)) {
                        Task.HIDE_UNTIL_DUE -> selection = 1
                        Task.HIDE_UNTIL_DAY_BEFORE -> selection = 3
                        Task.HIDE_UNTIL_WEEK_BEFORE -> selection = 4
                    }
                }
            } else if (hideUntil == dueDay.millis) {
                selection = 1
                hideUntil = 0
            } else if (hideUntil == dueTime) {
                selection = 2
                hideUntil = 0
            } else if (hideUntil + DateUtilities.ONE_DAY == dueDay.millis) {
                selection = 3
                hideUntil = 0
            } else if (hideUntil + DateUtilities.ONE_WEEK == dueDay.millis) {
                selection = 4
                hideUntil = 0
            }
            updateSpinnerOptions(hideUntil)
        } else {
            updateSpinnerOptions(savedInstanceState.getLong(EXTRA_CUSTOM))
            selection = savedInstanceState.getInt(EXTRA_SELECTION)
        }
        spinner.adapter = adapter
        spinner.setSelection(selection)
        spinner.onItemSelectedListener = this
        refreshDisplayView()
        return view
    }

    override val layout: Int
        get() = R.layout.control_set_hide

    override val icon: Int
        get() = R.drawable.ic_outline_visibility_off_24px

    override fun controlId(): Int {
        return TAG
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_HIDE_UNTIL) {
            if (resultCode == Activity.RESULT_OK) {
                setCustomDate(data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun apply(task: Task) {
        task.hideUntil = getHideUntil(task)
    }

    override fun hasChanges(original: Task): Boolean {
        return original.hideUntil != getHideUntil(original)
    }

    private fun getHideUntil(task: Task): Long {
        return task.createHideUntil(selectedValue!!.setting, selectedValue!!.date)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_CUSTOM, existingDate)
        outState.putInt(EXTRA_SELECTION, selection)
    }

    override fun inject(component: FragmentComponent) {
        component.inject(this)
    }

    private fun updateSpinnerOptions(specificDate: Long) {
        spinnerItems.clear()
        // set up base values
        val labelsSpinner = resources.getStringArray(R.array.TEA_hideUntil_spinner)
        val labelsDisplay = resources.getStringArray(R.array.TEA_hideUntil_display)
        spinnerItems.addAll(
                ArrayList(
                        listOf(
                                HideUntilValue(labelsSpinner[0], labelsDisplay[0], Task.HIDE_UNTIL_DUE),
                                HideUntilValue(labelsSpinner[1], labelsDisplay[1], Task.HIDE_UNTIL_DUE_TIME),
                                HideUntilValue(labelsSpinner[2], labelsDisplay[2], Task.HIDE_UNTIL_DAY_BEFORE),
                                HideUntilValue(labelsSpinner[3], labelsDisplay[3], Task.HIDE_UNTIL_WEEK_BEFORE),
                                HideUntilValue(
                                        labelsSpinner[4],
                                        "",
                                        Task.HIDE_UNTIL_SPECIFIC_DAY,
                                        -1)))) // no need for a string for display here, since the chosen day will be
        // displayed
        existingDate = if (specificDate > 0) {
            spinnerItems.add(0, getHideUntilValue(specificDate))
            specificDate
        } else {
            spinnerItems.add(
                    0, HideUntilValue(getString(R.string.TEA_hideUntil_label), Task.HIDE_UNTIL_NONE))
            EXISTING_TIME_UNSET.toLong()
        }
        adapter.notifyDataSetChanged()
    }

    private fun getHideUntilValue(timestamp: Long): HideUntilValue {
        val hideUntilAsDate = DateTimeUtils.newDateTime(timestamp)
        return if (hideUntilAsDate.hourOfDay == 0 && hideUntilAsDate.minuteOfHour == 0 && hideUntilAsDate.secondOfMinute == 0) {
            HideUntilValue(
                    DateUtilities.getDateString(context, DateTimeUtils.newDateTime(timestamp)),
                    Task.HIDE_UNTIL_SPECIFIC_DAY,
                    timestamp)
        } else {
            HideUntilValue(
                    DateUtilities.getLongDateStringWithTime(timestamp, locale.locale),
                    Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
                    timestamp)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // if specific date selected, show dialog
        // ... at conclusion of dialog, update our list
        val item = adapter.getItem(position)
        if (item!!.date == SPECIFIC_DATE.toLong()) {
            val customDate = DateTimeUtils.newDateTime(if (existingDate == EXISTING_TIME_UNSET.toLong()) DateUtilities.now() else existingDate)
                    .withSecondOfMinute(0)
            val intent = Intent(activity, DateAndTimePickerActivity::class.java)
            intent.putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, customDate.millis)
            startActivityForResult(intent, REQUEST_HIDE_UNTIL)
            spinner.setSelection(previousSetting)
        } else {
            previousSetting = position
        }
        selection = spinner.selectedItemPosition
        refreshDisplayView()
    }

    // --- listening for events
    private fun setCustomDate(timestamp: Long) {
        updateSpinnerOptions(timestamp)
        spinner.setSelection(0)
        refreshDisplayView()
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // ignore
    }

    private fun refreshDisplayView() {
        selectedValue = adapter.getItem(selection)
        clearButton.visibility = if (selectedValue!!.setting == Task.HIDE_UNTIL_NONE) View.GONE else View.VISIBLE
    }

    private class HideUntilValue @JvmOverloads internal constructor(val labelSpinner: String, val labelDisplay: String, val setting: Int, val date: Long = 0) {

        internal constructor(label: String, setting: Int) : this(label, label, setting, 0)

        internal constructor(label: String, setting: Int, date: Long) : this(label, label, setting, date)

        override fun toString(): String {
            return labelSpinner
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_hide_until_pref
        private const val EXTRA_CUSTOM = "extra_custom"
        private const val EXTRA_SELECTION = "extra_selection"
        private const val SPECIFIC_DATE = -1
        private const val EXISTING_TIME_UNSET = -2
        private const val REQUEST_HIDE_UNTIL = 11011
    }
}