/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import androidx.paging.DataSource
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Field
import com.todoroo.andlib.sql.Functions
import com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NO_ID
import com.todoroo.astrid.helper.UUIDHelper
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.data.Place
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.currentTimeMillis
import timber.log.Timber

@Dao
abstract class TaskDao(private val database: Database) {
    private lateinit var workManager: WorkManager

    fun initialize(workManager: WorkManager) {
        this.workManager = workManager
    }

    @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0 AND (hideUntil > :now OR dueDate > :now)")
    internal abstract suspend fun needsRefresh(now: Long = DateUtilities.now()): List<Task>

    suspend fun fetchBlocking(id: Long) = runBlocking {
        fetch(id)
    }

    @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    abstract suspend fun fetch(id: Long): Task?

    suspend fun fetch(ids: List<Long>): List<Task> = ids.chunkedMap(this::fetchInternal)

    @Query("SELECT * FROM tasks WHERE _id IN (:ids)")
    internal abstract suspend fun fetchInternal(ids: List<Long>): List<Task>

    @Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0 AND deleted = 0")
    abstract suspend fun activeTimers(): Int

    @Query("SELECT tasks.* FROM tasks INNER JOIN notification ON tasks._id = notification.task")
    abstract suspend fun activeNotifications(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId")
    abstract suspend fun fetch(remoteId: String): Task?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0")
    abstract suspend fun getActiveTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId IN (:remoteIds) "
            + "AND recurrence IS NOT NULL AND LENGTH(recurrence) > 0")
    abstract suspend fun getRecurringTasks(remoteIds: List<String>): List<Task>

    @Query("UPDATE tasks SET completed = :completionDate " + "WHERE remoteId = :remoteId")
    abstract suspend fun setCompletionDate(remoteId: String, completionDate: Long)

    @Query("UPDATE tasks SET snoozeTime = :millis WHERE _id in (:taskIds)")
    abstract suspend fun snooze(taskIds: List<Long>, millis: Long)

    @Query("SELECT tasks.* FROM tasks "
            + "LEFT JOIN google_tasks ON tasks._id = google_tasks.gt_task "
            + "WHERE gt_list_id IN (SELECT gtl_remote_id FROM google_task_lists WHERE gtl_account = :account)"
            + "AND (tasks.modified > google_tasks.gt_last_sync OR google_tasks.gt_remote_id = '' OR google_tasks.gt_deleted > 0) "
            + "ORDER BY CASE WHEN gt_parent = 0 THEN 0 ELSE 1 END, gt_order ASC")
    abstract suspend fun getGoogleTasksToPush(account: String): List<Task>

    @Query("""
        SELECT tasks.*
        FROM tasks
                 INNER JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task
        WHERE caldav_tasks.cd_calendar = :calendar
          AND (tasks.modified > caldav_tasks.cd_last_sync OR caldav_tasks.cd_last_sync = 0)""")
    abstract suspend fun getCaldavTasksToPush(calendar: String): List<Task>

    @Query("SELECT * FROM TASKS "
            + "WHERE completed = 0 AND deleted = 0 AND (notificationFlags > 0 OR notifications > 0)")
    abstract suspend fun getTasksWithReminders(): List<Task>

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

    @Transaction
    open suspend fun fetchTasks(callback: (SubtaskInfo) -> List<String>): List<TaskContainer> {
        return fetchTasks(callback, getSubtaskInfo())
    }

    @Transaction
    open suspend fun fetchTasks(callback: (SubtaskInfo) -> List<String>, subtasks: SubtaskInfo): List<TaskContainer> {
        val start = if (BuildConfig.DEBUG) DateUtilities.now() else 0
        val queries = callback.invoke(subtasks)
        val db = database.openHelper.writableDatabase
        val last = queries.size - 1
        for (i in 0 until last) {
            db.execSQL(queries[i])
        }
        val result = fetchTasks(SimpleSQLiteQuery(queries[last]))
        Timber.v("%sms: %s", DateUtilities.now() - start, queries.joinToString(";\n"))
        return result
    }

    suspend fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> {
        return fetchTasks {
            TaskListQuery.getQuery(preferences, filter, it)
        }
    }

    @RawQuery
    abstract suspend fun fetchTasks(query: SimpleSQLiteQuery): List<TaskContainer>

    @RawQuery
    abstract suspend fun count(query: SimpleSQLiteQuery): Int

    @Query("""
SELECT EXISTS(SELECT 1 FROM tasks WHERE parent > 0 AND deleted = 0) AS hasSubtasks,
       EXISTS(SELECT 1
              FROM google_tasks
                       INNER JOIN tasks ON gt_task = _id
              WHERE deleted = 0
                AND gt_parent > 0
                AND gt_deleted = 0)                                 AS hasGoogleSubtasks
    """)
    abstract suspend fun getSubtaskInfo(): SubtaskInfo

    @RawQuery(observedEntities = [Place::class])
    abstract fun getTaskFactory(query: SimpleSQLiteQuery): DataSource.Factory<Int, TaskContainer>

    suspend fun touch(id: Long) = touch(listOf(id))

