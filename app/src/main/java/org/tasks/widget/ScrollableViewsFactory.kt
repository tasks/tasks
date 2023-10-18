package org.tasks.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.subtasks.SubtasksHelper
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.date.DateTimeUtils
import org.tasks.extensions.Context.isNightMode
import org.tasks.markdown.Markdown
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.CheckBoxProvider
import timber.log.Timber
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

internal class ScrollableViewsFactory(
    private val subtasksHelper: SubtasksHelper,
    preferences: Preferences,
    private val context: Context,
    private val widgetId: Int,
    private val taskDao: TaskDao,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val checkBoxProvider: CheckBoxProvider,
    private val locale: Locale,
    private val chipProvider: ChipProvider,
    private val localBroadcastManager: LocalBroadcastManager,
    private val markdown: Markdown,
    private val headerFormatter: HeaderFormatter,
) : RemoteViewsFactory {
    private val indentPadding: Int
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
    private var disableGroups = false
    private var showSubtasks = false
    private var showStartDates = false
    private var showPlaces = false
    private var showLists = false
    private var showTags = false
    private var collapsed = mutableSetOf(HEADER_COMPLETED)
    private var groupMode = -1
    private var subtaskMode = -1
    private var tasks = SectionedDataSource(
        emptyList(),
        disableHeaders = false,
        groupMode = SortHelper.GROUP_NONE,
        subtaskMode = SortHelper.SORT_MANUAL,
        collapsed,
        preferences.completedTasksAtBottom,
    )
    private val widgetPreferences = WidgetPreferences(context, preferences, widgetId)
    private var isDark = checkIfDark
    private var showFullDate = false
    private var compact = false

    private val checkIfDark: Boolean
        get() = when (widgetPreferences.themeIndex) {
            0 -> false
            3 -> context.isNightMode
            else -> true
        }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            updateSettings()
            tasks = SectionedDataSource(
                    taskDao.fetchTasks { getQuery(filter) },
                    disableGroups,
                    groupMode,
                    subtaskMode,
                    collapsed,
                    widgetPreferences.completedTasksAtBottom,
            )
            if (collapsed.retainAll(tasks.getSectionValues())) {
                widgetPreferences.setCollapsed(collapsed)
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        if (isDark != checkIfDark) {
            isDark = !isDark
            localBroadcastManager.reconfigureWidget(widgetId)
        }
        return tasks.size
    }

    override fun getViewAt(position: Int): RemoteViews? =
            if (tasks.isHeader(position)) buildHeader(position) else buildUpdate(position)

    override fun getLoadingView(): RemoteViews = newRemoteView()

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int) = getTask(position)?.id ?: 0

    override fun hasStableIds(): Boolean = true

    private fun getCheckbox(task: Task): Bitmap = checkBoxProvider.getWidgetCheckBox(task)

    private fun newRemoteView(): RemoteViews = RemoteViews(
            BuildConfig.APPLICATION_ID,
            if (isDark) R.layout.widget_row_dark else R.layout.widget_row_light
    )

    private fun buildHeader(position: Int): RemoteViews {
        val row = RemoteViews(
                BuildConfig.APPLICATION_ID,
                if (isDark) R.layout.widget_header_dark else R.layout.widget_header_light
        )
        val section = tasks.getSection(position)
        val sortGroup = section.value
        val header: String? = if (filter?.supportsSorting() == true) {
            headerFormatter.headerStringBlocking(
                value = section.value,
                groupMode = groupMode,
                alwaysDisplayFullDate = showFullDate,
                style = FormatStyle.MEDIUM,
                compact = compact,
            )
        } else {
            null
        }
        row.setTextViewText(R.id.header, header)
        row.setImageViewResource(R.id.arrow, if (section.collapsed) {
            R.drawable.ic_keyboard_arrow_down_black_18dp
        } else {
            R.drawable.ic_keyboard_arrow_up_black_18dp
        })
        row.setTextColor(
            R.id.header,
            section.headerColor(
                context,
                groupMode,
                if (isDark) R.color.white_60 else R.color.black_60
            )
        )
        if (!showDividers) {
            row.setViewVisibility(R.id.divider, View.GONE)
        }
        row.setOnClickFillInIntent(
                R.id.row,
                Intent(WidgetClickActivity.TOGGLE_GROUP)
                        .putExtra(WidgetClickActivity.EXTRA_WIDGET, widgetId)
                        .putExtra(WidgetClickActivity.EXTRA_GROUP, sortGroup)
                        .putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !section.collapsed)
        )
        return row
    }

    private fun buildUpdate(position: Int): RemoteViews? {
        try {
            val taskContainer = getTask(position) ?: return null
            val task = taskContainer.task
            var textColorTitle = textColorPrimary
            val row = newRemoteView()
            if (task.isHidden) {
                textColorTitle = textColorSecondary
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
                formatDueDate(row, taskContainer)
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
            row.setTextViewText(
                R.id.widget_text,
                markdown.toMarkdown(task.title)
            )
            row.setTextColor(R.id.widget_text, textColorTitle)
            if (showDescription && task.hasNotes()) {
                row.setFloat(R.id.widget_description, "setTextSize", textSize)
                row.setTextViewText(
                    R.id.widget_description,
                    markdown.toMarkdown(task.notes)
                )
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
            row.removeAllViews(R.id.chips)
            if (showSubtasks && taskContainer.hasChildren()) {
                val chip = chipProvider.getSubtaskChip(taskContainer)
                row.addView(R.id.chips, chip)
                row.setOnClickFillInIntent(
                        R.id.chip,
                        Intent(WidgetClickActivity.TOGGLE_SUBTASKS)
                                .putExtra(WidgetClickActivity.EXTRA_TASK, task)
                                .putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !taskContainer.isCollapsed)
                )
            }
            if (taskContainer.isHidden && showStartDates) {
                val sortByDate = groupMode == SortHelper.SORT_START && !disableGroups
                chipProvider
                        .getStartDateChip(taskContainer, showFullDate, sortByDate)
                        ?.let { row.addView(R.id.chips, it) }
            }
            if (taskContainer.hasLocation() && showPlaces) {
                chipProvider
                        .getPlaceChip(filter, taskContainer)
                        ?.let { row.addView(R.id.chips, it) }
            }
            if (!taskContainer.hasParent() && showLists) {
                chipProvider
                        .getListChip(filter, taskContainer)
                        ?.let { row.addView(R.id.chips, it) }
            }
            if (showTags && taskContainer.tagsString?.isNotBlank() == true) {
                chipProvider
                        .getTagChips(filter, taskContainer)
                        .forEach { row.addView(R.id.chips, it) }
            }
            val startPad = taskContainer.indent * indentPadding
            row.setViewPadding(R.id.widget_row, startPad, 0, 0, 0)
            return row
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    private fun getTask(position: Int): TaskContainer? = tasks.getItem(position)

    private suspend fun getQuery(filter: Filter?): List<String> {
        val queries = getQuery(widgetPreferences, filter!!)
        val last = queries.size - 1
        queries[last] =
                subtasksHelper.applySubtasksToWidgetFilter(filter, widgetPreferences, queries[last])
        return queries
    }

    private fun formatDueDate(row: RemoteViews, task: TaskContainer) {
        val dueDateRes = if (endDueDate) R.id.widget_due_end else R.id.widget_due_bottom
        row.setViewVisibility(if (endDueDate) R.id.widget_due_bottom else R.id.widget_due_end, View.GONE)
        val hasDueDate = task.hasDueDate()
        val endPad = if (hasDueDate && endDueDate) 0 else hPad
        row.setViewPadding(R.id.widget_text, 0, 0, endPad, 0)
        if (hasDueDate) {
            if (endDueDate) {
                row.setViewPadding(R.id.widget_due_end, hPad, vPad, hPad, vPad)
            }
            row.setViewVisibility(dueDateRes, View.VISIBLE)
            val text = if (
                groupMode == SortHelper.SORT_DUE &&
                (task.sortGroup ?: 0L) >= now().startOfDay() &&
                !disableGroups
            ) {
                task.takeIf { it.hasDueTime() }?.let {
                    DateUtilities.getTimeString(context, DateTimeUtils.newDateTime(task.dueDate))
                }
            } else {
                DateUtilities.getRelativeDateTime(
                        context, task.dueDate, locale, FormatStyle.MEDIUM, showFullDate, false)
            }
            row.setTextViewText(dueDateRes, text)
            row.setTextColor(
                    dueDateRes,
                    if (task.isOverdue) context.getColor(R.color.overdue) else textColorSecondary)
            row.setFloat(dueDateRes, "setTextSize", dueDateTextSize)
            if (handleDueDateClick) {
                row.setOnClickFillInIntent(
                        dueDateRes,
                        Intent(WidgetClickActivity.RESCHEDULE_TASK)
                                .putExtra(WidgetClickActivity.EXTRA_TASK, task.task))
            } else {
                row.setInt(dueDateRes, "setBackgroundResource", 0)
            }
        } else {
            row.setViewVisibility(dueDateRes, View.GONE)
        }
    }

    private suspend fun updateSettings() {
        vPad = widgetPreferences.widgetSpacing
        hPad = context.resources.getDimension(R.dimen.widget_padding).toInt()
        handleDueDateClick = widgetPreferences.rescheduleOnDueDateClick()
        showFullTaskTitle = widgetPreferences.showFullTaskTitle()
        showDescription = widgetPreferences.showDescription()
        showFullDescription = widgetPreferences.showFullDescription()
        chipProvider.isDark = isDark
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
        disableGroups = filter?.let {
            !it.supportsSorting()
                    || (it.supportsManualSort() && widgetPreferences.isManualSort)
                    || (it is AstridOrderingFilter && widgetPreferences.isAstridSort)
        } == true
        showPlaces = widgetPreferences.showPlaces()
        showSubtasks = widgetPreferences.showSubtasks()
        showStartDates = widgetPreferences.showStartDates()
        showLists = widgetPreferences.showLists()
        showTags = widgetPreferences.showTags()
        showFullDate = widgetPreferences.alwaysDisplayFullDate
        widgetPreferences.groupMode.takeIf { it != groupMode }
            ?.let {
                if (groupMode != SortHelper.GROUP_NONE) {
                    widgetPreferences.setCollapsed(mutableSetOf(HEADER_COMPLETED))
                }
                groupMode = it
            }
        subtaskMode = widgetPreferences.subtaskMode
        collapsed = widgetPreferences.collapsed
        compact = widgetPreferences.compact
    }

    init {
        val metrics = context.resources.displayMetrics
        indentPadding = (20 * metrics.density).toInt()
    }
}