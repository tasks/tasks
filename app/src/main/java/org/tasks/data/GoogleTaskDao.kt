package org.tasks.data

import androidx.room.*
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.flow.Flow
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
abstract class GoogleTaskDao {
    @Insert
    abstract suspend fun insert(task: CaldavTask): Long

    @Insert
    abstract suspend fun insert(tasks: Iterable<CaldavTask>)

    @Transaction
    open suspend fun insertAndShift(task: CaldavTask, top: Boolean) {
        if (top) {
            task.order = 0
            shiftDown(task.calendar!!, task.parent, 0)
        } else {
            task.order = getBottom(task.calendar!!, task.parent)
        }
        task.id = insert(task)
    }

    @Query("UPDATE caldav_tasks SET cd_order = cd_order + 1 WHERE cd_calendar = :listId AND gt_parent = :parent AND cd_order >= :position")
    internal abstract suspend fun shiftDown(listId: String, parent: Long, position: Long)

    @Query("UPDATE caldav_tasks SET cd_order = cd_order - 1 WHERE cd_calendar = :listId AND gt_parent = :parent AND cd_order > :from AND cd_order <= :to")
    internal abstract suspend fun shiftUp(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE caldav_tasks SET cd_order = cd_order + 1 WHERE cd_calendar = :listId AND gt_parent = :parent AND cd_order < :from AND cd_order >= :to")
    internal abstract suspend fun shiftDown(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE caldav_tasks SET cd_order = cd_order - 1 WHERE cd_calendar = :listId AND gt_parent = :parent AND cd_order >= :position")
    internal abstract suspend fun shiftUp(listId: String, parent: Long, position: Long)

    @Transaction
    open suspend fun move(task: CaldavTask, newParent: Long, newPosition: Long) {
        val previousParent = task.parent
        val previousPosition = task.order!!
        if (newParent == previousParent) {
            if (previousPosition < newPosition) {
                shiftUp(task.calendar!!, newParent, previousPosition, newPosition)
            } else {
                shiftDown(task.calendar!!, newParent, previousPosition, newPosition)
            }
        } else {
            shiftUp(task.calendar!!, previousParent, previousPosition)
            shiftDown(task.calendar!!, newParent, newPosition)
        }
        task.parent = newParent
        task.order = newPosition
        update(task)
    }

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
    abstract suspend fun getByTaskId(taskId: Long): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
    abstract fun watchGoogleTask(taskId: Long): Flow<CaldavTask?>

    @Update
    abstract suspend fun update(googleTask: CaldavTask)

    @Query("UPDATE caldav_tasks SET cd_order = :order, gt_parent = :parent, gt_moved = 1 WHERE cd_id = :id")
    abstract suspend fun update(id: Long, parent: Long, order: Long)

    @Query("UPDATE caldav_tasks SET cd_deleted = :now WHERE cd_task = :task OR gt_parent = :task")
    abstract suspend fun markDeleted(task: Long, now: Long = currentTimeMillis())

    @Delete
    abstract suspend fun delete(deleted: CaldavTask)

    @Query("SELECT * FROM caldav_tasks WHERE cd_remote_id = :remoteId LIMIT 1")
    abstract suspend fun getByRemoteId(remoteId: String): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted > 0")
    abstract suspend fun getDeletedByTaskId(taskId: Long): List<CaldavTask>

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId")
    abstract suspend fun getAllByTaskId(taskId: Long): List<CaldavTask>

    @Query("SELECT DISTINCT cd_calendar FROM caldav_tasks WHERE cd_deleted = 0 AND cd_task IN (:tasks)")
    abstract suspend fun getLists(tasks: List<Long>): List<String>

    @Query("SELECT cd_task FROM caldav_tasks WHERE gt_parent IN (:ids) AND cd_deleted = 0")
    abstract suspend fun getChildren(ids: List<Long>): List<Long>

    suspend fun hasRecurringParent(ids: List<Long>): List<Long> =
            ids.chunkedMap { internalHasRecurringParent(it) }

    @Query("""
SELECT cd_task
FROM caldav_tasks
         INNER JOIN tasks ON gt_parent = _id
WHERE cd_task IN (:ids)
  AND cd_deleted = 0
  AND tasks.recurrence IS NOT NULL
  AND tasks.recurrence != ''
  AND tasks.completed = 0
        """)
    abstract suspend fun internalHasRecurringParent(ids: List<Long>): List<Long>

    @Query("SELECT tasks.* FROM tasks JOIN caldav_tasks ON tasks._id = cd_task WHERE gt_parent = :taskId")
    abstract suspend fun getChildTasks(taskId: Long): List<Task>

    @Query("SELECT tasks.* FROM tasks JOIN caldav_tasks ON tasks._id = gt_parent WHERE cd_task = :taskId")
    abstract suspend fun getParentTask(taskId: Long): Task?

    @Query("SELECT * FROM caldav_tasks WHERE gt_parent = :id AND cd_deleted = 0")
    abstract suspend fun getChildren(id: Long): List<CaldavTask>

    @Query("SELECT IFNULL(MAX(cd_order), -1) + 1 FROM caldav_tasks WHERE cd_calendar = :listId AND gt_parent = :parent")
    abstract suspend fun getBottom(listId: String, parent: Long): Long

    @Query(
        """
SELECT cd_remote_id
FROM caldav_tasks
         JOIN tasks ON tasks._id = cd_task
WHERE deleted = 0
  AND cd_calendar = :listId
  AND gt_parent = :parent
  AND cd_order < :order
  AND cd_remote_id IS NOT NULL
  AND cd_remote_id != ''
ORDER BY cd_order DESC
    """
    )
    abstract suspend fun getPrevious(listId: String, parent: Long, order: Long): String?

    @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_task = :task")
    abstract suspend fun getRemoteId(task: Long): String?

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_remote_id = :remoteId")
    abstract suspend fun getTask(remoteId: String): Long?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
SELECT caldav_tasks.*, cd_order AS primary_sort, NULL AS secondary_sort
FROM caldav_tasks
         JOIN tasks ON tasks._id = cd_task
WHERE gt_parent = 0
  AND cd_calendar = :listId
  AND tasks.deleted = 0
UNION
SELECT c.*, p.cd_order AS primary_sort, c.cd_order AS secondary_sort
FROM caldav_tasks AS c
         LEFT JOIN caldav_tasks AS p ON c.gt_parent = p.cd_task
         JOIN tasks ON tasks._id = c.cd_task
WHERE c.gt_parent > 0
  AND c.cd_calendar = :listId
  AND tasks.deleted = 0
ORDER BY primary_sort ASC, secondary_sort ASC
    """
    )
    abstract suspend fun getByLocalOrder(listId: String): List<CaldavTask>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
SELECT caldav_tasks.*, gt_remote_order AS primary_sort, NULL AS secondary_sort
FROM caldav_tasks
         JOIN tasks ON tasks._id = cd_task
WHERE gt_parent = 0
  AND cd_calendar = :listId
  AND tasks.deleted = 0
UNION
SELECT c.*, p.gt_remote_order AS primary_sort, c.gt_remote_order AS secondary_sort
FROM caldav_tasks AS c
         LEFT JOIN caldav_tasks AS p ON c.gt_parent = p.cd_task
         JOIN tasks ON tasks._id = c.cd_task
WHERE c.gt_parent > 0
  AND c.cd_calendar = :listId
  AND tasks.deleted = 0
ORDER BY primary_sort ASC, secondary_sort ASC
    """
    )
    internal abstract suspend fun getByRemoteOrder(listId: String): List<CaldavTask>
    
    @Query(
        """
UPDATE caldav_tasks
SET gt_parent = IFNULL((SELECT cd_task
                        FROM caldav_tasks AS p
                        WHERE caldav_tasks.cd_remote_parent IS NOT NULL
                          AND caldav_tasks.cd_remote_parent != ''
                          AND p.cd_remote_id = caldav_tasks.cd_remote_parent
                          AND p.cd_calendar = caldav_tasks.cd_calendar
                          AND p.cd_deleted = 0), 0)
WHERE gt_moved = 0
    """
    )
    abstract suspend fun updateParents()

    @Query(
        """
UPDATE caldav_tasks
SET gt_parent = IFNULL((SELECT cd_task
                        FROM caldav_tasks AS p
                        WHERE caldav_tasks.cd_remote_parent IS NOT NULL
                          AND caldav_tasks.cd_remote_parent != ''
                          AND p.cd_remote_id = caldav_tasks.cd_remote_parent
                          AND p.cd_calendar = caldav_tasks.cd_calendar
                          AND p.cd_deleted = 0), 0)
WHERE cd_calendar = :listId
  AND gt_moved = 0
    """
    )
    abstract suspend fun updateParents(listId: String)

    @Query("""
UPDATE caldav_tasks
SET cd_remote_parent = CASE WHEN :parent == '' THEN NULL ELSE :parent END,
    gt_remote_order  = :position
WHERE cd_remote_id = :id
    """)
    abstract suspend fun updatePosition(id: String, parent: String?, position: String)

    @Transaction
    open suspend fun reposition(listId: String) {
        updateParents(listId)
        val orderedTasks = getByRemoteOrder(listId)
        var subtasks = 0L
        var parent = 0L
        for (task in orderedTasks) {
            if (task.parent > 0) {
                if (task.order != subtasks && !task.isMoved) {
                    task.order = subtasks
                    update(task)
                }
                subtasks++
            } else {
                subtasks = 0
                if (task.order != parent && !task.isMoved) {
                    task.order = parent
                    update(task)
                }
                parent++
            }
        }
    }

    suspend fun validateSorting(listId: String) {
        val orderedTasks = getByLocalOrder(listId)
        var subtasks = 0L
        var parent = 0L
        for (task in orderedTasks) {
            if (task.parent > 0) {
                if (task.order != subtasks) {
                    throw IllegalStateException("Subtask violation, expected $subtasks but was ${task.order}")
                }
                subtasks++
            } else {
                subtasks = 0
                if (task.order != parent) {
                    throw IllegalStateException("Parent violation, expected $parent but was ${task.order}")
                }
                parent++
            }
        }
    }
}