package org.tasks.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.core.SortHelper.APPLE_EPOCH
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import org.tasks.R
import org.tasks.date.DateTimeUtils.toAppleEpoch
import org.tasks.db.DbUtils.chunkedMap
import org.tasks.filters.CaldavFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
abstract class CaldavDao {
    @Query("SELECT * FROM caldav_lists")
    abstract fun subscribeToCalendars(): LiveData<List<CaldavCalendar>>

    @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
    abstract fun getCalendarByUuid(uuid: String): CaldavCalendar?

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :uuid")
    abstract fun getCalendarsByAccount(uuid: String): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_accounts WHERE cda_uuid = :uuid LIMIT 1")
    abstract fun getAccountByUuid(uuid: String): CaldavAccount?

    @Query("SELECT COUNT(*) FROM caldav_accounts WHERE cda_account_type != 2")
    abstract fun accountCount(): Int

    @Query("SELECT * FROM caldav_accounts ORDER BY cda_account_type, UPPER(cda_name)")
    abstract fun getAccounts(): List<CaldavAccount>

    @Query("UPDATE caldav_accounts SET cda_collapsed = :collapsed WHERE cda_id = :id")
    abstract fun setCollapsed(id: Long, collapsed: Boolean)

    @Insert
    abstract fun insert(caldavAccount: CaldavAccount): Long

    @Update
    abstract fun update(caldavAccount: CaldavAccount)

    fun insert(caldavCalendar: CaldavCalendar) {
        caldavCalendar.id = insertInternal(caldavCalendar)
    }

    @Insert
    abstract fun insertInternal(caldavCalendar: CaldavCalendar): Long

    @Update
    abstract fun update(caldavCalendar: CaldavCalendar)

    @Transaction
    open fun insert(task: Task, caldavTask: CaldavTask, addToTop: Boolean): Long {
        if (caldavTask.order != null) {
            return insert(caldavTask)
        }
        if (addToTop) {
            caldavTask.order = findFirstTask(caldavTask.calendar!!, task.parent)
                    ?.takeIf { task.creationDate.toAppleEpoch() >= it }
                    ?.minus(1)
        } else {
            caldavTask.order = findLastTask(caldavTask.calendar!!, task.parent)
                    ?.takeIf { task.creationDate.toAppleEpoch() <= it }
                    ?.plus(1)
        }
        return insert(caldavTask)
    }

    @Query("SELECT MIN(IFNULL(cd_order, (created - $APPLE_EPOCH) / 1000)) FROM caldav_tasks INNER JOIN tasks ON _id = cd_task WHERE cd_calendar = :calendar AND cd_deleted = 0 AND deleted = 0 AND parent = :parent")
    internal abstract fun findFirstTask(calendar: String, parent: Long): Long?

    @Query("SELECT MAX(IFNULL(cd_order, (created - $APPLE_EPOCH) / 1000)) FROM caldav_tasks INNER JOIN tasks ON _id = cd_task WHERE cd_calendar = :calendar AND cd_deleted = 0 AND deleted = 0 AND parent = :parent")
    internal abstract fun findLastTask(calendar: String, parent: Long): Long?

    @Insert
    abstract fun insert(caldavTask: CaldavTask): Long

    @Insert
    abstract fun insert(tasks: Iterable<CaldavTask>)

    @Update
    abstract fun update(caldavTask: CaldavTask)

    fun update(caldavTask: SubsetCaldav) {
        update(caldavTask.cd_id, caldavTask.cd_order, caldavTask.cd_remote_parent)
    }

    @Query("UPDATE caldav_tasks SET cd_order = :position, cd_remote_parent = :parent WHERE cd_id = :id")
    internal abstract fun update(id: Long, position: Long?, parent: String?)

    @Query("UPDATE caldav_tasks SET cd_order = :position WHERE cd_id = :id")
    internal abstract fun update(id: Long, position: Long?)

    @Query("UPDATE caldav_tasks SET cd_remote_parent = :remoteParent WHERE cd_id = :id")
    internal abstract fun update(id: Long, remoteParent: String?)

    @Update
    abstract fun update(tasks: Iterable<CaldavTask>)

