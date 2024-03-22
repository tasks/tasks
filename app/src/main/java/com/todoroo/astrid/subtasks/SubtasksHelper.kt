package com.todoroo.astrid.subtasks

import android.content.Context
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.BuiltInFilterExposer.Companion.isInbox
import com.todoroo.astrid.core.BuiltInFilterExposer.Companion.isTodayFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task.Companion.isValidUuid
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildOrderString
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildTreeModel
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.serializeTree
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.data.TaskListMetadata
import org.tasks.data.TaskListMetadataDao
import org.tasks.db.QueryUtils.showHiddenAndCompleted
import org.tasks.preferences.QueryPreferences
import timber.log.Timber
import javax.inject.Inject

class SubtasksHelper @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val taskDao: TaskDao,
        private val tagDataDao: TagDataDao,
        private val taskListMetadataDao: TaskListMetadataDao) {

    suspend fun applySubtasksToWidgetFilter(
            filter: Filter,
            preferences: QueryPreferences,
    ) {
        if (filter is AstridOrderingFilter && preferences.isAstridSort) {
            var query = filter.sql!!
            val tagData = tagDataDao.getTagByName(filter.title!!)
            val tlm = when {
                tagData != null ->
                    taskListMetadataDao.fetchByTagOrFilter(tagData.remoteId!!)
                isInbox(context, filter) ->
                    taskListMetadataDao.fetchByTagOrFilter(TaskListMetadata.FILTER_ID_ALL)
                isTodayFilter(context, filter) ->
                    taskListMetadataDao.fetchByTagOrFilter(TaskListMetadata.FILTER_ID_TODAY)
                else -> null
            }
            if (tlm != null) {
                query = query.replace("ORDER BY .*".toRegex(), "")
                query += " ORDER BY ${getOrderString(tagData, tlm)}"
                query = showHiddenAndCompleted(query)
                filter.filterOverride = query
            }
        }
    }

    private suspend fun getOrderString(tagData: TagData?, tlm: TaskListMetadata?): String {
        val serialized: String? = when {
            tlm != null -> tlm.taskIds
            tagData != null -> convertTreeToRemoteIds(taskDao, tagData.tagOrdering)
            else -> "[]"
        }
        return buildOrderString(getStringIdArray(serialized))
    }

    companion object {
        @Deprecated("")
        private fun getIdList(serializedTree: String?): List<Long> {
            val ids = ArrayList<Long>()
            val digitsOnly = serializedTree!!.split("[\\[\\],\\s]".toRegex()).toTypedArray() // Split on [ ] , or whitespace chars
            for (idString in digitsOnly) {
                try {
                    if (!isNullOrEmpty(idString)) {
                        ids.add(idString.toLong())
                    }
                } catch (e: NumberFormatException) {
                    Timber.e(e)
                }
            }
            return ids
        }

        fun getStringIdArray(serializedTree: String?): List<String> {
            val ids = ArrayList<String>()
            val values = serializedTree!!.split("""[\[\],"\s]""".toRegex()).toTypedArray() // Split on [ ] , or whitespace chars
            for (idString in values) {
                if (!isNullOrEmpty(idString)) {
                    ids.add(idString)
                }
            }
            return ids
        }

        /** Takes a subtasks string containing local ids and remaps it to one containing UUIDs  */
        suspend fun convertTreeToRemoteIds(taskDao: TaskDao, localTree: String?): String {
            val localIds = getIdList(localTree)
            val idMap = getIdMap(taskDao, localIds)
            idMap[-1L] = "-1" // $NON-NLS-1$
            val tree = buildTreeModel(localTree, null)
            remapLocalTreeToRemote(tree, idMap)
            return serializeTree(tree)
        }

        private fun remapTree(root: SubtasksFilterUpdater.Node, idMap: Map<Long, String>, helper: (String) -> Long) {
            val children = root.children
            var i = 0
            while (i < children.size) {
                val child = children[i]
                val key = helper(child.uuid)
                val uuid = idMap[key]
                if (!isValidUuid(uuid!!)) {
                    children.removeAt(i)
                    children.addAll(i, child.children)
                    i--
                } else {
                    child.uuid = uuid
                    remapTree(child, idMap, helper)
                }
                i++
            }
        }

        private fun remapLocalTreeToRemote(root: SubtasksFilterUpdater.Node, idMap: Map<Long, String>) {
            remapTree(root, idMap) { uuid: String ->
                var localId = -1L
                try {
                    localId = uuid.toLong()
                } catch (e: NumberFormatException) {
                    Timber.e(e)
                }
                localId
            }
        }

        private suspend fun getIdMap(taskDao: TaskDao, keys: List<Long>): MutableMap<Long, String> {
            val tasks = taskDao.fetch(keys)
            val map: MutableMap<Long, String> = HashMap()
            for (task in tasks) {
                map[task.id] = task.uuid
            }
            return map
        }
    }
}