package org.tasks.tasklist

import android.app.Activity
import android.graphics.Paint
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.themeadapter.appcompat.AppCompatTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.core.SortHelper.SORT_LIST
import com.todoroo.astrid.core.SortHelper.SORT_START
import com.todoroo.astrid.ui.CheckableImageView
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.data.TaskContainer
import org.tasks.databinding.TaskAdapterRowBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.Linkify
import org.tasks.markdown.Markdown
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.CheckBoxProvider
import org.tasks.ui.ChipProvider
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class TaskViewHolder internal constructor(
    private val context: Activity,
    binding: TaskAdapterRowBinding,
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
    private val rowPaddingDp: Int,
    private val rowPaddingPx: Int,
    private val linkify: Linkify,
    private val locale: Locale,
    private val markdown: Markdown
) : RecyclerView.ViewHolder(binding.root) {
    private val row: ViewGroup = binding.row
    private val dueDate: TextView = binding.dueDate.apply {
        setOnClickListener { changeDueDate() }
    }
    private val rowBody: ViewGroup = binding.rowBody.apply {
        setOnClickListener { callback.onClick(this@TaskViewHolder) }
        setOnLongClickListener { callback.onLongPress(this@TaskViewHolder) }
    }
    private val nameView: TextView = binding.title
    private val description: TextView = binding.description
    private val completeBox: CheckableImageView = binding.completeBox.apply {
        setOnClickListener { onCompleteBoxClick() }
    }
    private val chipGroup: ComposeView = binding.chipGroup
    private val alwaysDisplayFullDate: Boolean = preferences.alwaysDisplayFullDate
    private val showGroupHeaders = preferences.showGroupHeaders()

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

    fun bindView(task: TaskContainer, filter: Filter, sortMode: Int) {
        this.task = task
        indent = task.indent
        markdown.setMarkdown(nameView, task.title)
        setupTitleAndCheckbox()
        setupDueDate(sortMode == SORT_DUE)
        setupChips(
            filter = filter,
            sortByStartDate = sortMode == SORT_START && showGroupHeaders,
            sortByList = sortMode == SORT_LIST && showGroupHeaders
        )
        if (preferences.getBoolean(R.string.p_show_description, true)) {
            markdown.setMarkdown(description, task.notes)
            description.visibility = if (task.hasNotes()) View.VISIBLE else View.GONE
        }
        if (markdown.enabled || preferences.getBoolean(R.string.p_linkify_task_list, false)) {
            linkify.setMovementMethod(
                nameView,
                linkClickHandler = { url -> callback.onLinkClicked(this, url) },
                rowClickHandler = { callback.onClick(this) }
            )
            linkify.setMovementMethod(
                description,
                linkClickHandler = { url -> callback.onLinkClicked(this, url) },
                rowClickHandler = { callback.onClick(this) }
            )
            if (!markdown.enabled) {
                Linkify.safeLinkify(nameView)
                Linkify.safeLinkify(description)
            }
            nameView.setOnLongClickListener { callback.onLongPress(this) }
            description.setOnLongClickListener { callback.onLongPress(this) }
        }
        when {
            chipGroup.visibility == View.VISIBLE -> {
                setBottomPadding(0, description, nameView)
            }
            description.visibility == View.VISIBLE -> {
                setBottomPadding(rowPaddingPx, description)
                setBottomPadding(0, nameView)
            }
            else -> {
                setBottomPadding(rowPaddingPx, nameView)
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
        completeBox.setImageDrawable(checkBoxProvider.getCheckBox(task.task))
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
                    && (task.sortGroup ?: 0) >= now().startOfDay()
                    && showGroupHeaders
            ) {
                task.takeIf { it.hasDueTime() }?.let {
                    DateUtilities.getTimeString(context, newDateTime(task.dueDate))
                }
            } else {
                DateUtilities.getRelativeDateTime(context, task.dueDate, locale, FormatStyle.MEDIUM, alwaysDisplayFullDate, false)
            }
            dueDate.text = dateValue
            dueDate.visibility = View.VISIBLE
        } else {
            dueDate.visibility = View.GONE
        }
    }

    private fun setupChips(filter: Filter, sortByStartDate: Boolean, sortByList: Boolean) {
        chipGroup.setContent {
            AppCompatTheme {
                ChipGroup(
                    modifier = Modifier.padding(
                        end = 16.dp,
                        bottom = rowPaddingDp.dp
                    )
                ) {
                    chipProvider.Chips(
                        filter = filter,
                        id = task.id,
                        children = task.children,
                        collapsed = task.isCollapsed,
                        isHidden = task.isHidden,
                        sortGroup = task.sortGroup,
                        startDate = task.task.hideUntil,
                        place = task.location?.place,
                        list = task.caldav,
                        tagsString = task.tagsString,
                        isSubtask = task.hasParent(),
                        isGoogleTask = task.isGoogleTask,
                        sortByStartDate = sortByStartDate,
                        sortByList = sortByList,
                        toggleSubtasks = { task: Long, collapsed: Boolean -> callback.toggleSubtasks(task, collapsed) },
                        onClick = { it: Filter -> callback.onClick(it) },
                    )
                }
            }
        }
    }

    private fun onCompleteBoxClick() {
        val newState = completeBox.isChecked
        if (newState != task.isCompleted) {
            callback.onCompletedTask(task, newState)
        }

        // set check box to actual action item state
        setupTitleAndCheckbox()
    }

    private fun changeDueDate() {
        callback.onChangeDueDate(task)
    }

    interface ViewHolderCallbacks {
        fun onCompletedTask(task: TaskContainer, newState: Boolean)
        fun onLinkClicked(vh: TaskViewHolder, url: String): Boolean
        fun onClick(taskViewHolder: TaskViewHolder)
        fun onClick(filter: Filter)
        fun toggleSubtasks(task: Long, collapsed: Boolean)
        fun onLongPress(taskViewHolder: TaskViewHolder): Boolean
        fun onChangeDueDate(task: TaskContainer)
    }

    init {
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
        setTopPadding(rowPaddingPx, nameView, completeBox, dueDate)
        setBottomPadding(rowPaddingPx, completeBox, dueDate)
        nameView.textSize = fontSize.toFloat()
        description.textSize = fontSize.toFloat()
        val fontSizeDetails = max(10, fontSize - 2)
        dueDate.textSize = fontSizeDetails.toFloat()
        with(binding.root) {
            tag = this@TaskViewHolder
            for (i in 0 until childCount) {
                getChildAt(i).tag = this@TaskViewHolder
            }
        }
    }
}