package org.tasks.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.core.SortHelper.APPLE_EPOCH
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.R
import org.tasks.data.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.date.DateTimeUtils.toAppleEpoch
import org.tasks.db.DbUtils.dbchunk
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.filters.CaldavFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
abstract class CaldavDao {
    @Query("SELECT COUNT(*) FROM caldav_lists WHERE cdl_account = :account")
    abstract fun listCount(account: String): Int

    @Query("SELECT * FROM caldav_lists")
    abstract fun subscribeToCalendars(): LiveData<List<CaldavCalendar>>

    @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
    abstract suspend fun getCalendarByUuid(uuid: String): CaldavCalendar?

    @Query("SELECT * FROM caldav_lists WHERE cdl_id = :id LIMIT 1")
    abstract suspend fun getCalendarById(id: Long): CaldavCalendar?

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :uuid")
    abstract suspend fun getCalendarsByAccount(uuid: String): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_accounts WHERE cda_uuid = :uuid LIMIT 1")
    abstract suspend fun getAccountByUuid(uuid: String): CaldavAccount?

    suspend fun getAccounts(vararg types: Int) = getAccounts(types.toList())

    @Query("SELECT * FROM caldav_accounts WHERE cda_account_type IN (:types)")
    abstract suspend fun getAccounts(types: List<Int>): List<CaldavAccount>

    @Query("SELECT * FROM caldav_accounts WHERE cda_account_type = :type AND cda_username = :username")
    abstract suspend fun getAccount(type: Int, username: String): CaldavAccount?

    @Query("SELECT * FROM caldav_accounts WHERE cda_id = :id")
    abstract fun watchAccount(id: Long): LiveData<CaldavAccount>

    @Query("""
SELECT *
FROM caldav_accounts
WHERE cda_account_type != $TYPE_LOCAL
ORDER BY CASE cda_account_type
             WHEN $TYPE_TASKS THEN 0
             ELSE 1
             END, UPPER(cda_name)
    """)
    abstract fun watchAccounts(): LiveData<List<CaldavAccount>>

    @Query("""
SELECT *
FROM caldav_accounts
ORDER BY CASE cda_account_type
             WHEN $TYPE_TASKS THEN 0
             WHEN $TYPE_LOCAL THEN 2
             ELSE 1
             END, UPPER(cda_name)
    """)
    abstract suspend fun getAccounts(): List<CaldavAccount>

    @Query("UPDATE caldav_accounts SET cda_collapsed = :collapsed WHERE cda_id = :id")
    abstract suspend fun setCollapsed(id: Long, collapsed: Boolean)

    @Insert
    abstract suspend fun insert(caldavAccount: CaldavAccount): Long

    @Update
    abstract suspend fun update(caldavAccount: CaldavAccount)

    suspend fun insert(caldavCalendar: CaldavCalendar) {
        caldavCalendar.id = insertInternal(caldavCalendar)
    }

    @Insert
    internal abstract suspend fun insertInternal(caldavCalendar: CaldavCalendar): Long

    @Update
    abstract suspend fun update(caldavCalendar: CaldavCalendar)

    @Transaction
    open suspend fun insert(task: Task, caldavTask: CaldavTask, addToTop: Boolean): Long {
        if (task.order != null) {
            return insert(caldavTask)
        }
        if (addToTop) {
            task.order = findFirstTask(caldavTask.calendar!!, task.parent)
                    ?.takeIf { task.creationDate.toAppleEpoch() >= it }
                    ?.minus(1)
        } else {
            task.order = findLastTask(caldavTask.calendar!!, task.parent)
                    ?.takeIf { task.creationDate.toAppleEpoch() <= it }
                    ?.plus(1)
        }
        val id = insert(caldavTask)
        update(task)
        return id
    }

    @Query("""
SELECT MIN(IFNULL(`order`, (created - $APPLE_EPOCH) / 1000))
FROM caldav_tasks
         INNER JOIN tasks ON _id = cd_task
WHERE cd_calendar = :calendar
  AND cd_deleted = 0
  AND deleted = 0
  AND parent = :parent
    """)
    internal abstract suspend fun findFirstTask(calendar: String, parent: Long): Long?

