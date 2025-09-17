package org.tasks.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.todoroo.andlib.utility.AndroidUtilities.atLeastAndroid16
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.subtasks.SubtasksHelper
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.data.dao.TaskDao
import org.tasks.data.hasNotes
import org.tasks.data.isHidden
import org.tasks.data.isOverdue
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.setBackgroundResource
import org.tasks.extensions.setColorFilter
import org.tasks.extensions.setMaxLines
import org.tasks.extensions.setTextSize
import org.tasks.extensions.strikethrough
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.markdown.Markdown
import org.tasks.tasklist.AdapterSection
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.headerColor
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import org.tasks.ui.CheckBoxProvider.Companion.getCheckboxRes
import timber.log.Timber
import kotlin.math.max

internal class TasksWidgetViewFactory(
    private val subtasksHelper: SubtasksHelper,
    private val widgetPreferences: WidgetPreferences,
    private val filter: Filter,
    private val context: Context,
    private val widgetId: Int,
    private val taskDao: TaskDao,
    private val chipProvider: WidgetChipProvider,
    private val markdown: Markdown,
    private val headerFormatter: HeaderFormatter,
) : RemoteViewsFactory {
    private val taskLimit = if (atLeastAndroid16()) 50 + 1 else Int.MAX_VALUE
    private val indentPadding = (20 * context.resources.displayMetrics.density).toInt()
    private val settings = widgetPreferences.getWidgetListSettings()
    private val hPad = context.resources.getDimension(R.dimen.widget_padding).toInt()
    private val disableGroups = !filter.supportsSorting()
            || (filter.supportsManualSort() && widgetPreferences.isManualSort)
            || (filter is AstridOrderingFilter && widgetPreferences.isAstridSort)
    private var tasks = SectionedDataSource()
    private val onSurface = context.getColor(if (settings.isDark) R.color.white_87 else R.color.black_87)
    private val onSurfaceVariant = context.getColor(if (settings.isDark) R.color.white_60 else R.color.black_60)

    init {
        chipProvider.isDark = settings.isDark

        val widgetThemes = context.resources.getStringArray(R.array.widget_themes)
        val customThemeName = context.getString(R.string.theme_custom)
        val customThemeIndex = widgetThemes.indexOf(customThemeName)

        if (widgetPreferences.themeIndex == customThemeIndex) {
            chipProvider.customBackgroundColor = widgetPreferences.customThemeColor
        }
    }

    override fun onCreate() {
        Timber.d("onCreate widgetId:$widgetId filter:$filter")
    }

    override fun onDataSetChanged() {
        Timber.v("onDataSetChanged $filter")
        runBlocking {
            val collapsed = widgetPreferences.collapsed
            tasks = SectionedDataSource(
                taskDao.fetchTasks(getQuery(filter)),
                disableGroups,
                settings.groupMode,
                widgetPreferences.subtaskMode,
                collapsed,
                widgetPreferences.completedTasksAtBottom,
            )
            collapsed.toMutableSet().let {
                if (it.retainAll(tasks.getSectionValues().toSet())) {
                    widgetPreferences.collapsed = it
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy widgetId:$widgetId")
    }

    override fun getCount() = tasks.size.coerceAtMost(taskLimit)

    override fun getViewAt(position: Int): RemoteViews? = tasks.let {
        when {
            position == taskLimit - 1 && it.size > taskLimit -> buildFooter()
            it.isHeader(position) -> buildHeader(it.getSection(position))
            position < it.size -> buildUpdate(it.getItem(position))
            else -> null
        }
    }

    override fun getLoadingView(): RemoteViews = newRemoteView()

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int) = tasks.let {
        when {
            position == taskLimit - 1 && it.size > taskLimit -> 0
            it.isHeader(position) -> it.getSection(position).value
            position < it.size -> it.getItem(position).id
            else -> 0
        }
    }

    override fun hasStableIds(): Boolean = true

    private fun newRemoteView() = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row)

    private fun buildFooter(): RemoteViews {
        return RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_footer).apply {
            setTextSize(R.id.widget_view_more, settings.textSize)
            setTextColor(R.id.widget_view_more, onSurface)
            setOnClickFillInIntent(
                R.id.widget_view_more,
                Intent(WidgetClickActivity.OPEN_TASK_LIST)
                    .putExtra(WidgetClickActivity.EXTRA_FILTER, filter)
            )
        }
    }

    private fun buildHeader(section: AdapterSection): RemoteViews {
        val sortGroup = section.value
        val header: String? = if (filter.supportsSorting()) {
            headerFormatter.headerStringBlocking(
                value = section.value,
                groupMode = settings.groupMode,
                alwaysDisplayFullDate = settings.showFullDate,
                style = DateStyle.MEDIUM,
                compact = settings.compact,
            )
        } else {
            null
        }
        return RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_header).apply {
            setTextViewText(R.id.header, header)
            setImageViewResource(
                R.id.arrow, if (section.collapsed) {
                    R.drawable.ic_keyboard_arrow_down_black_18dp
                } else {
                    R.drawable.ic_keyboard_arrow_up_black_18dp
                }
            )
            setColorFilter(R.id.arrow, onSurfaceVariant)
            setTextColor(
                R.id.header,
                section.headerColor(
                    context,
                    settings.groupMode,
                    if (settings.isDark) R.color.white_60 else R.color.black_60
                )
            )
            if (!settings.showDividers) {
                setViewVisibility(R.id.divider, View.GONE)
            }
            setOnClickFillInIntent(
                R.id.row,
                Intent(WidgetClickActivity.TOGGLE_GROUP)
                    .putExtra(WidgetClickActivity.EXTRA_WIDGET, widgetId)
                    .putExtra(WidgetClickActivity.EXTRA_GROUP, sortGroup)
                    .putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !section.collapsed)
            )
        }
    }

    private fun buildUpdate(taskContainer: TaskContainer): RemoteViews? {
        return try {
            val task = taskContainer.task
            val textColorTitle = when {
                task.isHidden -> onSurfaceVariant
                task.isCompleted -> onSurfaceVariant
                !settings.showDueDates && task.isOverdue -> context.getColor(R.color.overdue)
                else -> onSurface
            }
            newRemoteView().apply {
                strikethrough(R.id.widget_text, task.isCompleted)
                setTextSize(R.id.widget_text, settings.textSize)
                if (settings.showDueDates) {
                    formatDueDate(this, taskContainer)
                } else {
                    setViewVisibility(R.id.widget_due_bottom, View.GONE)
                    setViewVisibility(R.id.widget_due_end, View.GONE)
                }
                if (settings.showFullTaskTitle) {
                    setMaxLines(R.id.widget_text, Int.MAX_VALUE)
                }
                setTextViewText(
                    R.id.widget_text,
                    markdown.toMarkdown(task.title)
                )
                setTextColor(R.id.widget_text, textColorTitle)
                if (settings.showDescription && task.hasNotes()) {
                    setTextSize(R.id.widget_description, settings.textSize)
                    setTextColor(R.id.widget_description, onSurfaceVariant)
                    setTextViewText(
                        R.id.widget_description,
                        markdown.toMarkdown(task.notes)
                    )
                    setViewVisibility(R.id.widget_description, View.VISIBLE)
                    if (settings.showFullDescription) {
                        setMaxLines(R.id.widget_description, Int.MAX_VALUE)
                    }
                } else {
                    setViewVisibility(R.id.widget_description, View.GONE)
                }
                setOnClickFillInIntent(
                    R.id.widget_row,
                    Intent(WidgetClickActivity.EDIT_TASK)
                        .putExtra(WidgetClickActivity.EXTRA_FILTER, filter)
                        .putExtra(WidgetClickActivity.EXTRA_TASK_ID, task.id)
                )
                if (settings.showCheckboxes) {
                    setViewPadding(
                        R.id.widget_complete_box,
                        hPad,
                        settings.vPad,
                        hPad,
                        settings.vPad
                    )
                    setImageViewResource(R.id.widget_complete_box, task.getCheckboxRes())
                    setColorFilter(R.id.widget_complete_box, priorityColor(task.priority))
                    setOnClickFillInIntent(
                        R.id.widget_complete_box,
                        Intent(WidgetClickActivity.COMPLETE_TASK)
                            .putExtra(WidgetClickActivity.EXTRA_TASK_ID, task.id)
                            .putExtra(WidgetClickActivity.EXTRA_COMPLETED, !task.isCompleted)
                    )
                } else {
                    setViewPadding(R.id.widget_complete_box, hPad, 0, 0, 0)
                    setBackgroundResource(R.id.widget_complete_box, 0)
                }
                setViewPadding(R.id.top_padding, 0, settings.vPad, 0, 0)
                setViewPadding(R.id.bottom_padding, 0, settings.vPad, 0, 0)
                if (!settings.showDividers) {
                    setViewVisibility(R.id.divider, View.GONE)
                }
                removeAllViews(R.id.chips)
                if (settings.showSubtaskChips && taskContainer.hasChildren()) {
                    val chip = chipProvider.getSubtaskChip(taskContainer)
                    addView(R.id.chips, chip)
                    setOnClickFillInIntent(
                        R.id.chip,
                        Intent(WidgetClickActivity.TOGGLE_SUBTASKS)
                            .putExtra(WidgetClickActivity.EXTRA_TASK_ID, task.id)
                            .putExtra(
                                WidgetClickActivity.EXTRA_COLLAPSED,
                                !taskContainer.isCollapsed
                            )
                    )
                }
                if (taskContainer.task.isHidden && settings.showStartChips) {
                    val sortByDate = settings.groupMode == SortHelper.SORT_START && !disableGroups
                    chipProvider
                        .getStartDateChip(taskContainer, settings.showFullDate, sortByDate)
                        ?.let { addView(R.id.chips, it) }
                }
                if (taskContainer.hasLocation() && settings.showPlaceChips) {
                    chipProvider
                        .getPlaceChip(filter, taskContainer)
                        ?.let { addView(R.id.chips, it) }
                }
                if (!taskContainer.hasParent() && settings.showListChips) {
                    chipProvider
                        .getListChip(filter, taskContainer)
                        ?.let { addView(R.id.chips, it) }
                }
                if (settings.showTagChips && taskContainer.tagsString?.isNotBlank() == true) {
                    chipProvider
                        .getTagChips(filter, taskContainer)
                        .forEach { addView(R.id.chips, it) }
                }
                val startPad = taskContainer.indent * indentPadding
                setViewPadding(R.id.widget_row, startPad, 0, 0, 0)
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private suspend fun getQuery(filter: Filter): String {
        subtasksHelper.applySubtasksToWidgetFilter(filter, widgetPreferences)
        return getQuery(widgetPreferences, filter)
    }

    private fun formatDueDate(row: RemoteViews, task: TaskContainer) = with(row) {
        val dueDateRes = if (settings.endDueDate) R.id.widget_due_end else R.id.widget_due_bottom
        setViewVisibility(
            if (settings.endDueDate) R.id.widget_due_bottom else R.id.widget_due_end,
            View.GONE
        )
        val hasDueDate = task.hasDueDate()
        val endPad = if (hasDueDate && settings.endDueDate) 0 else hPad
        setViewPadding(R.id.widget_text, 0, 0, endPad, 0)
        if (hasDueDate) {
            if (settings.endDueDate) {
                setViewPadding(R.id.widget_due_end, hPad, settings.vPad, hPad, settings.vPad)
            }
            setViewVisibility(dueDateRes, View.VISIBLE)
            val text = if (
                settings.groupMode == SortHelper.SORT_DUE &&
                (task.sortGroup ?: 0L) >= currentTimeMillis().startOfDay() &&
                !disableGroups
            ) {
                task.takeIf { it.hasDueTime() }?.let {
                    getTimeString(task.dueDate, context.is24HourFormat)
                }
            } else {
                runBlocking {
                    getRelativeDateTime(
                        task.dueDate,
                        context.is24HourFormat,
                        alwaysDisplayFullDate = settings.showFullDate
                    )
                }
            }
            setTextViewText(dueDateRes, text)
            setTextColor(
                dueDateRes,
                if (task.task.isOverdue) context.getColor(R.color.overdue) else onSurfaceVariant
            )
            setTextSize(dueDateRes, max(10f, settings.textSize - 2))
            setOnClickFillInIntent(
                dueDateRes,
                Intent(WidgetClickActivity.RESCHEDULE_TASK)
                    .putExtra(WidgetClickActivity.EXTRA_TASK_ID, task.id)
            )
        } else {
            setViewVisibility(dueDateRes, View.GONE)
        }
    }
}
