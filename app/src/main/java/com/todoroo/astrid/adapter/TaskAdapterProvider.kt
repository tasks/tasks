package com.todoroo.astrid.adapter

import android.content.Context
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task.Companion.isUuidEmpty
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater
import com.todoroo.astrid.subtasks.SubtasksHelper
import org.tasks.LocalBroadcastManager
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskListMetadata
import org.tasks.data.TaskListMetadataDao
import org.tasks.injection.ApplicationContext
import org.tasks.preferences.Preferences
import javax.inject.Inject

class TaskAdapterProvider @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val taskListMetadataDao: TaskListMetadataDao,
        private val taskDao: TaskDao,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val localBroadcastManager: LocalBroadcastManager) {
    fun createTaskAdapter(filter: Filter): TaskAdapter {
        if (filter.supportsAstridSorting() && preferences.isAstridSort) {
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
        if (filter.supportsManualSort() && preferences.isManualSort) {
            when (filter) {
                is GtasksFilter -> return GoogleTaskManualSortAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager)
                is CaldavFilter -> return CaldavManualSortTaskAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager)
            }
        }
        return TaskAdapter(preferences.addTasksToTop(), googleTaskDao, caldavDao, taskDao, localBroadcastManager)
    }

    private fun createManualTagTaskAdapter(filter: TagFilter): TaskAdapter {
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
        return AstridTaskAdapter(list!!, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager)
    }

    private fun createManualFilterTaskAdapter(filter: Filter): TaskAdapter? {
        var filterId: String? = null
        var prefId: String? = null
        if (BuiltInFilterExposer.isInbox(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_ALL
            prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER
        } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
            filterId = TaskListMetadata.FILTER_ID_TODAY
            prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER
        }
        if (filterId.isNullOrBlank()) {
            return null
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
        return AstridTaskAdapter(list, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager)
    }
}