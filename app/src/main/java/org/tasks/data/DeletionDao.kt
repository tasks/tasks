package org.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import org.tasks.data.CaldavDao.Companion.LOCAL
import org.tasks.db.DbUtils.eachChunk
import java.util.*

@Dao
abstract class DeletionDao {
    @Query("DELETE FROM caldav_tasks WHERE cd_task IN(:ids)")
    abstract fun deleteCaldavTasks(ids: List<Long>)

    @Query("DELETE FROM google_tasks WHERE gt_task IN(:ids)")
    abstract fun deleteGoogleTasks(ids: List<Long>)

    @Query("DELETE FROM tags WHERE task IN(:ids)")
    abstract fun deleteTags(ids: List<Long>)

    @Query("DELETE FROM geofences WHERE task IN(:ids)")
    abstract fun deleteGeofences(ids: List<Long>)

    @Query("DELETE FROM alarms WHERE task IN(:ids)")
    abstract fun deleteAlarms(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE _id IN(:ids)")
    abstract fun deleteTasks(ids: List<Long>)

    @Transaction
    open fun delete(ids: List<Long>) {
        ids.eachChunk {
            deleteAlarms(it)
            deleteGeofences(it)
            deleteTags(it)
            deleteGoogleTasks(it)
            deleteCaldavTasks(it)
            deleteTasks(it)
        }
    }

    @Query("UPDATE tasks "
            + "SET modified = (strftime('%s','now')*1000), deleted = (strftime('%s','now')*1000)"
            + "WHERE _id IN(:ids)")
    abstract fun markDeletedInternal(ids: List<Long>)

    fun markDeleted(ids: Iterable<Long>) {
        ids.eachChunk(this::markDeletedInternal)
    }

    @Query("SELECT gt_task FROM google_tasks WHERE gt_deleted = 0 AND gt_list_id = :listId")
    abstract fun getActiveGoogleTasks(listId: String): List<Long>

    @Delete
    abstract fun deleteGoogleTaskList(googleTaskList: GoogleTaskList)

    @Transaction
    open fun delete(googleTaskList: GoogleTaskList): List<Long> {
        val tasks = getActiveGoogleTasks(googleTaskList.remoteId!!)
        delete(tasks)
        deleteGoogleTaskList(googleTaskList)
        return tasks
    }

    @Delete
    abstract fun deleteGoogleTaskAccount(googleTaskAccount: GoogleTaskAccount)

    @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
    abstract fun getLists(account: String): List<GoogleTaskList>

    @Transaction
    open fun delete(googleTaskAccount: GoogleTaskAccount): List<Long> {
        val deleted = ArrayList<Long>()
        for (list in getLists(googleTaskAccount.account!!)) {
            deleted.addAll(delete(list))
        }
        deleteGoogleTaskAccount(googleTaskAccount)
        return deleted
    }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0")
    abstract fun getActiveCaldavTasks(calendar: String): List<Long>

    @Delete
    abstract fun deleteCaldavCalendar(caldavCalendar: CaldavCalendar)

    @Transaction
    open fun delete(caldavCalendar: CaldavCalendar): List<Long> {
        val tasks = getActiveCaldavTasks(caldavCalendar.uuid!!)
        delete(tasks)
        deleteCaldavCalendar(caldavCalendar)
        return tasks
    }

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account")
    abstract fun getCalendars(account: String): List<CaldavCalendar>

    @Delete
    abstract fun deleteCaldavAccount(caldavAccount: CaldavAccount)

    @Query("DELETE FROM tasks WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task INNER JOIN caldav_lists ON cdl_uuid = cd_calendar WHERE cdl_account = '$LOCAL' AND deleted > 0)")
    abstract fun purgeDeleted()

    @Transaction
    open fun delete(caldavAccount: CaldavAccount): List<Long> {
        val deleted = ArrayList<Long>()
        for (calendar in getCalendars(caldavAccount.uuid!!)) {
            deleted.addAll(delete(calendar))
        }
        deleteCaldavAccount(caldavAccount)
        return deleted
    }
}