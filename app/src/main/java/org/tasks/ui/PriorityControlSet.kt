package org.tasks.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatRadioButton
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetPriorityBinding
import org.tasks.themes.ColorProvider
import javax.inject.Inject

@AndroidEntryPoint
class PriorityControlSet : TaskEditControlFragment() {
    @Inject lateinit var colorProvider: ColorProvider

    private lateinit var priorityHigh: AppCompatRadioButton
    private lateinit var priorityMedium: AppCompatRadioButton
    private lateinit var priorityLow: AppCompatRadioButton
    private lateinit var priorityNone: AppCompatRadioButton

    private fun onPriorityChanged() {
        viewModel.priority = getPriority()
    }

    override fun createView(savedInstanceState: Bundle?) {
        when (viewModel.priority) {
            0 -> priorityHigh.isChecked = true
            1 -> priorityMedium.isChecked = true
            2 -> priorityLow.isChecked = true
            else -> priorityNone.isChecked = true
        }
        tintRadioButton(priorityHigh, 0)
        tintRadioButton(priorityMedium, 1)
        tintRadioButton(priorityLow, 2)
        tintRadioButton(priorityNone, 3)
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetPriorityBinding.inflate(layoutInflater, parent, true).let {
            priorityHigh = it.priorityHigh.apply {
                setOnClickListener { onPriorityChanged() }
            }
            priorityMedium = it.priorityMedium.apply {
                setOnClickListener { onPriorityChanged() }
            }
            priorityLow = it.priorityLow.apply {
                setOnClickListener { onPriorityChanged() }
            }
            priorityNone = it.priorityNone.apply {
                setOnClickListener { onPriorityChanged() }
            }
            it.root
        }

    override val icon = R.drawable.ic_outline_flag_24px

    override fun controlId() = TAG

    private fun tintRadioButton(radioButton: AppCompatRadioButton, priority: Int) {
        val color = colorProvider.getPriorityColor(priority, true)
        radioButton.buttonTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(color, color))
    }

    @Task.Priority
    private fun getPriority() = when {
        priorityHigh.isChecked -> Task.Priority.HIGH
        priorityMedium.isChecked -> Task.Priority.MEDIUM
        priorityLow.isChecked -> Task.Priority.LOW
        else -> Task.Priority.NONE
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_importance_pref
    }
}