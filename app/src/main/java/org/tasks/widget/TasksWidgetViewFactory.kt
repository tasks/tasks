package org.tasks.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.subtasks.SubtasksHelper
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.date.DateTimeUtils
import org.tasks.extensions.setBackgroundResource
import org.tasks.extensions.setColorFilter
import org.tasks.extensions.setMaxLines
import org.tasks.extensions.setTextSize
import org.tasks.extensions.strikethrough
import org.tasks.markdown.Markdown
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.themes.ColorProvider.Companion.priorityColor
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.CheckBoxProvider.Companion.getCheckboxRes
import timber.log.Timber
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

internal class TasksWidgetViewFactory(
    private val subtasksHelper: SubtasksHelper,
    private val widgetPreferences: WidgetPreferences,
    private val filter: Filter,
    private val context: Context,
    private val widgetId: Int,
    private val taskDao: TaskDao,
    private val locale: Locale,
    private val chipProvider: WidgetChipProvider,
    private val markdown: Markdown,
    private val headerFormatter: HeaderFormatter,
) : RemoteViewsFactory {
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
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            val collapsed = widgetPreferences.collapsed
            tasks = SectionedDataSource(
                taskDao.fetchTasks { getQuery(filter) },
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

    override fun onDestroy() {}

    override fun getCount() = tasks.size

    override fun getViewAt(position: Int): RemoteViews? =
        if (tasks.isHeader(position)) buildHeader(position) else buildUpdate(position)

    override fun getLoadingView(): RemoteViews = newRemoteView()

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int) = getTask(position).id

    override fun hasStableIds(): Boolean = true

    private fun newRemoteView() = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row)

    private fun buildHeader(position: Int): RemoteViews {
        val section = tasks.getSection(position)
        val sortGroup = section.value
        val header: String? = if (filter.supportsSorting()) {
            headerFormatter.headerStringBlocking(
                value = section.value,
                groupMode = settings.groupMode,
                alwaysDisplayFullDate = settings.showFullDate,
                style = FormatStyle.MEDIUM,
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

    private fun buildUpdate(position: Int): RemoteViews? {
        return try {
            val taskContainer = getTask(position)
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
                        .putExtra(WidgetClickActivity.EXTRA_TASK, task)
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
                            .putExtra(WidgetClickActivity.EXTRA_TASK, task)
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
                            .putExtra(WidgetClickActivity.EXTRA_TASK, task)
                            .putExtra(
                                WidgetClickActivity.EXTRA_COLLAPSED,
                                !taskContainer.isCollapsed
                            )
                    )
                }
                if (taskContainer.isHidden && settings.showStartChips) {
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

    private fun getTask(position: Int): TaskContainer = tasks.getItem(position)

    private suspend fun getQuery(filter: Filter): List<String> {
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
                (task.sortGroup ?: 0L) >= now().startOfDay() &&
                !disableGroups
            ) {
                task.takeIf { it.hasDueTime() }?.let {
                    DateUtilities.getTimeString(context, DateTimeUtils.newDateTime(task.dueDate))
                }
            } else {
                DateUtilities.getRelativeDateTime(
                    context, task.dueDate, locale, FormatStyle.MEDIUM, settings.showFullDate, false
                )
            }
            setTextViewText(dueDateRes, text)
            setTextColor(
                dueDateRes,
                if (task.isOverdue) context.getColor(R.color.overdue) else onSurfaceVariant
            )
            setTextSize(dueDateRes, max(10f, settings.textSize - 2))
            setOnClickFillInIntent(
                dueDateRes,
                Intent(WidgetClickActivity.RESCHEDULE_TASK)
                    .putExtra(WidgetClickActivity.EXTRA_TASK, task.task)
            )
        } else {
            setViewVisibility(dueDateRes, View.GONE)
        }
    }
}

