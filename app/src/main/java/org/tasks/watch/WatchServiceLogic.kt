package org.tasks.watch

import android.content.Context
import android.text.format.DateFormat
import co.touchlab.kermit.Logger
import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.service.TaskCreator
import org.tasks.analytics.Analytics
import org.tasks.billing.PurchaseState
import org.tasks.data.NO_COUNT
import org.tasks.data.TaskSaver
import org.tasks.data.count
import org.tasks.data.countCompletedSql
import org.tasks.data.countSql
import org.tasks.data.dao.TaskDao
import org.tasks.data.fetchTasks
import org.tasks.data.isHidden
import org.tasks.db.QueryUtils
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.getIcon
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.QueryPreferences
import org.tasks.service.TaskCompleter
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.UiItem
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

class WatchServiceLogic(
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val appPreferences: QueryPreferences,
    private val taskCompleter: TaskCompleter,
    private val headerFormatter: HeaderFormatter,
    private val analytics: Analytics,
    private val filterProvider: FilterProvider,
    private val purchaseState: PurchaseState,
    private val colorProvider: ColorProvider,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val taskCreator: TaskCreator,
    private val context: Context,
) : WatchService {
    private val is24HourTime: Boolean
        get() = DateFormat.is24HourFormat(context)

    suspend fun resolveFilter(filterPreference: String?): Filter =
        defaultFilterProvider.getFilterFromPreference(
            filterPreference?.takeIf { it.isNotBlank() }
        )

    override suspend fun getTasks(
        filterPreference: String?,
        position: Int,
        limit: Int,
        showHidden: Boolean,
        showCompleted: Boolean,
        sortMode: Int?,
        groupMode: Int?,
        collapsed: Set<Long>,
    ): WatchTaskList {
        val effectiveLimit = if (limit > 0) limit else Int.MAX_VALUE
        val filter = resolveFilter(filterPreference)
        val preferences = WatchQueryPreferences(
            delegate = appPreferences,
            overrideShowHidden = showHidden,
            overrideShowCompleted = showCompleted,
            overrideSortMode = sortMode,
            overrideGroupMode = groupMode,
        )
        val payload = SectionedDataSource(
            tasks = taskDao.fetchTasks(preferences, filter),
            disableHeaders = filter.disableHeaders()
                    || (filter.supportsManualSort() && preferences.isManualSort)
                    || (filter is AstridOrderingFilter && preferences.isAstridSort),
            groupMode = preferences.groupMode,
            subtaskMode = preferences.subtaskMode,
            completedAtBottom = preferences.completedTasksAtBottom,
            collapsed = collapsed,
        )
        return WatchTaskList(
            totalItems = payload.size,
            items = payload
                .subList(position, (position + effectiveLimit).coerceAtMost(payload.size))
                .map { item ->
                    when (item) {
                        is UiItem.Header ->
                            WatchUiItem.Header(
                                id = item.value,
                                title = headerFormatter.headerString(
                                    item.value,
                                    groupMode = preferences.groupMode,
                                    style = DateStyle.MEDIUM,
                                ),
                                collapsed = item.collapsed,
                            )

                        is UiItem.Task -> {
                            val timestamp = if (preferences.groupMode == SORT_DUE &&
                                (item.task.sortGroup ?: 0) >= currentTimeMillis().startOfDay()
                            ) {
                                item.task.takeIf { it.hasDueTime() }?.let {
                                    getTimeString(item.task.dueDate, is24HourTime)
                                }
                            } else if (item.task.hasDueDate()) {
                                getRelativeDateTime(
                                    item.task.dueDate,
                                    is24HourTime,
                                )
                            } else {
                                null
                            }
                            WatchUiItem.Task(
                                id = item.task.id,
                                title = item.task.title,
                                priority = item.task.priority,
                                completed = item.task.isCompleted,
                                hidden = item.task.task.isHidden,
                                indent = item.task.indent,
                                collapsed = item.task.isCollapsed,
                                numSubtasks = item.task.children,
                                timestamp = timestamp,
                                repeating = item.task.task.isRecurring,
                            )
                        }
                    }
                }
        )
    }

    override suspend fun completeTask(taskId: Long, completed: Boolean, source: String) {
        taskCompleter.setComplete(taskId, completed)
        analytics.completeTask(source)
    }

    suspend fun toggleGroup(value: Long, collapsed: Boolean) {
        taskSaver.setCollapsed(value, collapsed)
    }

    override suspend fun getLists(position: Int, limit: Int): WatchListItems {
        val effectiveLimit = if (limit > 0) limit else Int.MAX_VALUE
        val filters = filterProvider.wearableFilters()
        return WatchListItems(
            totalItems = filters.size,
            items = filters
                .subList(position, (position + effectiveLimit).coerceAtMost(filters.size))
                .map { item ->
                    when (item) {
                        is Filter -> {
                            val (bgColor, fgColor) = getColors(item)
                            WatchListItem.FilterItem(
                                id = defaultFilterProvider.getFilterPreferenceValue(item) ?: "",
                                title = item.title,
                                icon = item.getIcon(purchaseState),
                                color = bgColor,
                                textColor = fgColor,
                                taskCount = item.count.takeIf { it != NO_COUNT } ?: try {
                                    taskDao.count(item)
                                } catch (e: Exception) {
                                    Logger.e(e) { "Failed to count tasks" }
                                    0
                                },
                            )
                        }

                        is NavigationDrawerSubheader ->
                            WatchListItem.Header(
                                id = "${item.subheaderType}_${item.id}",
                                title = item.title ?: "",
                            )

                        else -> throw IllegalArgumentException()
                    }
                }
        )
    }

    override suspend fun getTask(taskId: Long): WatchTaskDetail {
        val task = taskDao.fetch(taskId)
            ?: throw IllegalArgumentException("Task $taskId not found")
        return WatchTaskDetail(
            title = task.title ?: "",
            completed = task.isCompleted,
            priority = task.priority,
            repeating = task.isRecurring,
            description = task.notes ?: "",
        )
    }

    override suspend fun saveTask(
        taskId: Long,
        title: String,
        completed: Boolean,
        filterPreference: String?,
        source: String,
    ): Long {
        if (taskId == 0L) {
            val filter = resolveFilter(filterPreference)
            val task = taskCreator.basicQuickAddTask(
                title = title,
                filter = filter,
            )
            analytics.addTask(source)
            return task.id
        } else {
            taskDao.fetch(taskId)?.let { task ->
                taskSaver.save(
                    task.copy(
                        title = title,
                        completionDate = when {
                            !completed -> 0
                            task.isCompleted -> task.completionDate
                            else -> System.currentTimeMillis()
                        },
                    )
                )
            }
            return taskId
        }
    }

    override suspend fun getTaskCount(
        filterPreference: String?,
        showHidden: Boolean,
        showCompleted: Boolean,
    ): WatchTaskCount {
        val filter = resolveFilter(filterPreference)
        var sql = filter.sql ?: return WatchTaskCount(0, 0)
        if (showHidden) {
            sql = QueryUtils.showHidden(sql)
        }
        val count = taskDao.countSql(sql)
        val completedCount = if (showCompleted) {
            taskDao.countCompletedSql(sql)
        } else {
            0
        }
        return WatchTaskCount(count, completedCount)
    }

    private fun getColors(filter: Filter): Pair<Int, Int> {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || purchaseState.purchasedThemes()) {
                return color.primaryColor to color.colorOnPrimary
            }
        }
        return 0 to 0
    }
}