    @Query("""
SELECT MAX(IFNULL(`order`, (created - $APPLE_EPOCH) / 1000))
FROM caldav_tasks
         INNER JOIN tasks ON _id = cd_task
WHERE cd_calendar = :calendar
  AND cd_deleted = 0
  AND deleted = 0
  AND parent = :parent
    """)
    internal abstract suspend fun findLastTask(calendar: String, parent: Long): Long?

    @Insert
    abstract suspend fun insert(caldavTask: CaldavTask): Long

    @Insert
    abstract suspend fun insert(tasks: Iterable<CaldavTask>)

    @Update
    abstract suspend fun update(caldavTask: CaldavTask)

    @Update
    abstract suspend fun update(task: Task)

    @Update
    abstract suspend fun updateTasks(tasks: Iterable<Task>)

    @Query("UPDATE caldav_tasks SET cd_remote_parent = :remoteParent WHERE cd_id = :id")
    internal abstract suspend fun update(id: Long, remoteParent: String?)

    @Update
    abstract suspend fun update(tasks: Iterable<CaldavTask>)

    @Delete
    abstract suspend fun delete(caldavTask: CaldavTask)

    @Delete
    abstract suspend fun delete(caldavTasks: List<CaldavTask>)

    @Query("SELECT * FROM caldav_tasks WHERE cd_deleted > 0 AND cd_calendar = :calendar")
    abstract suspend fun getMoved(calendar: String): List<CaldavTask>

    @Query("UPDATE caldav_tasks SET cd_deleted = :now WHERE cd_task IN (:tasks)")
    abstract suspend fun markDeleted(tasks: List<Long>, now: Long = currentTimeMillis())

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
    abstract suspend fun getTask(taskId: Long): CaldavTask?

    @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0")
    abstract suspend fun getRemoteIdForTask(taskId: Long): String?

    @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object = :obj LIMIT 1")
    abstract suspend fun getTask(calendar: String, obj: String): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_remote_id = :remoteId")
    abstract suspend fun getTaskByRemoteId(calendar: String, remoteId: String): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId")
    abstract suspend fun getTasks(taskId: Long): List<CaldavTask>

    @Query("""
SELECT EXISTS(SELECT 1
              FROM caldav_tasks
                       INNER JOIN caldav_lists ON cdl_uuid = cd_calendar
                       INNER JOIN caldav_accounts ON cda_uuid = cdl_account
              WHERE cd_task = :id
                AND cda_account_type IN (:types))
""")
    abstract suspend fun isAccountType(id: Long, types: List<Int>): Boolean

    suspend fun getTasks(taskIds: List<Long>): List<CaldavTask> =
            taskIds.chunkedMap { getTasksInternal(it) }

    @Query("SELECT * FROM caldav_tasks WHERE cd_task in (:taskIds) AND cd_deleted = 0")
    internal abstract suspend fun getTasksInternal(taskIds: List<Long>): List<CaldavTask>

    @Query("SELECT task.*, caldav_task.* FROM tasks AS task "
            + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
            + "WHERE cd_calendar = :calendar "
            + "AND modified > cd_last_sync "
            + "AND cd_deleted = 0")
    abstract suspend fun getCaldavTasksToPush(calendar: String): List<CaldavTaskContainer>

