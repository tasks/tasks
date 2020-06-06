package org.tasks.data

import androidx.room.*
import com.todoroo.astrid.data.Task
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
abstract class GoogleTaskDao {
    @Insert
    abstract fun insert(task: GoogleTask): Long

    @Insert
    abstract fun insert(tasks: Iterable<GoogleTask>)

    @Transaction
    open fun insertAndShift(task: GoogleTask, top: Boolean) {
        if (top) {
            task.order = 0
            shiftDown(task.listId!!, task.parent, 0)
        } else {
            task.order = getBottom(task.listId!!, task.parent)
        }
        task.id = insert(task)
    }

    @Query("UPDATE google_tasks SET gt_order = gt_order + 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order >= :position")
    abstract fun shiftDown(listId: String, parent: Long, position: Long)

    @Query("UPDATE google_tasks SET gt_order = gt_order - 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order > :from AND gt_order <= :to")
    abstract fun shiftUp(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE google_tasks SET gt_order = gt_order + 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order < :from AND gt_order >= :to")
    abstract fun shiftDown(listId: String, parent: Long, from: Long, to: Long)

    @Query("UPDATE google_tasks SET gt_order = gt_order - 1 WHERE gt_list_id = :listId AND gt_parent = :parent AND gt_order >= :position")
    abstract fun shiftUp(listId: String, parent: Long, position: Long)

    @Transaction
    open fun move(task: SubsetGoogleTask, newParent: Long, newPosition: Long) {
        val previousParent = task.parent
        val previousPosition = task.order
        if (newParent == previousParent) {
            if (previousPosition < newPosition) {
                shiftUp(task.listId, newParent, previousPosition, newPosition)
            } else {
                shiftDown(task.listId, newParent, previousPosition, newPosition)
            }
        } else {
            shiftUp(task.listId, previousParent, previousPosition)
            shiftDown(task.listId, newParent, newPosition)
        }
        task.parent = newParent
        task.order = newPosition
        update(task)
    }

    @Query("UPDATE google_task_accounts SET gta_collapsed = :collapsed WHERE gta_id = :id")
    abstract fun setCollapsed(id: Long, collapsed: Boolean)

    @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId AND gt_deleted = 0 LIMIT 1")
    abstract fun getByTaskId(taskId: Long): GoogleTask?

    @Update
    abstract fun update(googleTask: GoogleTask)

    private fun update(googleTask: SubsetGoogleTask) {
        update(googleTask.id, googleTask.parent, googleTask.order)
    }

    @Query("UPDATE google_tasks SET gt_order = :order, gt_parent = :parent, gt_moved = 1 WHERE gt_id = :id")
    abstract fun update(id: Long, parent: Long, order: Long)

    @Query("UPDATE google_tasks SET gt_deleted = :now WHERE gt_task = :task OR gt_parent = :task")
    abstract fun markDeleted(task: Long, now: Long = currentTimeMillis())

    @Delete
    abstract fun delete(deleted: GoogleTask)

    @Query("SELECT * FROM google_tasks WHERE gt_remote_id = :remoteId LIMIT 1")
    abstract fun getByRemoteId(remoteId: String): GoogleTask?

    @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId AND gt_deleted > 0")
    abstract fun getDeletedByTaskId(taskId: Long): List<GoogleTask>

    @Query("SELECT * FROM google_tasks WHERE gt_task = :taskId")
    abstract fun getAllByTaskId(taskId: Long): List<GoogleTask>

    @Query("SELECT DISTINCT gt_list_id FROM google_tasks WHERE gt_deleted = 0 AND gt_task IN (:tasks)")
    abstract fun getLists(tasks: List<Long>): List<String>

    @Query("SELECT gt_task FROM google_tasks WHERE gt_parent IN (:ids) AND gt_deleted = 0")
    abstract fun getChildren(ids: List<Long>): List<Long>

    @Query("SELECT tasks.* FROM tasks JOIN google_tasks ON tasks._id = gt_task WHERE gt_parent = :taskId")
    abstract fun getChildTasks(taskId: Long): List<Task>

    @Query("SELECT * FROM google_tasks WHERE gt_parent = :id AND gt_deleted = 0")
    abstract fun getChildren(id: Long): List<GoogleTask>

    @Query("SELECT IFNULL(MAX(gt_order), -1) + 1 FROM google_tasks WHERE gt_list_id = :listId AND gt_parent = :parent")
    abstract fun getBottom(listId: String, parent: Long): Long

    @Query("SELECT gt_remote_id FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE deleted = 0 AND gt_list_id = :listId AND gt_parent = :parent AND gt_order < :order AND gt_remote_id IS NOT NULL AND gt_remote_id != '' ORDER BY gt_order DESC")
    abstract fun getPrevious(listId: String, parent: Long, order: Long): String?

    @Query("SELECT gt_remote_id FROM google_tasks WHERE gt_task = :task")
    abstract fun getRemoteId(task: Long): String?

    @Query("SELECT gt_task FROM google_tasks WHERE gt_remote_id = :remoteId")
    abstract fun getTask(remoteId: String): Long

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT google_tasks.*, gt_order AS primary_sort, NULL AS secondary_sort FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE gt_parent = 0 AND gt_list_id = :listId AND tasks.deleted = 0 UNION SELECT c.*, p.gt_order AS primary_sort, c.gt_order AS secondary_sort FROM google_tasks AS c LEFT JOIN google_tasks AS p ON c.gt_parent = p.gt_task JOIN tasks ON tasks._id = c.gt_task WHERE c.gt_parent > 0 AND c.gt_list_id = :listId AND tasks.deleted = 0 ORDER BY primary_sort ASC, secondary_sort ASC")
    abstract fun getByLocalOrder(listId: String): List<GoogleTask>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT google_tasks.*, gt_remote_order AS primary_sort, NULL AS secondary_sort FROM google_tasks JOIN tasks ON tasks._id = gt_task WHERE gt_parent = 0 AND gt_list_id = :listId AND tasks.deleted = 0 UNION SELECT c.*, p.gt_remote_order AS primary_sort, c.gt_remote_order AS secondary_sort FROM google_tasks AS c LEFT JOIN google_tasks AS p ON c.gt_parent = p.gt_task JOIN tasks ON tasks._id = c.gt_task WHERE c.gt_parent > 0 AND c.gt_list_id = :listId AND tasks.deleted = 0 ORDER BY primary_sort ASC, secondary_sort ASC")
    abstract fun getByRemoteOrder(listId: String): List<GoogleTask>

    @Query("UPDATE google_tasks"
            + " SET gt_parent = IFNULL(("
            + "   SELECT gt_task FROM google_tasks AS p"
            + "   WHERE p.gt_remote_id = google_tasks.gt_remote_parent"
            + "     AND p.gt_list_id = google_tasks.gt_list_id "
            + "     AND p.gt_deleted = 0),"
            + " 0)"
            + " WHERE gt_moved = 0")
    abstract fun updateParents()

    @Query("UPDATE google_tasks SET gt_parent = IFNULL((SELECT gt_task FROM google_tasks AS p WHERE p.gt_remote_id = google_tasks.gt_remote_parent), 0) WHERE gt_list_id = :listId AND gt_moved = 0")
    abstract fun updateParents(listId: String)

    @Query("UPDATE google_tasks SET gt_remote_parent = :parent, gt_remote_order = :position WHERE gt_remote_id = :id")
    abstract fun updatePosition(id: String, parent: String, position: String)

    @Transaction
    open fun reposition(listId: String) {
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

    fun validateSorting(listId: String) {
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