package org.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Functions
import com.todoroo.astrid.dao.Database
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NO_ID
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber

private const val MAX_TIME = 9999999999999

@Dao
abstract class TaskDao(private val database: Database) {

    @Query("""
SELECT MIN(min_value)
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
    abstract suspend fun nextRefresh(now: Long = currentTimeMillis()): Long

    @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    abstract suspend fun fetch(id: Long): Task?

    suspend fun fetch(ids: List<Long>): List<Task> = ids.chunkedMap(this::fetchInternal)

    @Query("SELECT * FROM tasks WHERE _id IN (:ids)")
    internal abstract suspend fun fetchInternal(ids: List<Long>): List<Task>

    @Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0 AND deleted = 0")
    abstract suspend fun activeTimers(): Int

    @Query("SELECT COUNT(1) FROM tasks INNER JOIN alarms ON tasks._id = alarms.task WHERE deleted = 0 AND completed = 0 AND type = $TYPE_SNOOZE")
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

    @Query("UPDATE tasks SET completed = :completionDate, modified = :updateTime WHERE remoteId IN (:remoteIds)")
    abstract suspend fun setCompletionDate(remoteIds: List<String>, completionDate: Long, updateTime: Long = currentTimeMillis())

    @Query("SELECT tasks.* FROM tasks "
            + "LEFT JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task "
            + "LEFT JOIN caldav_lists ON caldav_tasks.cd_calendar = caldav_lists.cdl_uuid "
            + "WHERE cdl_account = :account "
            + "AND (tasks.modified > caldav_tasks.cd_last_sync OR caldav_tasks.cd_remote_id = '' OR caldav_tasks.cd_remote_id IS NULL OR caldav_tasks.cd_deleted > 0) "
            + "ORDER BY CASE WHEN parent = 0 THEN 0 ELSE 1 END, `order` ASC")
    abstract suspend fun getGoogleTasksToPush(account: String): List<Task>

    @Query("""
        SELECT tasks.*
        FROM tasks
                 INNER JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task
        WHERE caldav_tasks.cd_calendar = :calendar
          AND cd_deleted = 0
          AND (tasks.modified > caldav_tasks.cd_last_sync OR caldav_tasks.cd_last_sync = 0)
        ORDER BY created""")
    abstract suspend fun getCaldavTasksToPush(calendar: String): List<Task>

    // --- SQL clause generators
    @Query("SELECT * FROM tasks")
    abstract suspend fun getAll(): List<Task>

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

    open suspend fun fetchTasks(callback: suspend () -> List<String>): List<TaskContainer> =
            database.withTransaction {
                val start = if (BuildConfig.DEBUG) currentTimeMillis() else 0
                val queries = callback()
                val last = queries.size - 1
                for (i in 0 until last) {
                    query(SimpleSQLiteQuery(queries[i]))
                }
                val result = fetchTasks(SimpleSQLiteQuery(queries[last]))
                Timber.v("%sms: %s", currentTimeMillis() - start, queries.joinToString(";\n"))
                result
            }

    @RawQuery
    internal abstract suspend fun query(query: SimpleSQLiteQuery): Int

    @RawQuery
    abstract suspend fun fetchTasks(query: SimpleSQLiteQuery): List<TaskContainer>

    @RawQuery
    abstract suspend fun countRaw(query: SimpleSQLiteQuery): Int

    suspend fun touch(ids: List<Long>, now: Long = currentTimeMillis()) =
        ids.eachChunk { internalTouch(it, now) }

    @Query("UPDATE tasks SET modified = :now WHERE _id in (:ids)")
    internal abstract suspend fun internalTouch(ids: List<Long>, now: Long = currentTimeMillis())

    @Query("UPDATE tasks SET `order` = :order WHERE _id = :id")
    abstract suspend fun setOrder(id: Long, order: Long?)

    suspend fun setParent(parent: Long, tasks: List<Long>) =
            tasks.eachChunk { setParentInternal(parent, it) }

    @Query("UPDATE tasks SET parent = :parent WHERE _id IN (:children) AND _id != :parent")
    internal abstract suspend fun setParentInternal(parent: Long, children: List<Long>)

    @Query("UPDATE tasks SET lastNotified = :timestamp WHERE _id = :id AND lastNotified != :timestamp")
    abstract suspend fun setLastNotified(id: Long, timestamp: Long)

    suspend fun getChildren(id: Long): List<Long> = getChildren(listOf(id))

    @Query("""
WITH RECURSIVE recursive_tasks (task) AS (
    SELECT _id
    FROM tasks
    WHERE parent IN (:ids)
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

    @Query("UPDATE tasks SET collapsed = :collapsed, modified = :now WHERE _id IN (:ids)")
    abstract suspend fun setCollapsed(ids: List<Long>, collapsed: Boolean, now: Long = currentTimeMillis())

    @Insert
    abstract suspend fun insert(task: Task): Long

    suspend fun update(task: Task, original: Task? = null): Boolean {
        if (!task.insignificantChange(original)) {
            task.modificationDate = currentTimeMillis()
        }
        return updateInternal(task) == 1
    }

    @Update
    internal abstract suspend fun updateInternal(task: Task): Int

    @Update
    abstract suspend fun updateInternal(tasks: List<Task>)

    suspend fun createNew(task: Task): Long {
        task.id = NO_ID
        if (task.creationDate == 0L) {
            task.creationDate = currentTimeMillis()
        }
        if (Task.isUuidEmpty(task.remoteId)) {
            task.remoteId = UUIDHelper.newUUID()
        }
        if (BuildConfig.DEBUG) {
            require(task.remoteId?.isNotBlank() == true && task.remoteId != "0")
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
        @JvmStatic fun activeAndVisible(): Criterion = Criterion.and(
                Task.COMPLETION_DATE.lte(0),
                Task.DELETION_DATE.lte(0),
                Task.HIDE_UNTIL.lte(Functions.now()))
    }
}