    @Delete
    abstract fun delete(caldavTask: CaldavTask)

    @Query("SELECT * FROM caldav_tasks WHERE cd_deleted > 0 AND cd_calendar = :calendar")
    abstract fun getDeleted(calendar: String): List<CaldavTask>

    @Query("UPDATE caldav_tasks SET cd_deleted = :now WHERE cd_task IN (:tasks)")
    abstract fun markDeleted(tasks: List<Long>, now: Long = currentTimeMillis())

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
    abstract fun getTask(taskId: Long): CaldavTask?

    @Query("SELECT cd_remote_id FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0")
    abstract fun getRemoteIdForTask(taskId: Long): String?

    @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object = :obj LIMIT 1")
    abstract fun getTask(calendar: String, obj: String): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_remote_id = :remoteId")
    abstract fun getTaskByRemoteId(calendar: String, remoteId: String): CaldavTask?

    @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId")
    abstract fun getTasks(taskId: Long): List<CaldavTask>

    @Query("SELECT * FROM caldav_tasks WHERE cd_task in (:taskIds) AND cd_deleted = 0")
    abstract fun getTasks(taskIds: List<Long>): List<CaldavTask>

    @Query("SELECT task.*, caldav_task.* FROM tasks AS task "
            + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
            + "WHERE cd_deleted = 0 AND cd_vtodo IS NOT NULL AND cd_vtodo != ''")
    abstract fun getTasks(): List<CaldavTaskContainer>

    @Query("SELECT task.*, caldav_task.* FROM tasks AS task "
            + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
            + "WHERE cd_calendar = :calendar "
            + "AND modified > cd_last_sync "
            + "AND cd_deleted = 0")
    abstract fun getCaldavTasksToPush(calendar: String): List<CaldavTaskContainer>

    @Query("SELECT * FROM caldav_lists ORDER BY cdl_name COLLATE NOCASE")
    abstract fun getCalendars(): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
    abstract fun getCalendar(uuid: String): CaldavCalendar?

    @Query("SELECT cd_object FROM caldav_tasks WHERE cd_calendar = :calendar")
    abstract fun getObjects(calendar: String): List<String>

    fun getTasks(calendar: String, objects: List<String>): List<Long> =
            objects.chunkedMap { getTasksInternal(calendar, it) }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object IN (:objects)")
    abstract fun getTasksInternal(calendar: String, objects: List<String>): List<Long>

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url NOT IN (:urls)")
    abstract fun findDeletedCalendars(account: String, urls: List<String>): List<CaldavCalendar>

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url = :url LIMIT 1")
    abstract fun getCalendarByUrl(account: String, url: String): CaldavCalendar?

    @Query("SELECT caldav_accounts.* from caldav_accounts"
            + " INNER JOIN caldav_tasks ON cd_task = :task"
            + " INNER JOIN caldav_lists ON cd_calendar = cdl_uuid"
            + " WHERE cdl_account = cda_uuid")
    abstract fun getAccountForTask(task: Long): CaldavAccount?

    @Query("SELECT DISTINCT cd_calendar FROM caldav_tasks WHERE cd_deleted = 0 AND cd_task IN (:tasks)")
    abstract fun getCalendars(tasks: List<Long>): List<String>

    @Query("SELECT caldav_lists.*, COUNT(tasks._id) AS count"
            + " FROM caldav_lists"
            + " LEFT JOIN caldav_tasks ON caldav_tasks.cd_calendar = caldav_lists.cdl_uuid"
            + " LEFT JOIN tasks ON caldav_tasks.cd_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now AND cd_deleted = 0"
            + " WHERE caldav_lists.cdl_account = :uuid"
            + " GROUP BY caldav_lists.cdl_uuid")
    abstract fun getCaldavFilters(uuid: String, now: Long = currentTimeMillis()): List<CaldavFilters>

    @Query("SELECT tasks._id FROM tasks "
            + "INNER JOIN tags ON tags.task = tasks._id "
            + "INNER JOIN caldav_tasks ON cd_task = tasks._id "
            + "GROUP BY tasks._id")
    abstract fun getTasksWithTags(): List<Long>