    @Query("SELECT * FROM caldav_lists " +
            "INNER JOIN caldav_accounts ON caldav_lists.cdl_account = caldav_accounts.cda_uuid " +
            "WHERE caldav_accounts.cda_account_type = $TYPE_GOOGLE_TASKS " +
            "ORDER BY cdl_name COLLATE NOCASE")
    abstract suspend fun getGoogleTaskLists(): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_lists " +
            "INNER JOIN caldav_accounts ON caldav_lists.cdl_account = caldav_accounts.cda_uuid " +
            "WHERE caldav_accounts.cda_account_type != $TYPE_GOOGLE_TASKS " +
            "ORDER BY cdl_name COLLATE NOCASE")
    abstract suspend fun getCalendars(): List<CaldavCalendar>

    @Query("""
SELECT EXISTS(SELECT 1
              FROM caldav_lists
                       INNER JOIN caldav_accounts ON cdl_account = cda_uuid
              WHERE cda_account_type != $TYPE_OPENTASKS
                AND cda_account_type != $TYPE_ETESYNC
                AND cdl_url IN (:urls))
    """)
    abstract suspend fun anyExist(urls: List<String>): Boolean

    @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
    abstract suspend fun getCalendar(uuid: String): CaldavCalendar?

    @Query("SELECT cd_object FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0 AND cd_last_sync > 0")
    abstract suspend fun getRemoteObjects(calendar: String): List<String>

    @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0 AND cd_last_sync > 0")
    abstract suspend fun getRemoteIds(calendar: String): List<String>

    suspend fun getTasksByRemoteId(calendar: String, remoteIds: List<String>): List<Long> =
            remoteIds.chunkedMap { getTasksByRemoteIdInternal(calendar, it) }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_remote_id IN (:remoteIds)")
    internal abstract suspend fun getTasksByRemoteIdInternal(calendar: String, remoteIds: List<String>): List<Long>

    suspend fun getTasks(calendar: String, objects: List<String>): List<Long> =
            objects.chunkedMap { getTasksInternal(calendar, it) }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object IN (:objects)")
    internal abstract suspend fun getTasksInternal(calendar: String, objects: List<String>): List<Long>

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url NOT IN (:urls)")
    abstract suspend fun findDeletedCalendars(account: String, urls: List<String>): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url = :url LIMIT 1")
    abstract suspend fun getCalendarByUrl(account: String, url: String): CaldavCalendar?

    @Query("SELECT caldav_accounts.* from caldav_accounts"
            + " INNER JOIN caldav_tasks ON cd_task = :task"
            + " INNER JOIN caldav_lists ON cd_calendar = cdl_uuid"
            + " WHERE cdl_account = cda_uuid")
    abstract suspend fun getAccountForTask(task: Long): CaldavAccount?

    @Query("SELECT DISTINCT cd_calendar FROM caldav_tasks WHERE cd_deleted = 0 AND cd_task IN (:tasks)")
    abstract suspend fun getCalendars(tasks: List<Long>): List<String>

    @Query("""
SELECT caldav_lists.*, COUNT(DISTINCT(tasks._id)) AS count, COUNT(DISTINCT(principal_access.id)) AS principals
FROM caldav_lists
         LEFT JOIN caldav_tasks ON caldav_tasks.cd_calendar = caldav_lists.cdl_uuid
         LEFT JOIN tasks ON caldav_tasks.cd_task = tasks._id AND 
                            tasks.deleted = 0 AND 
                            tasks.completed = 0 AND
                            tasks.hideUntil < :now AND 
                            cd_deleted = 0
         LEFT JOIN principal_access ON caldav_lists.cdl_id = principal_access.list
         LEFT JOIN caldav_accounts ON caldav_accounts.cda_uuid = caldav_lists.cdl_account
WHERE caldav_lists.cdl_account = :uuid
AND caldav_accounts.cda_account_type != $TYPE_GOOGLE_TASKS 
GROUP BY caldav_lists.cdl_uuid
    """)
    abstract suspend fun getCaldavFilters(uuid: String, now: Long = currentTimeMillis()): List<CaldavFilters>

    @Query("UPDATE tasks SET parent = IFNULL(("
            + " SELECT p.cd_task FROM caldav_tasks AS p"
            + "  INNER JOIN caldav_tasks ON caldav_tasks.cd_task = tasks._id"
            + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
            + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
            + "    AND p.cd_deleted = 0"
            + "    AND caldav_tasks.cd_remote_parent IS NOT NULL"
            + "    AND caldav_tasks.cd_remote_parent != ''"
            + "), 0)"
            + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0)")
    abstract suspend fun updateParents()

    @Query("UPDATE tasks SET parent = IFNULL(("
            + " SELECT p.cd_task FROM caldav_tasks AS p"
            + "  INNER JOIN caldav_tasks "
            + "    ON caldav_tasks.cd_task = tasks._id"
            + "    AND caldav_tasks.cd_calendar = :calendar"
            + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
            + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
            + "    AND p.cd_deleted = 0"
            + "    AND caldav_tasks.cd_remote_parent IS NOT NULL"
            + "    AND caldav_tasks.cd_remote_parent != ''"
            + "), 0)"
            + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0 AND cd_calendar = :calendar)")
    abstract suspend fun updateParents(calendar: String)

    @Transaction
    open suspend fun move(task: TaskContainer, previousParent: Long, newParent: Long, newPosition: Long?) {
        val previousPosition = task.caldavSortOrder
        if (newPosition != null) {
            if (newParent == previousParent && newPosition < previousPosition) {
                shiftDown(task.caldav!!, newParent, newPosition, previousPosition)
            } else {
                shiftDown(task.caldav!!, newParent, newPosition)
            }
        }
        task.task.order = newPosition
        setTaskOrder(task.id, newPosition)
    }

    @Transaction
    open suspend fun shiftDown(calendar: String, parent: Long, from: Long, to: Long? = null) {
        val updated = ArrayList<Task>()
        val tasks = getTasksToShift(calendar, parent, from, to)
        for (i in tasks.indices) {
            val task = tasks[i]
            val current = from + i
            if (task.sortOrder == current) {
                val task = task.task
                task.order = current + 1
                updated.add(task)
            } else if (task.sortOrder > current) {
                break
            }
        }
        updateTasks(updated)
        updated
                .map(Task::id)
                .dbchunk()
                .forEach { touchInternal(it) }
    }

    @Query("UPDATE tasks SET modified = :modificationTime WHERE _id in (:ids)")
    internal abstract suspend fun touchInternal(ids: List<Long>, modificationTime: Long = now())

    @Query("""
SELECT task.*, caldav_task.*, IFNULL(`order`, (created - $APPLE_EPOCH) / 1000) AS primary_sort
FROM caldav_tasks AS caldav_task
         INNER JOIN tasks AS task ON _id = cd_task
WHERE cd_calendar = :calendar
  AND parent = :parent
  AND cd_deleted = 0
  AND deleted = 0
  AND primary_sort >= :from
  AND primary_sort < IFNULL(:to, ${Long.MAX_VALUE})
ORDER BY primary_sort
    """)
    internal abstract suspend fun getTasksToShift(calendar: String, parent: Long, from: Long, to: Long?): List<CaldavTaskContainer>

    @Query("UPDATE caldav_lists SET cdl_order = $NO_ORDER")
    abstract suspend fun resetOrders()

    @Query("UPDATE caldav_lists SET cdl_order = :order WHERE cdl_id = :id")
    abstract suspend fun setOrder(id: Long, order: Int)

    @Query("UPDATE tasks SET `order` = :order WHERE _id = :id")
    abstract suspend fun setTaskOrder(id: Long, order: Long?)

    suspend fun setupLocalAccount(context: Context): CaldavAccount = mutex.withLock {
        val account = getLocalAccount()
        getLocalList(context, account)
        return account
    }

    suspend fun getLocalList(context: Context) = mutex.withLock {
        getLocalList(context, getLocalAccount())
    }

    private suspend fun getLocalAccount() = getAccountByUuid(LOCAL) ?: CaldavAccount().apply {
        accountType = TYPE_LOCAL
        uuid = LOCAL
        id = insert(this)
    }

    private suspend fun getLocalList(context: Context, account: CaldavAccount): CaldavCalendar =
            getCalendarsByAccount(account.uuid!!).getOrNull(0)
                    ?: CaldavCalendar(
                        name = context.getString(R.string.default_list),
                        uuid = UUIDHelper.newUUID(),
                        account = account.uuid,
                    ).apply {
                        insert(this)
                    }

    companion object {
        const val LOCAL = "local"
        private val mutex = Mutex()
    }
}