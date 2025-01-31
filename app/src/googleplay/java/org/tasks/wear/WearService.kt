package org.tasks.wear

import androidx.datastore.core.DataStore
import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import kotlinx.coroutines.flow.firstOrNull
import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.CompleteTaskResponse
import org.tasks.GrpcProto.GetListsResponse
import org.tasks.GrpcProto.GetTaskResponse
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.ListItem
import org.tasks.GrpcProto.ListItemType
import org.tasks.GrpcProto.SaveTaskResponse
import org.tasks.GrpcProto.Tasks
import org.tasks.GrpcProto.ToggleGroupRequest
import org.tasks.GrpcProto.ToggleGroupResponse
import org.tasks.WearServiceGrpcKt
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.copy
import org.tasks.data.NO_COUNT
import org.tasks.data.isHidden
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.getIcon
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.UiItem
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import timber.log.Timber

class WearService(
    private val taskDao: TaskDao,
    private val appPreferences: Preferences,
    private val taskCompleter: TaskCompleter,
    private val headerFormatter: HeaderFormatter,
    private val settings: DataStore<GrpcProto.Settings>,
    private val firebase: Firebase,
    private val filterProvider: FilterProvider,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val taskCreator: TaskCreator,
    private val is24HourTime: Boolean,
) : WearServiceGrpcKt.WearServiceCoroutineImplBase() {
    override suspend fun getTasks(request: GetTasksRequest): Tasks {
        val position = request.position
        val limit = request.limit.takeIf { it > 0 } ?: Int.MAX_VALUE
        val settingsData = settings.data.firstOrNull() ?: GrpcProto.Settings.getDefaultInstance()
        val filter =
            defaultFilterProvider.getFilterFromPreference(settingsData.filter.takeIf { it.isNotBlank() })
        val preferences = WearPreferences(appPreferences, settingsData)
        val collapsed = settingsData?.collapsedList?.toSet() ?: emptySet()
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
        return Tasks.newBuilder()
            .setTotalItems(payload.size)
            .addAllItems(
                payload
                    .subList(position, position + limit)
                    .map { item ->
                        when (item) {
                            is UiItem.Header ->
                                GrpcProto.UiItem.newBuilder()
                                    .setId(item.value)
                                    .setType(ListItemType.Header)
                                    .setTitle(headerFormatter.headerString(item.value, style = DateStyle.MEDIUM))
                                    .setCollapsed(item.collapsed)
                                    .build()

                            is UiItem.Task -> {
                                val timestamp = if (preferences.groupMode == SORT_DUE &&
                                    (item.task.sortGroup
                                        ?: 0) >= currentTimeMillis().startOfDay()
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

                                GrpcProto.UiItem.newBuilder()
                                    .setType(ListItemType.Item)
                                    .setId(item.task.id)
                                    .setPriority(item.task.priority)
                                    .setCompleted(item.task.isCompleted)
                                    .setHidden(item.task.task.isHidden)
                                    .setIndent(item.task.indent)
                                    .setCollapsed(item.task.isCollapsed)
                                    .setNumSubtasks(item.task.children)
                                    .apply {
                                        if (item.task.title != null) {
                                            setTitle(item.task.title)
                                        }
                                        if (timestamp != null) {
                                            setTimestamp(timestamp)
                                        }
                                    }
                                    .setRepeating(item.task.task.isRecurring)
                                    .build()
                            }
                        }
                    }
            )
            .build()
    }

    override suspend fun completeTask(request: CompleteTaskRequest): CompleteTaskResponse {
        taskCompleter.setComplete(request.id, request.completed)
        firebase.completeTask("wearable")
        return CompleteTaskResponse.newBuilder().setSuccess(true).build()
    }

    override suspend fun toggleGroup(request: ToggleGroupRequest): ToggleGroupResponse {
        settings.updateData {
            it.copy {
                if (request.collapsed) {
                    if (!collapsed.contains(request.value)) {
                        collapsed.add(request.value)
                    }
                } else {
                    if (collapsed.contains(request.value)) {
                        collapsed.clear()
                        collapsed.addAll(
                            it.collapsedList.toMutableList().apply { remove(request.value) })
                    }
                }
            }
        }

        return ToggleGroupResponse.getDefaultInstance()
    }

    override suspend fun updateSettings(request: GrpcProto.UpdateSettingsRequest): GrpcProto.Settings {
        return settings.updateData { request.settings }
    }

    override suspend fun toggleSubtasks(request: ToggleGroupRequest): ToggleGroupResponse {
        taskDao.setCollapsed(request.value, request.collapsed)
        return ToggleGroupResponse.newBuilder().build()
    }

    override suspend fun getLists(request: GrpcProto.GetListsRequest): GetListsResponse {
        val position = request.position
        val limit = request.limit.takeIf { it > 0 } ?: Int.MAX_VALUE
        val selected = settings.data.firstOrNull()?.filter?.takeIf { it.isNotBlank() }
            ?: defaultFilterProvider.getFilterPreferenceValue(MyTasksFilter.create())
        val filters = filterProvider.wearableFilters()
        return GetListsResponse.newBuilder()
            .setTotalItems(filters.size)
            .addAllItems(
                filters
                    .subList(position, (position + limit).coerceAtMost(filters.size))
                    .map { item ->
                        when (item) {
                            is Filter -> {
                                ListItem.newBuilder()
                                    .setId(defaultFilterProvider.getFilterPreferenceValue(item))
                                    .setType(ListItemType.Item)
                                    .setTitle(item.title ?: "")
                                    .setIcon(item.getIcon(inventory))
                                    .setColor(getColor(item))
                                    .setTaskCount(item.count.takeIf { it != NO_COUNT } ?: try {
                                        taskDao.count(item)
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                        0
                                    })
                                    .build()
                            }

                            is NavigationDrawerSubheader ->
                                ListItem.newBuilder()
                                    .setType(ListItemType.Header)
                                    .setTitle(item.title ?: "")
                                    .setId("${item.subheaderType}_${item.id}")
                                    .build()

                            else -> throw IllegalArgumentException()
                        }
                    }
            )
            .build()
    }

    override suspend fun getTask(request: GrpcProto.GetTaskRequest): GetTaskResponse {
        Timber.d("getTask($request)")
        val task = taskDao.fetch(request.taskId)
            ?: throw IllegalArgumentException()
        return GetTaskResponse.newBuilder()
            .setTitle(task.title ?: "")
            .setCompleted(task.isCompleted)
            .setPriority(task.priority)
            .setRepeating(task.isRecurring)
            .build()
    }

    override suspend fun saveTask(request: GrpcProto.SaveTaskRequest): SaveTaskResponse {
        Timber.d("saveTask($request)")
        if (request.taskId == 0L) {
            val filter = defaultFilterProvider.getFilterFromPreference(
                settings.data.firstOrNull()?.filter?.takeIf { it.isNotBlank() }
            )
            val task = taskCreator.basicQuickAddTask(
                title = request.title,
                filter = filter,
            )
            firebase.addTask("wearable")
            return SaveTaskResponse.newBuilder().setTaskId(task.id).build()
        } else {
            taskDao.fetch(request.taskId)?.let { task ->
                taskDao.save(
                    task.copy(
                        title = request.title,
                        completionDate = when {
                            !request.completed -> 0
                            task.isCompleted -> task.completionDate
                            else -> System.currentTimeMillis()
                        },
                    )
                )
            }
            return SaveTaskResponse.newBuilder().setTaskId(request.taskId).build()
        }
    }

    private fun getColor(filter: Filter): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return 0
    }
}