    @Query("UPDATE tasks SET parent = IFNULL(("
            + " SELECT p.cd_task FROM caldav_tasks AS p"
            + "  INNER JOIN caldav_tasks ON caldav_tasks.cd_task = tasks._id"
            + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
            + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
            + "    AND p.cd_deleted = 0), 0)"
            + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0)")
    abstract fun updateParents()

    @Query("UPDATE tasks SET parent = IFNULL(("
            + " SELECT p.cd_task FROM caldav_tasks AS p"
            + "  INNER JOIN caldav_tasks "
            + "    ON caldav_tasks.cd_task = tasks._id"
            + "    AND caldav_tasks.cd_calendar = :calendar"
            + "  WHERE p.cd_remote_id = caldav_tasks.cd_remote_parent"
            + "    AND p.cd_calendar = caldav_tasks.cd_calendar"
            + "    AND caldav_tasks.cd_deleted = 0), 0)"
            + "WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task WHERE cd_deleted = 0 AND cd_calendar = :calendar)")
    abstract fun updateParents(calendar: String)

    @Transaction
    open fun move(task: TaskContainer, newParent: Long, newPosition: Long?) {
        val previousParent = task.parent
        val caldavTask = task.caldavTask
        val previousPosition = task.caldavSortOrder
        if (newPosition != null) {
            if (newParent == previousParent && newPosition < previousPosition) {
                shiftDown(task.caldav!!, newParent, newPosition, previousPosition)
            } else {
                shiftDown(task.caldav!!, newParent, newPosition)
            }
        }
        caldavTask.cd_order = newPosition
        update(caldavTask.cd_id, caldavTask.cd_order)
    }

    @Transaction
    open fun shiftDown(calendar: String, parent: Long, from: Long, to: Long? = null) {
        val updated = ArrayList<CaldavTask>()
        val tasks = getTasksToShift(calendar, parent, from, to)
        for (i in tasks.indices) {
            val task = tasks[i]
            val current = from + i
            if (task.sortOrder == current) {
                val caldavTask = task.caldavTask
                caldavTask.order = current + 1
                updated.add(caldavTask)
            } else if (task.sortOrder > current) {
                break
            }
        }
        update(updated)
        touchInternal(updated.map(CaldavTask::task))
    }

    @Query("UPDATE tasks SET modified = :modificationTime WHERE _id in (:ids)")
    internal abstract fun touchInternal(ids: List<Long>, modificationTime: Long = now())

    @Query("SELECT task.*, caldav_task.*, IFNULL(cd_order, (created - $APPLE_EPOCH) / 1000) AS primary_sort FROM caldav_tasks AS caldav_task INNER JOIN tasks AS task ON _id = cd_task WHERE cd_calendar = :calendar AND parent = :parent AND cd_deleted = 0 AND deleted = 0 AND primary_sort >= :from AND primary_sort < IFNULL(:to, ${Long.MAX_VALUE}) ORDER BY primary_sort")
    internal abstract fun getTasksToShift(calendar: String, parent: Long, from: Long, to: Long?): List<CaldavTaskContainer>

    @Query("UPDATE caldav_lists SET cdl_order = $NO_ORDER")
    abstract fun resetOrders()

    @Query("UPDATE caldav_lists SET cdl_order = :order WHERE cdl_id = :id")
    abstract fun setOrder(id: Long, order: Int)

    fun setupLocalAccount(context: Context): CaldavAccount {
        val account = getLocalAccount()
        getLocalList(context, account)
        return account
    }

    fun getLocalList(context: Context) = getLocalList(context, getLocalAccount())

    private fun getLocalAccount() = getAccountByUuid(LOCAL) ?: CaldavAccount().apply {
        accountType = CaldavAccount.TYPE_LOCAL
        uuid = LOCAL
        id = insert(this)
    }

    private fun getLocalList(context: Context, account: CaldavAccount): CaldavCalendar =
            getCalendarsByAccount(account.uuid!!).getOrNull(0)
                    ?: CaldavCalendar(context.getString(R.string.default_list), UUIDHelper.newUUID()).apply {
                        this.account = account.uuid
                        insert(this)
                    }

    companion object {
        const val LOCAL = "local"
    }
}