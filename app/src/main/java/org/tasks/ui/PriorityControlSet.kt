package org.tasks.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatRadioButton
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.injection.FragmentComponent
import org.tasks.themes.ColorProvider
import javax.inject.Inject

class PriorityControlSet : TaskEditControlFragment() {
    @Inject lateinit var colorProvider: ColorProvider

    @BindView(R.id.priority_high)
    lateinit var priorityHigh: AppCompatRadioButton

    @BindView(R.id.priority_medium)
    lateinit var priorityMedium: AppCompatRadioButton

    @BindView(R.id.priority_low)
    lateinit var priorityLow: AppCompatRadioButton

    @BindView(R.id.priority_none)
    lateinit var priorityNone: AppCompatRadioButton

    @Task.Priority
    private var priority = 0

    override fun inject(component: FragmentComponent) = component.inject(this)

    @OnClick(R.id.priority_high, R.id.priority_medium, R.id.priority_low, R.id.priority_none)
    fun onPriorityChanged() {
        priority = getPriority()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        priority = savedInstanceState?.getInt(EXTRA_PRIORITY) ?: task.priority
        when (priority) {
            0 -> priorityHigh.isChecked = true
            1 -> priorityMedium.isChecked = true
            2 -> priorityLow.isChecked = true
            else -> priorityNone.isChecked = true
        }
        tintRadioButton(priorityHigh, 0)
        tintRadioButton(priorityMedium, 1)
        tintRadioButton(priorityLow, 2)
        tintRadioButton(priorityNone, 3)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_PRIORITY, priority)
    }

    override val layout: Int
        get() = R.layout.control_set_priority

    override val icon: Int
        get() = R.drawable.ic_outline_flag_24px

    override fun controlId() = TAG

    override fun apply(task: Task) {
        task.priority = priority
    }

    override fun hasChanges(original: Task): Boolean {
        return original.priority != priority
    }

    private fun tintRadioButton(radioButton: AppCompatRadioButton, priority: Int) {
        val color = colorProvider.getPriorityColor(priority, true)
        radioButton.buttonTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(color, color))
    }

    @Task.Priority
    private fun getPriority(): Int {
        if (priorityHigh.isChecked) {
            return Task.Priority.HIGH
        }
        if (priorityMedium.isChecked) {
            return Task.Priority.MEDIUM
        }
        return if (priorityLow.isChecked) {
            Task.Priority.LOW
        } else {
            Task.Priority.NONE
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_importance_pref
        private const val EXTRA_PRIORITY = "extra_priority"
    }
}