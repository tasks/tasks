package org.tasks.tasklist

import android.graphics.Paint
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.ui.CheckableImageView
import org.tasks.data.TaskContainer
import org.tasks.databinding.SubtaskAdapterRowBodyBinding
import org.tasks.ui.CheckBoxProvider
import org.tasks.ui.ChipProvider
import kotlin.math.roundToInt

class SubtaskViewHolder internal constructor(
    binding: SubtaskAdapterRowBodyBinding,
    private val callbacks: Callbacks,
    private val metrics: DisplayMetrics,
    private val chipProvider: ChipProvider,
    private val checkBoxProvider: CheckBoxProvider
) : RecyclerView.ViewHolder(binding.root) {
    private var task: TaskContainer? = null
    private val rowBody: ViewGroup
    private val nameView: TextView
    private val completeBox: CheckableImageView
    private val chipGroup: ComposeView

    init {
        rowBody = binding.rowBody
        nameView = binding.title
        completeBox = binding.completeBox
        chipGroup = binding.chipGroup
        nameView.setOnClickListener { v: View? -> openSubtask() }
        completeBox.setOnClickListener { v: View? -> onCompleteBoxClick() }
        val view: ViewGroup = binding.root
        view.tag = this
        for (i in 0 until view.childCount) {
            view.getChildAt(i).tag = this
        }
    }

    private val shiftSize: Float
        get() = 20 * metrics.density

    private fun getIndentSize(indent: Int): Int {
        return (indent * shiftSize).roundToInt()
    }

    fun bindView(task: TaskContainer) {
        this.task = task
        setIndent(task.indent)
        chipGroup.setContent {
            MdcTheme {
                if (task.hasChildren()) {
                    chipProvider.SubtaskChip(task = task, compact = true) {
                        callbacks.toggleSubtask(task.id, !task.isCollapsed)
                    }
                }
            }
        }
        nameView.text = task.title
        setupTitleAndCheckbox()
    }

    private fun setupTitleAndCheckbox() {
        if (task!!.isCompleted) {
            nameView.isEnabled = false
            nameView.paintFlags = nameView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            nameView.isEnabled = !task!!.isHidden
            nameView.paintFlags = nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        completeBox.isChecked = task!!.isCompleted
        completeBox.setImageDrawable(checkBoxProvider.getCheckBox(task!!.getTask()))
        completeBox.invalidate()
    }

    private fun openSubtask() {
        callbacks.openSubtask(task!!.getTask())
    }

    private fun onCompleteBoxClick() {
        if (task == null) {
            return
        }
        val newState = completeBox.isChecked
        if (newState != task!!.isCompleted) {
            callbacks.complete(task!!.getTask(), newState)
        }

        // set check box to actual action item state
        setupTitleAndCheckbox()
    }

    private fun setIndent(indent: Int) {
        val indentSize = getIndentSize(indent)
        val layoutParams = rowBody.layoutParams as MarginLayoutParams
        layoutParams.marginStart = indentSize
        rowBody.layoutParams = layoutParams
    }

    interface Callbacks {
        fun openSubtask(task: Task)
        fun toggleSubtask(taskId: Long, collapsed: Boolean)
        fun complete(task: Task, completed: Boolean)
    }
}