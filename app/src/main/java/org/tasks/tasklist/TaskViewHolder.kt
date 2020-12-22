package org.tasks.tasklist

import android.app.Activity
import android.graphics.Paint
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import com.google.android.material.chip.ChipGroup
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.ui.CheckableImageView
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.Linkify
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.CheckBoxProvider
import org.tasks.ui.ChipProvider
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

class TaskViewHolder internal constructor(
        private val context: Activity,
        view: ViewGroup,
        private val preferences: Preferences,
        fontSize: Int,
        private val chipProvider: ChipProvider,
        private val checkBoxProvider: CheckBoxProvider,
        private val textColorOverdue: Int,
        private val textColorSecondary: Int,
        private val callback: ViewHolderCallbacks,
        private val metrics: DisplayMetrics,
        private val background: Int,
        private val selectedColor: Int,
        private val rowPadding: Int,
        private val linkify: Linkify,
        private val locale: Locale) : RecyclerView.ViewHolder(view) {

    @BindView(R.id.row)
    lateinit var row: ViewGroup

    @BindView(R.id.due_date)
    lateinit var dueDate: TextView
    
    @BindView(R.id.rowBody)
    lateinit var rowBody: ViewGroup

    @BindView(R.id.title)
    lateinit var nameView: TextView

    @BindView(R.id.description)
    lateinit var description: TextView

    @BindView(R.id.completeBox)
    lateinit var completeBox: CheckableImageView

    @BindView(R.id.chip_group)
    lateinit var chipGroup: ChipGroup

    @BindView(R.id.hidden_icon)
    lateinit var hiddenIcon: View
    
    lateinit var task: TaskContainer
    
    var indent = 0
        set(value) {
            field = value
            val indentSize = getIndentSize(value)
            val layoutParams = row.layoutParams as MarginLayoutParams
            layoutParams.marginStart = indentSize
            row.layoutParams = layoutParams
        }

    var selected = false
        set(value) {
            field = value
            updateBackground()
        }

    var moving = false
        set(value) {
            field = value
            updateBackground()
        }

    var minIndent = 0
        set(value) {
            field = value
            if (task.targetIndent < value) {
                task.targetIndent = value
            }
        }

    var maxIndent = 0
        set(value) {
            field = value
            if (task.targetIndent > value) {
                task.targetIndent = value
            }
        }

    private fun setTopPadding(padding: Int, vararg views: View) {
        for (v in views) {
            v.setPaddingRelative(v.paddingStart, padding, v.paddingEnd, v.paddingBottom)
        }
    }

    private fun setBottomPadding(padding: Int, vararg views: View) {
        for (v in views) {
            v.setPaddingRelative(v.paddingStart, v.paddingTop, v.paddingEnd, padding)
        }
    }
    
    private fun updateBackground() {
        if (selected || moving) {
            rowBody.setBackgroundColor(selectedColor)
        } else {
            rowBody.setBackgroundResource(background)
            rowBody.background.jumpToCurrentState()
        }
    }

    val shiftSize: Float
        get() = 20 * metrics.density

    private fun getIndentSize(indent: Int) = (indent * shiftSize).roundToInt()

    fun bindView(task: TaskContainer, filter: Filter, sortByDueDate: Boolean) {
        this.task = task
        indent = task.indent
        nameView.text = task.title
        hiddenIcon.visibility = if (task.isHidden) View.VISIBLE else View.GONE
        setupTitleAndCheckbox()
        setupDueDate(sortByDueDate)
        setupChips(filter)
        if (preferences.getBoolean(R.string.p_show_description, true)) {
            description.text = task.notes
            description.visibility = if (task.hasNotes()) View.VISIBLE else View.GONE
        }
        if (preferences.getBoolean(R.string.p_linkify_task_list, false)) {
            linkify.linkify(nameView) { onRowBodyClick() }
            linkify.linkify(description) { onRowBodyClick() }
            nameView.setOnLongClickListener { onRowBodyLongClick() }
            description.setOnLongClickListener { onRowBodyLongClick() }
        }
        when {
            chipGroup.visibility == View.VISIBLE -> {
                setBottomPadding(rowPadding, chipGroup)
                setBottomPadding(0, description, nameView)
            }
            description.visibility == View.VISIBLE -> {
                setBottomPadding(rowPadding, description)
                setBottomPadding(0, nameView)
            }
            else -> {
                setBottomPadding(rowPadding, nameView)
            }
        }
    }

    private fun setupTitleAndCheckbox() {
        if (task.isCompleted) {
            nameView.setTextColor(context.getColor(R.color.text_tertiary))
            nameView.paintFlags = nameView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            nameView.setTextColor(
                    context.getColor(if (task.isHidden) R.color.text_tertiary else R.color.text_primary))
            nameView.paintFlags = nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        completeBox.isChecked = task.isCompleted
        completeBox.setImageDrawable(checkBoxProvider.getCheckBox(task.getTask()))
        completeBox.invalidate()
    }

    private fun setupDueDate(sortByDueDate: Boolean) {
        if (task.hasDueDate()) {
            if (task.isOverdue) {
                dueDate.setTextColor(textColorOverdue)
            } else {
                dueDate.setTextColor(textColorSecondary)
            }
            val dateValue: String? = if (sortByDueDate
                    && task.sortGroup?.startOfDay() == task.dueDate.startOfDay()
                    && preferences.showGroupHeaders()) {
                task.takeIf { it.hasDueTime() }?.let {
                    DateUtilities.getTimeString(context, newDateTime(task.dueDate))
                }
            } else {
                DateUtilities.getRelativeDateTime(context, task.dueDate, locale, FormatStyle.MEDIUM, preferences.alwaysDisplayFullDate, false)
            }
            dueDate.text = dateValue
            dueDate.visibility = View.VISIBLE
        } else {
            dueDate.visibility = View.GONE
        }
    }

    private fun setupChips(filter: Filter) {
        val chips = chipProvider.getChips(filter, indent > 0, task)
        if (chips.isEmpty()) {
            chipGroup.visibility = View.GONE
        } else {
            chipGroup.removeAllViews()
            for (chip in chips) {
                chip.setOnClickListener { v: View -> onChipClick(v) }
                chipGroup.addView(chip)
            }
            chipGroup.visibility = View.VISIBLE
        }
    }

    private fun onChipClick(v: View) {
        val tag = v.tag
        if (tag is Filter) {
            callback.onClick(tag)
        } else if (tag is TaskContainer) {
            callback.toggleSubtasks(tag, !tag.isCollapsed)
        }
    }

    @OnClick(R.id.rowBody)
    fun onRowBodyClick() = callback.onClick(this)

    @OnLongClick(R.id.rowBody)
    fun onRowBodyLongClick(): Boolean = callback.onLongPress(this)

    @OnClick(R.id.completeBox)
    fun onCompleteBoxClick() {
        val newState = completeBox.isChecked
        if (newState != task.isCompleted) {
            callback.onCompletedTask(task, newState)
        }

        // set check box to actual action item state
        setupTitleAndCheckbox()
    }

    @OnClick(R.id.due_date)
    fun changeDueDate() {
        callback.onChangeDueDate(task)
    }

    interface ViewHolderCallbacks {
        fun onCompletedTask(task: TaskContainer, newState: Boolean)
        fun onClick(taskViewHolder: TaskViewHolder)
        fun onClick(filter: Filter)
        fun toggleSubtasks(task: TaskContainer, collapsed: Boolean)
        fun onLongPress(taskViewHolder: TaskViewHolder): Boolean
        fun onChangeDueDate(task: TaskContainer)
    }

    init {
        ButterKnife.bind(this, view)
        if (preferences.getBoolean(R.string.p_fullTaskTitle, false)) {
            nameView.maxLines = Int.MAX_VALUE
            nameView.isSingleLine = false
            nameView.ellipsize = null
        }
        if (preferences.getBoolean(R.string.p_show_full_description, false)) {
            description.maxLines = Int.MAX_VALUE
            description.isSingleLine = false
            description.ellipsize = null
        }
        setTopPadding(rowPadding, nameView, completeBox, dueDate, hiddenIcon)
        setBottomPadding(rowPadding, completeBox, dueDate)
        nameView.textSize = fontSize.toFloat()
        description.textSize = fontSize.toFloat()
        val fontSizeDetails = max(10, fontSize - 2)
        dueDate.textSize = fontSizeDetails.toFloat()
        view.tag = this
        for (i in 0 until view.childCount) {
            view.getChildAt(i).tag = this
        }
    }
}