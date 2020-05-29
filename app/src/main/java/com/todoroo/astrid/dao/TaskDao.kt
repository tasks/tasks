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
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NO_ID
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.BuildConfig
import org.tasks.data.Place
import org.tasks.data.SubtaskInfo
import org.tasks.data.TaskContainer
import org.tasks.data.TaskListQuery
import org.tasks.db.DbUtils
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.*

@Dao
abstract class TaskDao(private val database: Database) {
    private lateinit var workManager: WorkManager

    fun initialize(workManager: WorkManager) {
        this.workManager = workManager
    }

    fun needsRefresh(): List<Task> {
        return needsRefresh(DateUtilities.now())
    }

    @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0 AND (hideUntil > :now OR dueDate > :now)")
    abstract fun needsRefresh(now: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    abstract fun fetch(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE _id IN (:taskIds)")
    abstract fun fetch(taskIds: List<Long>): List<Task>

    @Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0 AND deleted = 0")
    abstract fun activeTimers(): Int

    @Query("SELECT tasks.* FROM tasks INNER JOIN notification ON tasks._id = notification.task")
    abstract fun activeNotifications(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId")
    abstract fun fetch(remoteId: String): Task?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0")
    abstract fun getActiveTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE hideUntil < (strftime('%s','now')*1000)")
    abstract fun getVisibleTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE remoteId IN (:remoteIds) "
            + "AND recurrence IS NOT NULL AND LENGTH(recurrence) > 0")
    abstract fun getRecurringTasks(remoteIds: List<String>): List<Task>

    @Query("UPDATE tasks SET completed = :completionDate " + "WHERE remoteId = :remoteId")
    abstract fun setCompletionDate(remoteId: String, completionDate: Long)

    @Query("UPDATE tasks SET snoozeTime = :millis WHERE _id in (:taskIds)")
    abstract fun snooze(taskIds: List<Long>, millis: Long)

    @Query("SELECT tasks.* FROM tasks "
            + "LEFT JOIN google_tasks ON tasks._id = google_tasks.gt_task "
            + "WHERE gt_list_id IN (SELECT gtl_remote_id FROM google_task_lists WHERE gtl_account = :account)"
            + "AND (tasks.modified > google_tasks.gt_last_sync OR google_tasks.gt_remote_id = '' OR google_tasks.gt_deleted > 0) "
            + "ORDER BY CASE WHEN gt_parent = 0 THEN 0 ELSE 1 END, gt_order ASC")
    abstract fun getGoogleTasksToPush(account: String): List<Task>

    @Query("SELECT tasks.* FROM tasks "
            + "LEFT JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task "
            + "WHERE caldav_tasks.cd_calendar = :calendar "
            + "AND tasks.modified > caldav_tasks.cd_last_sync")
    abstract fun getCaldavTasksToPush(calendar: String): List<Task>

    @Query("SELECT * FROM TASKS "
            + "WHERE completed = 0 AND deleted = 0 AND (notificationFlags > 0 OR notifications > 0)")
    abstract fun getTasksWithReminders(): List<Task>

    // --- SQL clause generators
    @Query("SELECT * FROM tasks")
    abstract fun getAll(): List<Task>

    @Query("SELECT calendarUri FROM tasks " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
    abstract fun getAllCalendarEvents(): List<String>

    @Query("UPDATE tasks SET calendarUri = '' " + "WHERE calendarUri IS NOT NULL AND calendarUri != ''")
    abstract fun clearAllCalendarEvents(): Int

    @Query("SELECT calendarUri FROM tasks "
            + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
    abstract fun getCompletedCalendarEvents(): List<String>

    @Query("UPDATE tasks SET calendarUri = '' "
            + "WHERE completed > 0 AND calendarUri IS NOT NULL AND calendarUri != ''")
    abstract fun clearCompletedCalendarEvents(): Int

    @Transaction
    open fun fetchTasks(callback: (SubtaskInfo) -> List<String>): List<TaskContainer> {
        return fetchTasks(callback, getSubtaskInfo())
    }

    @Transaction
    open fun fetchTasks(callback: (SubtaskInfo) -> List<String>, subtasks: SubtaskInfo): List<TaskContainer> {
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

    fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> {
        return fetchTasks {
            TaskListQuery.getQuery(preferences, filter, it)
        }
    }

    @RawQuery
    abstract fun fetchTasks(query: SimpleSQLiteQuery): List<TaskContainer>

    @RawQuery
    abstract fun count(query: SimpleSQLiteQuery): Int

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE parent > 0 AND deleted = 0) AS hasSubtasks,"
            + "EXISTS(SELECT 1 FROM google_tasks "
            + "  INNER JOIN tasks ON gt_task = _id "
            + " WHERE deleted = 0 AND gt_parent > 0 AND gt_deleted = 0) AS hasGoogleSubtasks")
    abstract fun getSubtaskInfo(): SubtaskInfo

    @RawQuery(observedEntities = [Place::class])
    abstract fun getTaskFactory(
            query: SimpleSQLiteQuery): DataSource.Factory<Int, TaskContainer>

    fun touch(id: Long) {
        touch(listOf(id))
    }

    fun touch(ids: List<Long>) {
        touchInternal(ids)
        workManager.sync(false)
    }

    @Query("UPDATE tasks SET modified = strftime('%s','now')*1000 WHERE _id in (:ids)")
    abstract fun touchInternal(ids: List<Long>)

    @Query("UPDATE tasks SET parent = IFNULL(("
            + " SELECT parent._id FROM tasks AS parent"
            + " WHERE parent.remoteId = tasks.parent_uuid AND parent.deleted = 0), 0)"
            + "WHERE parent_uuid IS NOT NULL AND parent_uuid != ''")
    abstract fun updateParents()

    @Query("UPDATE tasks SET parent_uuid = "
            + "  (SELECT parent.remoteId FROM tasks AS parent WHERE parent._id = tasks.parent)"
            + "  WHERE parent > 0 AND _id IN (:tasks)")
    abstract fun updateParentUids(tasks: List<Long>)

    @Query("UPDATE tasks SET parent = :parent, parent_uuid = :parentUuid WHERE _id IN (:children)")
    abstract fun setParent(parent: Long, parentUuid: String?, children: List<Long>)

    @Transaction
    open fun fetchChildren(id: Long): List<Task> {
        return fetch(getChildren(id))
    }

    fun getChildren(id: Long): List<Long> {
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
    abstract fun getChildren(ids: List<Long>): List<Long>

    fun findChildrenInList(ids: List<Long>): List<Long> {
        val result = ArrayList(ids)
        result.retainAll(getChildren(ids))
        return result
    }

    @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id = :id")
    abstract fun setCollapsed(id: Long, collapsed: Boolean)

    @Transaction
    open fun setCollapsed(preferences: Preferences, filter: Filter, collapsed: Boolean) {
        val tasks = fetchTasks(preferences, filter)
                .filter(TaskContainer::hasChildren)
                .map(TaskContainer::getId)
        DbUtils.batch(tasks) {
            collapse(it, collapsed)
        }
    }

    @Query("UPDATE tasks SET collapsed = :collapsed WHERE _id IN (:ids)")
    abstract fun collapse(ids: List<Long>, collapsed: Boolean)

    // --- save
    // TODO: get rid of this super-hack
    /**
     * Saves the given task to the database.getDatabase(). Task must already exist. Returns true on
     * success.
     */
    @JvmOverloads
    fun save(task: Task, original: Task? = fetch(task.id)) {
        if (!task.insignificantChange(original)) {
            task.modificationDate = DateUtilities.now()
        }
        if (update(task) == 1) {
            workManager.afterSave(task, original)
        }
    }

    @Insert
    abstract fun insert(task: Task): Long

    @Update
    abstract fun update(task: Task): Int

    fun createNew(task: Task) {
        task.id = NO_ID
        if (task.creationDate == 0L) {
            task.creationDate = DateUtilities.now()
        }
        if (Task.isUuidEmpty(task.remoteId)) {
            task.remoteId = UUIDHelper.newUUID()
        }
        val insert = insert(task)
        task.id = insert
    }

    @Query("SELECT * FROM tasks "
            + "WHERE completed = 0 AND deleted = 0 AND hideUntil < (strftime('%s','now')*1000) "
            + "ORDER BY (CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE ((CASE WHEN (dueDate / 1000) % 60 > 0 THEN dueDate ELSE (dueDate + 43140000) END)) END) + 172800000 * importance ASC "
            + "LIMIT 100")
    abstract fun getAstrid2TaskProviderTasks(): List<Task>

    fun count(filter: Filter): Int {
        val query = getQuery(filter.sqlQuery, Field.COUNT)
        val start = if (BuildConfig.DEBUG) DateUtilities.now() else 0
        val count = count(query)
        Timber.v("%sms: %s", DateUtilities.now() - start, query.sql)
        return count
    }

    fun fetchFiltered(filter: Filter): List<Task> {
        return fetchFiltered(filter.getSqlQuery())
    }

    fun fetchFiltered(queryTemplate: String): List<Task> {
        val query = getQuery(queryTemplate, Task.FIELDS)
        val start = if (BuildConfig.DEBUG) DateUtilities.now() else 0
        val tasks = fetchTasks(query)
        Timber.v("%sms: %s", DateUtilities.now() - start, query.sql)
        return tasks.map(TaskContainer::getTask)
    }

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
        private fun getQuery(queryTemplate: String, vararg fields: Field): SimpleSQLiteQuery {
            return SimpleSQLiteQuery(
                    com.todoroo.andlib.sql.Query.select(*fields)
                            .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(queryTemplate))
                            .from(Task.TABLE)
                            .toString())
        }
    }
}