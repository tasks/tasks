package org.tasks.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.subtasks.SubtasksHelper
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.locale.Locale
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.ui.CheckBoxProvider
import timber.log.Timber
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.max

internal class ScrollableViewsFactory(
        private val subtasksHelper: SubtasksHelper,
        private val preferences: Preferences,
        private val context: Context,
        private val widgetId: Int,
        private val taskDao: TaskDao,
        private val defaultFilterProvider: DefaultFilterProvider,
        private val checkBoxProvider: CheckBoxProvider,
        private val locale: Locale) : RemoteViewsFactory {
    private val indentPadding: Int
    private var isDark = false
    private var showDueDates = false
    private var endDueDate = false
    private var showCheckboxes = false
    private var textSize = 0f
    private var dueDateTextSize = 0f
    private var filter: Filter? = null
    private var textColorPrimary = 0
    private var textColorSecondary = 0
    private var showFullTaskTitle = false
    private var showDescription = false
    private var showFullDescription = false
    private var vPad = 0
    private var hPad = 0
    private var handleDueDateClick = false
    private var showDividers = false
    private var showSubtasks = false
    private var isRtl = false
    private var tasks: List<TaskContainer> = ArrayList()
    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            updateSettings()
            tasks = taskDao.fetchTasks { subtasks: SubtaskInfo ->
                getQuery(filter, subtasks)
            }
        }
    }

    override fun onDestroy() {}
    override fun getCount(): Int {
        return tasks.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        return buildUpdate(position)!!
    }

    override fun getLoadingView(): RemoteViews {
        return newRemoteView()
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        val task = getTask(position)
        return task?.id ?: 0
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun getCheckbox(task: Task): Bitmap {
        return checkBoxProvider.getWidgetCheckBox(task)
    }

    private fun newRemoteView(): RemoteViews {
        return RemoteViews(
                BuildConfig.APPLICATION_ID, if (isDark) R.layout.widget_row_dark else R.layout.widget_row_light)
    }

    private fun buildUpdate(position: Int): RemoteViews? {
        try {
            val taskContainer = getTask(position) ?: return null
            val task = taskContainer.getTask()
            var textColorTitle = textColorPrimary
            val row = newRemoteView()
            if (task.isHidden) {
                textColorTitle = textColorSecondary
                row.setViewVisibility(R.id.hidden_icon, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.hidden_icon, View.GONE)
            }
            if (task.isCompleted) {
                textColorTitle = textColorSecondary
                row.setInt(
                        R.id.widget_text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            } else {
                row.setInt(R.id.widget_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            }
            row.setFloat(R.id.widget_text, "setTextSize", textSize)
            if (showDueDates) {
                formatDueDate(row, task)
            } else {
                row.setViewVisibility(R.id.widget_due_bottom, View.GONE)
                row.setViewVisibility(R.id.widget_due_end, View.GONE)
                if (task.hasDueDate() && task.isOverdue) {
                    textColorTitle = context.getColor(R.color.overdue)
                }
            }
            if (showFullTaskTitle) {
                row.setInt(R.id.widget_text, "setMaxLines", Int.MAX_VALUE)
            }
            row.setTextViewText(R.id.widget_text, task.title)
            row.setTextColor(R.id.widget_text, textColorTitle)
            if (showDescription && task.hasNotes()) {
                row.setFloat(R.id.widget_description, "setTextSize", textSize)
                row.setTextViewText(R.id.widget_description, task.notes)
                row.setViewVisibility(R.id.widget_description, View.VISIBLE)
                if (showFullDescription) {
                    row.setInt(R.id.widget_description, "setMaxLines", Int.MAX_VALUE)
                }
            } else {
                row.setViewVisibility(R.id.widget_description, View.GONE)
            }
            row.setOnClickFillInIntent(
                    R.id.widget_row,
                    Intent(WidgetClickActivity.EDIT_TASK)
                            .putExtra(WidgetClickActivity.EXTRA_FILTER, filter)
                            .putExtra(WidgetClickActivity.EXTRA_TASK, task))
            if (showCheckboxes) {
                row.setViewPadding(R.id.widget_complete_box, hPad, vPad, hPad, vPad)
                row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task))
                row.setOnClickFillInIntent(
                        R.id.widget_complete_box,
                        Intent(WidgetClickActivity.COMPLETE_TASK)
                                .putExtra(WidgetClickActivity.EXTRA_TASK, task))
            } else {
                row.setViewPadding(R.id.widget_complete_box, hPad, 0, 0, 0)
                row.setInt(R.id.widget_complete_box, "setBackgroundResource", 0)
            }
            row.setViewPadding(R.id.top_padding, 0, vPad, 0, 0)
            row.setViewPadding(R.id.bottom_padding, 0, vPad, 0, 0)
            if (!showDividers) {
                row.setViewVisibility(R.id.divider, View.GONE)
            }
            if (showSubtasks && taskContainer.hasChildren()) {
                row.setOnClickFillInIntent(
                        R.id.subtask_button,
                        Intent(WidgetClickActivity.TOGGLE_SUBTASKS)
                                .putExtra(WidgetClickActivity.EXTRA_TASK, task)
                                .putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !taskContainer.isCollapsed))
                row.setTextViewText(
                        R.id.subtask_text,
                        context
                                .resources
                                .getQuantityString(
                                        R.plurals.subtask_count, taskContainer.children, taskContainer.children))
                row.setImageViewResource(
                        R.id.subtask_icon,
                        if (taskContainer.isCollapsed) R.drawable.ic_keyboard_arrow_up_black_18dp else R.drawable.ic_keyboard_arrow_down_black_18dp)
                row.setViewVisibility(R.id.subtask_button, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.subtask_button, View.GONE)
            }
            row.setInt(R.id.widget_row, "setLayoutDirection", locale.directionality)
            val startPad = taskContainer.getIndent() * indentPadding
            row.setViewPadding(R.id.widget_row, if (isRtl) 0 else startPad, 0, if (isRtl) startPad else 0, 0)
            return row
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    private fun getTask(position: Int): TaskContainer? {
        return if (position < tasks.size) tasks[position] else null
    }

    private suspend fun getQuery(filter: Filter?, subtasks: SubtaskInfo): List<String> {
        val queries = getQuery(preferences, filter!!, subtasks)
        val last = queries.size - 1
        queries[last] = subtasksHelper.applySubtasksToWidgetFilter(filter, queries[last])
        return queries
    }

    private fun formatDueDate(row: RemoteViews, task: Task) {
        val dueDateRes = if (endDueDate) R.id.widget_due_end else R.id.widget_due_bottom
        row.setViewVisibility(if (endDueDate) R.id.widget_due_bottom else R.id.widget_due_end, View.GONE)
        val hasDueDate = task.hasDueDate()
        val endPad = if (hasDueDate && endDueDate) 0 else hPad
        row.setViewPadding(R.id.widget_text, if (isRtl) endPad else 0, 0, if (isRtl) 0 else endPad, 0)
        if (hasDueDate) {
            if (endDueDate) {
                row.setViewPadding(R.id.widget_due_end, hPad, vPad, hPad, vPad)
            }
            row.setViewVisibility(dueDateRes, View.VISIBLE)
            row.setTextViewText(
                    dueDateRes,
                    DateUtilities.getRelativeDateTime(
                            context, task.dueDate, locale.locale, FormatStyle.MEDIUM))
            row.setTextColor(
                    dueDateRes,
                    if (task.isOverdue) context.getColor(R.color.overdue) else textColorSecondary)
            row.setFloat(dueDateRes, "setTextSize", dueDateTextSize)
            if (handleDueDateClick) {
                row.setOnClickFillInIntent(
                        dueDateRes,
                        Intent(WidgetClickActivity.RESCHEDULE_TASK)
                                .putExtra(WidgetClickActivity.EXTRA_TASK, task))
            } else {
                row.setInt(dueDateRes, "setBackgroundResource", 0)
            }
        } else {
            row.setViewVisibility(dueDateRes, View.GONE)
        }
    }

    private suspend fun updateSettings() {
        val widgetPreferences = WidgetPreferences(context, preferences, widgetId)
        vPad = widgetPreferences.widgetSpacing
        hPad = context.resources.getDimension(R.dimen.widget_padding).toInt()
        handleDueDateClick = widgetPreferences.rescheduleOnDueDateClick()
        showFullTaskTitle = widgetPreferences.showFullTaskTitle()
        showDescription = widgetPreferences.showDescription()
        showFullDescription = widgetPreferences.showFullDescription()
        isDark = widgetPreferences.themeIndex > 0
        textColorPrimary = context.getColor(if (isDark) R.color.white_87 else R.color.black_87)
        textColorSecondary = context.getColor(if (isDark) R.color.white_60 else R.color.black_60)
        val dueDatePosition = widgetPreferences.dueDatePosition
        showDueDates = dueDatePosition != 2
        endDueDate = dueDatePosition != 1
        showCheckboxes = widgetPreferences.showCheckboxes()
        textSize = widgetPreferences.fontSize.toFloat()
        dueDateTextSize = max(10f, textSize - 2)
        filter = defaultFilterProvider.getFilterFromPreference(widgetPreferences.filterId)
        showDividers = widgetPreferences.showDividers()
        showSubtasks = widgetPreferences.showSubtasks()
        isRtl = locale.directionality == View.LAYOUT_DIRECTION_RTL
    }

    init {
        val metrics = context.resources.displayMetrics
        indentPadding = (20 * metrics.density).toInt()
    }
}