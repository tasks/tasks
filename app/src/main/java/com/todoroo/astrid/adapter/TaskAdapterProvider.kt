package com.todoroo.astrid.adapter

import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater
import com.todoroo.astrid.subtasks.SubtasksHelper
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.Task.Companion.isUuidEmpty
import org.tasks.data.entity.TaskListMetadata
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TodayFilter
import org.tasks.filters.key
import org.tasks.preferences.FilterPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences
import org.tasks.preferences.TasksPreferences
import javax.inject.Inject

class TaskAdapterProvider @Inject constructor(
    private val preferences: Preferences,
    private val tasksPreferences: TasksPreferences,
    private val taskListMetadataDao: TaskListMetadataDao,
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val googleTaskDao: GoogleTaskDao,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskMover: TaskMover,
) {
    fun createTaskAdapter(filter: Filter): TaskAdapter {
        val queryPreferences: QueryPreferences = if (preferences.isPerListSortEnabled) {
            FilterPreferences(preferences, tasksPreferences, filter.key())
        } else {
            preferences
        }
        if (filter is AstridOrderingFilter && queryPreferences.isAstridSort) {
            when (filter) {
                is TagFilter -> return createManualTagTaskAdapter(filter)
                else -> {
                    val adapter = createManualFilterTaskAdapter(filter)
                    if (adapter != null) {
                        return adapter
                    }
                }
            }
        }
        if (filter.supportsManualSort() && queryPreferences.isManualSort) {
            if (filter is CaldavFilter) {
                when {
                    filter.isGoogleTasks -> return GoogleTaskManualSortAdapter(googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover)
                    filter.isIcalendar -> return CaldavManualSortTaskAdapter(googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover)
                }
            }
        }
        return TaskAdapter(preferences.addTasksToTop(), googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover)
    }

    private fun createManualTagTaskAdapter(filter: TagFilter): TaskAdapter = runBlocking {
        val tagData = filter.tagData
        val tdId = tagData.remoteId
        var list = taskListMetadataDao.fetchByTagOrFilter(tagData.remoteId!!)
        if (list == null && !isUuidEmpty(tdId)) {
            list = TaskListMetadata()
            list.tagUuid = tdId
            taskListMetadataDao.createNew(list)
        }
        val updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
        updater.initialize(list, filter)
        AstridTaskAdapter(list!!, filter, updater, googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover)
    }

    private fun createManualFilterTaskAdapter(filter: AstridOrderingFilter): TaskAdapter? = runBlocking {
        var filterId: String? = null
        var prefId: String? = null
        if (filter is MyTasksFilter) {
            filterId = TaskListMetadata.FILTER_ID_ALL
            prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER
        } else if (filter is TodayFilter) {
            filterId = TaskListMetadata.FILTER_ID_TODAY
            prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER
        }
        if (filterId.isNullOrBlank()) {
            return@runBlocking null
        }
        var list = taskListMetadataDao.fetchByTagOrFilter(filterId)
        if (list == null) {
            var defaultOrder = preferences.getStringValue(prefId)
            if (isNullOrEmpty(defaultOrder)) {
                defaultOrder = "[]" // $NON-NLS-1$
            }
            defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskDao, defaultOrder)
            list = TaskListMetadata()
            list.filter = filterId
            list.taskIds = defaultOrder
            taskListMetadataDao.createNew(list)
        }
        val updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
        updater.initialize(list, filter)
        AstridTaskAdapter(list, filter, updater, googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover)
    }
}