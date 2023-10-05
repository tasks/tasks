package com.todoroo.astrid.adapter

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater
import org.tasks.LocalBroadcastManager
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListMetadata
import timber.log.Timber
import java.util.Collections
import kotlin.math.abs

@Deprecated("legacy astrid manual sorting")
class AstridTaskAdapter internal constructor(
    private val list: TaskListMetadata,
    private val filter: AstridOrderingFilter,
    private val updater: SubtasksFilterUpdater,
    googleTaskDao: GoogleTaskDao,
    caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    taskMover: TaskMover,
) : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover) {

    private val chainedCompletions = Collections.synchronizedMap(HashMap<String, ArrayList<String>>())

    override fun getIndent(task: TaskContainer) = updater.getIndentForTask(task.uuid)

    override fun canMove(source: TaskContainer, from: Int, target: TaskContainer, to: Int) = !updater.isDescendantOf(target.uuid, source.uuid)

    override fun maxIndent(previousPosition: Int, task: TaskContainer): Int {
        val previous = getTask(previousPosition)
        return updater.getIndentForTask(previous.uuid) + 1
    }

    override fun supportsAstridSorting() = true

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        val source = getTask(from)
        val targetTaskId = source.uuid
        try {
            if (to >= count) {
                updater.moveTo(list, filter, targetTaskId, "-1") // $NON-NLS-1$
            } else {
                val destinationTaskId = getItemUuid(to)
                updater.moveTo(list, filter, targetTaskId, destinationTaskId)
            }
            val currentIndent = updater.getIndentForTask(targetTaskId)
            val delta = indent - currentIndent
            for (i in 0 until abs(delta)) {
                updater.indent(list, filter, targetTaskId, delta)
            }
            localBroadcastManager.broadcastRefresh()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun onTaskCreated(uuid: String) = updater.onCreateTask(list, filter, uuid)

    override suspend fun onTaskDeleted(task: Task) = updater.onDeleteTask(list, filter, task.uuid)

    override suspend fun onCompletedTask(task: TaskContainer, newState: Boolean) {
        val itemId = task.uuid
        val completionDate = if (newState) DateUtilities.now() else 0
        if (!newState) {
            val chained = chainedCompletions[itemId]
            if (chained != null) {
                for (taskId in chained) {
                    taskDao.setCompletionDate(taskId, completionDate)
                }
            }
            return
        }
        val chained = ArrayList<String>()
        updater.applyToDescendants(itemId) { node: SubtasksFilterUpdater.Node ->
            val uuid = node.uuid
            taskDao.setCompletionDate(uuid, completionDate)
            chained.add(node.uuid)
        }
        if (chained.size > 0) {
            // move recurring items to item parent
            val tasks = taskDao.getRecurringTasks(chained)
            var madeChanges = false
            for (t in tasks) {
                if (!isNullOrEmpty(t.recurrence)) {
                    updater.moveToParentOf(t.uuid, itemId)
                    madeChanges = true
                }
            }
            if (madeChanges) {
                updater.writeSerialization(list, updater.serializeTree())
            }
            chainedCompletions[itemId] = chained
        }
    }

    override fun supportsHiddenTasks() = false
}