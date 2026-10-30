package org.tasks.data

import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task

/**
 * One row of the "about to be deleted" preview: the task plus whether it will
 * actually be deleted. Read-only tasks (e.g. synced from a shared calendar)
 * are filtered out of the deletion by [org.tasks.service.TaskDeleter], so they
 * are surfaced as `readOnly = true` — shown greyed, not deleted.
 */
data class DeletePreviewEntry(
    val task: Task,
    val readOnly: Boolean,
    val depth: Int = 0,
)

/**
 * Computes the exact set of tasks a delete of [taskId] will remove, in
 * display order (the task first, then its descendants breadth-first).
 *
 * Deliberately mirrors [org.tasks.service.TaskDeleter.markDeleted]: the same
 * recursive `getChildren` CTE and the same read-only exclusion, so the preview
 * can never disagree with the delete. Already-deleted descendants are skipped
 * (the CTE filters `deleted = 0`, as does the deletion).
 */
object DeletePreview {

    suspend fun forTask(taskDao: TaskDao, taskId: Long): List<DeletePreviewEntry> {
        val ids = (listOf(taskId) + taskDao.getChildren(listOf(taskId))).distinct()
        val tasks = taskDao.fetch(ids).associateBy { it.id }
        val queue = ArrayDeque(listOf(taskId to 0))
        val result = mutableListOf<DeletePreviewEntry>()
        while (queue.isNotEmpty()) {
            val (id, depth) = queue.removeFirst()
            val task = tasks[id] ?: continue
            result += DeletePreviewEntry(task, task.readOnly, depth)
            queue += ids.filter { tasks[it]?.parent == id }.map { it to depth + 1 }
        }
        return result
    }
}
