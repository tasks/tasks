package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.*
import io.reactivex.Single
import org.tasks.db.DbUtils
import org.tasks.filters.CaldavFilters

@Dao
abstract class CaldavDao {
    @Query("SELECT * FROM caldav_lists")
    abstract fun subscribeToCalendars(): LiveData<List<CaldavCalendar>>

    @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
    abstract fun getCalendarByUuid(uuid: String): CaldavCalendar?

    @Query("SELECT * FROM caldav_accounts WHERE cda_uuid = :uuid LIMIT 1")
    abstract fun getAccountByUuid(uuid: String): CaldavAccount?

    @Query("SELECT COUNT(*) FROM caldav_accounts")
    abstract fun accountCount(): Single<Int>

    @Query("SELECT * FROM caldav_accounts ORDER BY UPPER(cda_name) ASC")
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

    @Insert
    abstract fun insert(caldavTask: CaldavTask): Long

    @Insert
    abstract fun insert(tasks: Iterable<CaldavTask>)

    @Update
    abstract fun update(caldavTask: CaldavTask)

    fun update(caldavTask: SubsetCaldav) {
        update(caldavTask.id, caldavTask.remoteParent)
    }

    @Query("UPDATE caldav_tasks SET cd_remote_parent = :remoteParent WHERE cd_id = :id")
    abstract fun update(id: Long, remoteParent: String)

    @Update
    abstract fun update(tasks: Iterable<CaldavTask>)

    @Delete
    abstract fun delete(caldavTask: CaldavTask)

    @Query("SELECT * FROM caldav_tasks WHERE cd_deleted > 0 AND cd_calendar = :calendar")
    abstract fun getDeleted(calendar: String): List<CaldavTask>

    @Query("UPDATE caldav_tasks SET cd_deleted = :now WHERE cd_task IN (:tasks)")
    abstract fun markDeleted(now: Long, tasks: List<Long>)

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

    fun getTasks(calendar: String, objects: List<String>): List<Long> {
        return DbUtils.collect(objects) { getTasksInternal(calendar, it!!) }
    }

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
    abstract fun getCaldavFilters(uuid: String, now: Long): List<CaldavFilters>

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
}