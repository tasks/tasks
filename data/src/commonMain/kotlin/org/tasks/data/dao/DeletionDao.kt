package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import org.tasks.data.dao.CaldavDao.Companion.LOCAL
import org.tasks.data.db.Database
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.withTransaction

@Dao
abstract class DeletionDao(private val database: Database) {
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

    suspend fun delete(ids: List<Long>) { ids.eachChunk { deleteTasks(it) } }

    @Query("UPDATE tasks "
            + "SET modified = (strftime('%s','now')*1000), deleted = (strftime('%s','now')*1000)"
            + "WHERE _id IN(:ids)")
    internal abstract suspend fun markDeletedInternal(ids: List<Long>)

    suspend fun markDeleted(ids: Iterable<Long>) {
        ids.eachChunk(this::markDeletedInternal)
    }

    @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0")
    internal abstract suspend fun getActiveCaldavTasks(calendar: String): List<Long>

    @Delete
    internal abstract suspend fun deleteCaldavCalendar(caldavCalendar: CaldavCalendar)

    suspend fun delete(caldavCalendar: CaldavCalendar): List<Long> =
        database.withTransaction {
            val tasks = getActiveCaldavTasks(caldavCalendar.uuid!!)
            delete(tasks)
            deleteCaldavCalendar(caldavCalendar)
            tasks
        }

    @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account")
    abstract suspend fun getCalendars(account: String): List<CaldavCalendar>

    @Delete
    internal abstract suspend fun deleteCaldavAccount(caldavAccount: CaldavAccount)

    @Query("DELETE FROM tasks WHERE _id IN (SELECT _id FROM tasks INNER JOIN caldav_tasks ON _id = cd_task INNER JOIN caldav_lists ON cdl_uuid = cd_calendar WHERE cdl_account = '$LOCAL' AND deleted > 0 AND cd_deleted = 0)")
    abstract suspend fun purgeDeleted()

    suspend fun delete(caldavAccount: CaldavAccount): List<Long> =
        database.withTransaction {
            val deleted = ArrayList<Long>()
            for (calendar in getCalendars(caldavAccount.uuid!!)) {
                deleted.addAll(delete(calendar))
            }
            deleteCaldavAccount(caldavAccount)
            deleted
        }
}