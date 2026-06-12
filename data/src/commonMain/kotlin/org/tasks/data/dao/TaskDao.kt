package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Update
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import org.tasks.data.TaskContainer
import org.tasks.data.UUIDHelper
import org.tasks.data.db.Database
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount.Companion.TYPES_CALDAV
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Functions
import org.tasks.time.DateTimeUtils2

private const val MAX_TIME = 9999999999999

@Dao
abstract class TaskDao(private val database: Database) {

    @Query("""
SELECT COALESCE(MIN(min_value), $MAX_TIME)
FROM (
  SELECT
      MIN(
        CASE WHEN dueDate > :now THEN dueDate ELSE $MAX_TIME END,
        CASE WHEN hideUntil > :now THEN hideUntil ELSE $MAX_TIME END
      ) as min_value
  FROM tasks
    WHERE completed = 0 AND deleted = 0
)
    """)
    abstract suspend fun nextRefresh(now: Long = DateTimeUtils2.currentTimeMillis()): Long

    @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    abstract suspend fun fetch(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    abstract fun watch(id: Long): Flow<Task?>

    suspend fun fetch(ids: List<Long>): List<Task> = ids.chunkedMap(this::fetchInternal)

    @Query("SELECT * FROM tasks WHERE _id IN (:ids)")
    internal abstract suspend fun fetchInternal(ids: List<Long>): List<Task>

    @Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0 AND deleted = 0")
    abstract suspend fun activeTimers(): Int

    @Query("SELECT COUNT(1) FROM tasks INNER JOIN alarms ON tasks._id = alarms.task WHERE deleted = 0 AND completed = 0 AND type = ${Alarm.TYPE_SNOOZE}")
    abstract suspend fun snoozedReminders(): Int

    @Query("SELECT COUNT(1) FROM tasks INNER JOIN notification ON tasks._id = notification.task")
    abstract suspend fun hasNotifications(): Int

    @Query("SELECT tasks.* FROM tasks INNER JOIN notification ON tasks._id = notification.task")
    abstract suspend fun activeNotifications(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId")
    abstract suspend fun fetch(remoteId: String): Task?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0")
    abstract suspend fun getActiveTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId IN (:remoteIds) "
            + "AND recurrence IS NOT NULL AND LENGTH(recurrence) > 0")
    abstract suspend fun getRecurringTasks(remoteIds: List<String>): List<Task>

    @Transaction
    open suspend fun setCompletionDate(remoteIds: List<String>, completionDate: Long, updateTime: Long = DateTimeUtils2.currentTimeMillis()) {
        updateCompletionDate(remoteIds, completionDate, updateTime)
        database.dirtyDao().setDirty(getTaskIds(remoteIds))
    }

    @Query("UPDATE tasks SET completed = :completionDate, modified = :updateTime WHERE remoteId IN (:remoteIds)")
    internal abstract suspend fun updateCompletionDate(remoteIds: List<String>, completionDate: Long, updateTime: Long = DateTimeUtils2.currentTimeMillis())

    @Query("SELECT _id FROM tasks WHERE remoteId IN (:remoteIds)")
    internal abstract suspend fun getTaskIds(remoteIds: List<String>): List<Long>

    // --- SQL clause generators
    @Query("SELECT * FROM tasks")
    abstract suspend fun getAll(): List<Task>

    @Query("SELECT _id FROM tasks")
    abstract suspend fun getAllTaskIds(): List<Long>

    @Query("SELECT calendarUri FROM tasks " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
    abstract suspend fun getAllCalendarEvents(): List<String>

    @Query("UPDATE tasks SET calendarUri = '' " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
    abstract suspend fun clearAllCalendarEvents(): Int

    @Query("SELECT calendarUri FROM tasks "
            + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
    abstract suspend fun getCompletedCalendarEvents(): List<String>

    @Query("UPDATE tasks SET calendarUri = '' "
            + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
    abstract suspend fun clearCompletedCalendarEvents(): Int

    suspend fun fetchTasks(query: String): List<TaskContainer> {
        val start = DateTimeUtils2.currentTimeMillis()
        val result = fetchRaw(RoomRawQuery(query))
        val end = DateTimeUtils2.currentTimeMillis()
        Logger.v("TaskDao") { "${end - start}ms: ${query.replace(Regex("\\s+"), " ").trim()}" }
        return result
    }

    @RawQuery
    internal abstract suspend fun fetchRaw(query: RoomRawQuery): List<TaskContainer>

    suspend fun count(query: String): Int {
        val start = DateTimeUtils2.currentTimeMillis()
        val result = countRaw(RoomRawQuery(query))
        val end = DateTimeUtils2.currentTimeMillis()
        Logger.v("TaskDao") { "${end - start}ms: ${query.replace(Regex("\\s+"), " ").trim()}" }
        return result
    }

    @RawQuery
    internal abstract suspend fun countRaw(query: RoomRawQuery): Int

    suspend fun touch(ids: List<Long>) =
        ids.eachChunk { touchInternal(it) }

    @Query("UPDATE tasks SET modified = :now WHERE _id IN (:ids)")
    internal abstract suspend fun touchInternal(ids: List<Long>, now: Long = DateTimeUtils2.currentTimeMillis())

    @Query("UPDATE tasks SET `order` = :order WHERE _id = :id")
    abstract suspend fun setOrder(id: Long, order: Long?)

    suspend fun setParent(parent: Long, tasks: List<Long>) =
            tasks.eachChunk { setParentInternal(parent, it) }

    @Query("UPDATE tasks SET parent = :parent WHERE _id IN (:children) AND _id != :parent")
    internal abstract suspend fun setParentInternal(parent: Long, children: List<Long>)

    @Query("UPDATE tasks SET lastNotified = :timestamp WHERE _id = :id")
    abstract suspend fun setLastNotified(id: Long, timestamp: Long)

    suspend fun getChildren(id: Long): List<Long> = getChildren(listOf(id))

    @Query("""
WITH RECURSIVE recursive_tasks (task) AS (
    SELECT _id
    FROM tasks
    WHERE parent IN (:ids)
    AND deleted = 0
    UNION ALL
    SELECT _id
    FROM tasks
             INNER JOIN recursive_tasks ON recursive_tasks.task = tasks.parent
    WHERE tasks.deleted = 0)
SELECT task
FROM recursive_tasks
    """)
    abstract suspend fun getChildren(ids: List<Long>): List<Long>

    @Query("""
WITH RECURSIVE recursive_tasks (task, parent) AS (
    SELECT _id, parent FROM tasks WHERE _id = :parent
    UNION ALL
    SELECT _id, tasks.parent FROM tasks
        INNER JOIN recursive_tasks ON recursive_tasks.parent = tasks._id
    WHERE tasks.deleted = 0
)
SELECT task
FROM recursive_tasks
""")
    abstract suspend fun getParents(parent: Long): List<Long>

    @Transaction
    open suspend fun setCollapsed(ids: List<Long>, collapsed: Boolean) {
        updateCollapsed(ids, collapsed)
        database.dirtyDao().setDirty(ids, TYPES_CALDAV)
    }

    @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id IN (:ids)")
    internal abstract suspend fun updateCollapsed(ids: List<Long>, collapsed: Boolean)

    @Transaction
    open suspend fun <T> inTransaction(block: suspend () -> T): T = block()

    @Insert
    abstract suspend fun insert(task: Task): Long

    @Transaction
    open suspend fun update(task: Task, original: Task? = null, updateTimestamp: Boolean = true, markDirty: Boolean = false): Boolean {
        if (updateTimestamp && !task.insignificantChange(original)) {
            task.modificationDate = DateTimeUtils2.currentTimeMillis()
        }
        val updated = updateInternal(task) == 1
        if (updated && markDirty) {
            database.dirtyDao().upsertDirty(listOf(task.id))
        }
        return updated
    }

    @Update
    internal abstract suspend fun updateInternal(task: Task): Int

    @Update
    abstract suspend fun updateInternal(tasks: List<Task>)

    suspend fun createNew(task: Task): Long {
        task.id = Task.NO_ID
        if (task.creationDate == 0L) {
            task.creationDate = DateTimeUtils2.currentTimeMillis()
        }
        if (Task.isUuidEmpty(task.remoteId)) {
            task.remoteId = UUIDHelper.newUUID()
        }
        val insert = insert(task)
        task.id = insert
        return task.id
    }

    @Query("""
SELECT _id
FROM tasks
         LEFT JOIN caldav_tasks ON _id = cd_task AND cd_deleted = 0
WHERE cd_id IS NULL
  AND parent = 0
    """)
    abstract suspend fun getLocalTasks(): List<Long>

    /** Generates SQL clauses  */
    object TaskCriteria {
        /** @return tasks that have not yet been completed or deleted
         */
        fun activeAndVisible(): Criterion = Criterion.and(
            Task.COMPLETION_DATE.lte(0),
            Task.DELETION_DATE.lte(0),
            Task.HIDE_UNTIL.lte(Functions.now())
        )
    }
}