    suspend fun touch(ids: List<Long>) {
        ids.eachChunk { touchInternal(it) }
        workManager.sync(false)
    }

    @Query("UPDATE tasks SET modified = :now WHERE _id in (:ids)")
    internal abstract suspend fun touchInternal(ids: List<Long>, now: Long = currentTimeMillis())

    suspend fun setParent(parent: Long, tasks: List<Long>) =
            tasks.eachChunk { setParentInternal(parent, it) }

    @Query("UPDATE tasks SET parent = :parent WHERE _id IN (:children)")
    internal abstract suspend fun setParentInternal(parent: Long, children: List<Long>)

    @Transaction
    open suspend fun fetchChildren(id: Long): List<Task> {
        return fetch(getChildren(id))
    }

    suspend fun getChildren(id: Long): List<Long> {
        return getChildren(listOf(id))
    }

    @Query("WITH RECURSIVE "
            + " recursive_tasks (task) AS ( "
            + " SELECT _id "
            + " FROM tasks "
            + "WHERE parent IN (:ids)"
            + "UNION ALL "
            + " SELECT _id "
            + " FROM tasks "
            + " INNER JOIN recursive_tasks "
            + "  ON recursive_tasks.task = tasks.parent"
            + " WHERE tasks.deleted = 0)"
            + "SELECT task FROM recursive_tasks")
    abstract suspend fun getChildren(ids: List<Long>): List<Long>

    @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id = :id")
    abstract suspend fun setCollapsed(id: Long, collapsed: Boolean)

    @Transaction
    open suspend fun setCollapsed(preferences: Preferences, filter: Filter, collapsed: Boolean) {
        fetchTasks(preferences, filter)
                .filter(TaskContainer::hasChildren)
                .map(TaskContainer::getId)
                .eachChunk { collapse(it, collapsed) }
    }

    @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id IN (:ids)")
    internal abstract suspend fun collapse(ids: List<Long>, collapsed: Boolean)

    // --- save
    // TODO: get rid of this super-hack
    /**
     * Saves the given task to the database.getDatabase(). Task must already exist. Returns true on
     * success.
     */

    suspend fun save(task: Task) = save(task, fetchBlocking(task.id))

    suspend fun save(task: Task, original: Task?) {
        if (!task.insignificantChange(original)) {
            task.modificationDate = DateUtilities.now()
        }
        if (task.dueDate != original?.dueDate) {
            task.reminderSnooze = 0
        }
        if (update(task) == 1) {
            workManager.afterSave(task, original)
        }
    }

    @Insert
    abstract suspend fun insert(task: Task): Long

    @Update
    abstract suspend fun update(task: Task): Int

    suspend fun createNew(task: Task) {
        task.id = NO_ID
        if (task.creationDate == 0L) {
            task.creationDate = DateUtilities.now()
        }
        if (Task.isUuidEmpty(task.remoteId)) {
            task.remoteId = UUIDHelper.newUUID()
        }
        if (BuildConfig.DEBUG) {
            require(task.remoteId?.isNotBlank() == true && task.remoteId != "0")
        }
        val insert = insert(task)
        task.id = insert
    }

    suspend fun count(filter: Filter): Int {
        val query = getQuery(filter.sqlQuery, Field.COUNT)
        val start = if (BuildConfig.DEBUG) DateUtilities.now() else 0
        val count = count(query)
        Timber.v("%sms: %s", DateUtilities.now() - start, query.sql)
        return count
    }

    suspend fun fetchFiltered(filter: Filter): List<Task> {
        return fetchFiltered(filter.getSqlQuery())
    }

    suspend fun fetchFiltered(queryTemplate: String): List<Task> {
        val query = getQuery(queryTemplate, Task.FIELDS)
        val start = if (BuildConfig.DEBUG) DateUtilities.now() else 0
        val tasks = fetchTasks(query)
        Timber.v("%sms: %s", DateUtilities.now() - start, query.sql)
        return tasks.map(TaskContainer::getTask)
    }

    @Query("SELECT _id FROM tasks LEFT JOIN google_tasks ON _id = gt_task AND gt_deleted = 0 LEFT JOIN caldav_tasks ON _id = cd_task AND cd_deleted = 0 WHERE gt_id IS NULL AND cd_id IS NULL AND parent = 0")
    abstract suspend fun getLocalTasks(): List<Long>

    /** Generates SQL clauses  */
    object TaskCriteria {
        /** @return tasks that have not yet been completed or deleted
         */
        @JvmStatic fun activeAndVisible(): Criterion {
            return Criterion.and(
                    Task.COMPLETION_DATE.lte(0),
                    Task.DELETION_DATE.lte(0),
                    Task.HIDE_UNTIL.lte(Functions.now()))
        }
    }

    companion object {
        const val TRANS_SUPPRESS_REFRESH = "suppress-refresh"
        fun getQuery(queryTemplate: String, vararg fields: Field): SimpleSQLiteQuery {
            return SimpleSQLiteQuery(
                    com.todoroo.andlib.sql.Query.select(*fields)
                            .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(queryTemplate))
                            .from(Task.TABLE)
                            .toString())
        }
    }
}