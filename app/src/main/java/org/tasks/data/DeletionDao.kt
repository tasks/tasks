package org.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import org.tasks.data.CaldavDao.Companion.LOCAL
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.db.SuspendDbUtils.eachChunk

@Dao
abstract class DeletionDao {
    @Query("DELETE FROM caldav_tasks WHERE cd_task IN(:ids)")
    internal abstract suspend fun deleteCaldavTasks(ids: List<Long>)

    @Query("DELETE FROM google_tasks WHERE gt_task IN(:ids)")
    internal abstract suspend fun deleteGoogleTasks(ids: List<Long>)

    @Query("DELETE FROM tags WHERE task IN(:ids)")
    internal abstract suspend fun deleteTags(ids: List<Long>)

    @Query("DELETE FROM geofences WHERE task IN(:ids)")
    internal abstract suspend fun deleteGeofences(ids: List<Long>)

    @Query("DELETE FROM alarms WHERE task IN(:ids)")
    internal abstract suspend fun deleteAlarms(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE _id IN(:ids)")
    internal abstract suspend fun deleteTasks(ids: List<Long>)

    suspend fun hasRecurringAncestors(ids: List<Long>): List<Long> =
            ids.chunkedMap { internalHasRecurringAncestors(it) }

    @Query("""
WITH RECURSIVE recursive_tasks (descendent, parent, recurring) AS (
    SELECT _id, parent, 0
    FROM tasks
    WHERE _id IN (:ids)
      AND parent > 0
    UNION ALL
    SELECT recursive_tasks.descendent,
           tasks.parent,
           CASE
               WHEN recursive_tasks.recurring THEN 1
               WHEN recurrence IS NOT NULL AND recurrence != '' AND completed = 0 THEN 1
               ELSE 0
               END
    FROM tasks
             INNER JOIN recursive_tasks ON recursive_tasks.parent = _id
)
SELECT DISTINCT(descendent)
FROM recursive_tasks
WHERE recurring = 1
    """)
    abstract suspend fun internalHasRecurringAncestors(ids: List<Long>): List<Long>

    @Transaction
    open suspend fun delete(ids: List<Long>) {
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
    internal abstract suspend fun markDeletedInternal(ids: List<Long>)

    suspend fun markDeleted(ids: Iterable<Long>) {
        ids.eachChunk(this::markDeletedInternal)
    }

    @Query("SELECT gt_task FROM google_tasks WHERE gt_deleted = 0 AND gt_list_id = :listId")
    internal abstract suspend fun getActiveGoogleTasks(listId: String): List<Long>

    @Delete
    internal abstract suspend fun deleteGoogleTaskList(googleTaskList: GoogleTaskList)

    @Transaction
    open suspend fun delete(googleTaskList: GoogleTaskList): List<Long> {
        val tasks = getActiveGoogleTasks(googleTaskList.remoteId!!)
        delete(tasks)
        deleteGoogleTaskList(googleTaskList)
        return tasks
    }

    @Delete
    internal abstract suspend fun deleteGoogleTaskAccount(googleTaskAccount: GoogleTaskAccount)

    @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
    abstract suspend fun getLists(account: String): List<GoogleTaskList>

    @Transaction
    open suspend fun delete(googleTaskAccount: GoogleTaskAccount): List<Long> {
        val deleted = ArrayList<Long>()
        for (list in getLists(googleTaskAccount.account!!)) {
            deleted.addAll(delete(list))
        }
        deleteGoogleTaskAccount(googleTaskAccount)
        return deleted
    }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0")
    internal abstract suspend fun getActiveCaldavTasks(calendar: String): List<Long>

    @Delete
    internal abstract suspend fun deleteCaldavCalendar(caldavCalendar: CaldavCalendar)

    @Transaction
    open suspend fun delete(caldavCalendar: CaldavCalendar): List<Long> {
        val tasks = getActiveCaldavTasks(caldavCalendar.uuid!!)
        delete(tasks)
        deleteCaldavCalendar(caldavCalendar)
        return tasks
    }

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account")
    abstract suspend fun getCalendars(account: String): List<CaldavCalendar>

    @Delete
    internal abstract suspend fun deleteCaldavAccount(caldavAccount: CaldavAccount)

    @Query("DELETE FROM tasks WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task INNER JOIN caldav_lists ON cdl_uuid = cd_calendar WHERE cdl_account = '$LOCAL' AND deleted > 0 AND cd_deleted = 0)")
    abstract suspend fun purgeDeleted()

    @Transaction
    open suspend fun delete(caldavAccount: CaldavAccount): List<Long> {
        val deleted = ArrayList<Long>()
        for (calendar in getCalendars(caldavAccount.uuid!!)) {
            deleted.addAll(delete(calendar))
        }
        deleteCaldavAccount(caldavAccount)
        return deleted
    }
}