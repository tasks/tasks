package org.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todoroo.astrid.data.Task
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS

@Dao
abstract class GoogleTaskDao {
    @Insert
    abstract suspend fun insert(task: CaldavTask): Long

    @Insert
    abstract suspend fun insert(tasks: Iterable<CaldavTask>)

    @Transaction
    open suspend fun insertAndShift(task: Task, caldavTask: CaldavTask, top: Boolean) {
        if (top) {
            task.order = 0
            shiftDown(caldavTask.calendar!!, task.parent, 0)
        } else {
            task.order = getBottom(caldavTask.calendar!!, task.parent)
        }
        insert(caldavTask)
        update(task)
    }

    @Query("UPDATE tasks SET `order` = `order` + 1 WHERE parent = :parent AND `order` >= :position AND _id IN (SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :listId)")
    internal abstract suspend fun shiftDown(listId: String, parent: Long, position: Long)

    @Query("UPDATE tasks SET `order` = `order` - 1 WHERE parent = :parent AND `order` > :from AND `order` <= :to  AND _id IN (SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :listId)")
    internal abstract suspend fun shiftUp(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE tasks SET `order` = `order` + 1 WHERE parent = :parent AND `order` < :from AND `order` >= :to  AND _id IN (SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :listId)")
    internal abstract suspend fun shiftDown(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE tasks SET `order` = `order` - 1 WHERE parent = :parent AND `order` >= :position AND _id IN (SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :listId)")
    internal abstract suspend fun shiftUp(listId: String, parent: Long, position: Long)

    @Transaction
    open suspend fun move(task: Task, list: String, newParent: Long, newPosition: Long) {
        val previousParent = task.parent
        val previousPosition = task.order!!
        if (newParent == previousParent) {
            if (previousPosition < newPosition) {
                shiftUp(list, newParent, previousPosition, newPosition)
            } else {
                shiftDown(list, newParent, previousPosition, newPosition)
            }
        } else {
            shiftUp(list, previousParent, previousPosition)
            shiftDown(list, newParent, newPosition)
        }
        task.parent = newParent
        task.order = newPosition
        update(task)
        setMoved(task.id, list)
    }

    @Query("UPDATE caldav_tasks SET gt_moved = 1 WHERE cd_task = :task and cd_calendar = :list")
    internal abstract suspend fun setMoved(task: Long, list: String)

    @Query("SELECT caldav_tasks.* FROM caldav_tasks INNER JOIN caldav_lists ON cdl_uuid = cd_calendar INNER JOIN caldav_accounts ON cda_uuid = cdl_account WHERE cd_task = :taskId AND cd_deleted = 0 AND cda_account_type = $TYPE_GOOGLE_TASKS LIMIT 1")
    abstract suspend fun getByTaskId(taskId: Long): CaldavTask?

    @Update
    abstract suspend fun update(googleTask: CaldavTask)

    @Update
    abstract suspend fun update(task: Task)

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

    @Query("SELECT IFNULL(MAX(`order`), -1) + 1 FROM tasks INNER JOIN caldav_tasks ON cd_task = tasks._id WHERE cd_calendar = :listId AND parent = :parent")
    abstract suspend fun getBottom(listId: String, parent: Long): Long

    @Query(
        """
SELECT cd_remote_id
FROM caldav_tasks
         INNER JOIN tasks ON tasks._id = cd_task
WHERE deleted = 0
  AND cd_calendar = :listId
  AND parent = :parent
  AND `order` < :order
  AND cd_remote_id IS NOT NULL
  AND cd_remote_id != ''
ORDER BY `order` DESC
    """
    )
    abstract suspend fun getPrevious(listId: String, parent: Long, order: Long): String?

    @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_task = :task")
    abstract suspend fun getRemoteId(task: Long): String?

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_remote_id = :remoteId")
    abstract suspend fun getTask(remoteId: String): Long?

    @Query(
        """
SELECT tasks.*, `order` AS primary_sort, NULL AS secondary_sort
FROM tasks
         INNER JOIN caldav_tasks ON tasks._id = cd_task
WHERE parent = 0
  AND cd_calendar = :listId
  AND tasks.deleted = 0
UNION
SELECT c.*, p.`order` AS primary_sort, c.`order` AS secondary_sort
FROM tasks AS c
         INNER JOIN tasks AS p ON c.parent = p._id
         INNER JOIN caldav_tasks ON c._id = cd_task
WHERE c.parent > 0
  AND cd_calendar = :listId
  AND c.deleted = 0
ORDER BY primary_sort ASC, secondary_sort ASC
    """
    )
    internal abstract suspend fun getByLocalOrder(listId: String): List<Task>

    @Query(
        """
SELECT tasks.*, gt_remote_order AS primary_sort, NULL AS secondary_sort
FROM tasks
         JOIN caldav_tasks ON tasks._id = cd_task
WHERE parent = 0
  AND cd_calendar = :listId
  AND tasks.deleted = 0
UNION
SELECT c.*, parent.gt_remote_order AS primary_sort, child.gt_remote_order AS secondary_sort
FROM tasks AS c
         INNER JOIN tasks AS p ON c.parent = p._id
         INNER JOIN caldav_tasks AS child ON c._id = child.cd_task
         INNER JOIN caldav_tasks AS parent ON p._id = parent.cd_task
WHERE c.parent > 0
  AND child.cd_calendar = :listId
  AND c.deleted = 0
ORDER BY primary_sort ASC, secondary_sort ASC
    """
    )
    internal abstract suspend fun getByRemoteOrder(listId: String): List<Task>
    
    @Query("""
UPDATE caldav_tasks
SET cd_remote_parent = CASE WHEN :parent == '' THEN NULL ELSE :parent END,
    gt_remote_order  = :position
WHERE cd_remote_id = :id
    """)
    abstract suspend fun updatePosition(id: String, parent: String?, position: String)

    @Transaction
    open suspend fun reposition(caldavDao: CaldavDao, listId: String) {
        caldavDao.updateParents(listId)
        val orderedTasks = getByRemoteOrder(listId)
        var subtasks = 0L
        var parent = 0L
        for (task in orderedTasks) {
            if (task.parent > 0) {
                if (task.order != subtasks) {
                    task.order = subtasks
                    update(task)
                }
                subtasks++
            } else {
                subtasks = 0
                if (task.order != parent) {
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