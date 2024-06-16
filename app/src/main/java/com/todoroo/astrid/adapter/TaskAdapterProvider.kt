package com.todoroo.astrid.adapter

import android.content.Context
import org.tasks.filters.CaldavFilter
import org.tasks.filters.GtasksFilter
import org.tasks.filters.TagFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater
import com.todoroo.astrid.subtasks.SubtasksHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.Task.Companion.isUuidEmpty
import org.tasks.data.entity.TaskListMetadata
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.preferences.Preferences
import javax.inject.Inject

class TaskAdapterProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val taskListMetadataDao: TaskListMetadataDao,
    private val taskDao: TaskDao,
    private val googleTaskDao: GoogleTaskDao,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskMover: TaskMover,
) {
    fun createTaskAdapter(filter: Filter): TaskAdapter {
        if (filter is AstridOrderingFilter && preferences.isAstridSort) {
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
                is GtasksFilter -> return GoogleTaskManualSortAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
                is CaldavFilter -> return CaldavManualSortTaskAdapter(googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
            }
        }
        return TaskAdapter(preferences.addTasksToTop(), googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
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
        AstridTaskAdapter(list!!, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
    }

    private fun createManualFilterTaskAdapter(filter: AstridOrderingFilter): TaskAdapter? = runBlocking {
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
        AstridTaskAdapter(list, filter, updater, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover)
    }
}