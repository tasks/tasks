package org.tasks.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import org.tasks.data.CaldavTaskContainer
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.Task

@Dao
interface UpgraderDao {
    @Query("""
SELECT task.*, caldav_task.*
FROM tasks AS task
         INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task
WHERE cd_deleted = 0
    """)
    suspend fun tasksWithVtodos(): List<CaldavTaskContainer>

    @Query("""
SELECT task.*, caldav_task.*
FROM tasks AS task
         INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task
         INNER JOIN caldav_lists ON cdl_uuid = cd_calendar
         INNER JOIN caldav_accounts ON cda_uuid = cdl_account
WHERE cda_account_type IN ($TYPE_CALDAV, $TYPE_TASKS, $TYPE_ETEBASE)
    """)
    suspend fun tasksWithCachedVtodos(): List<CaldavTaskContainer>

    @Query("""
SELECT tasks._id
FROM tasks
         INNER JOIN tags ON tags.task = tasks._id
         INNER JOIN caldav_tasks ON cd_task = tasks._id
GROUP BY tasks._id
    """)
    suspend fun tasksWithTags(): List<Long>

    @Query("""
SELECT task.*, caldav_task.*
FROM tasks AS task
         INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task
         INNER JOIN caldav_lists ON cd_calendar = cdl_uuid
WHERE cd_deleted = 0
  AND cdl_account = :account AND cdl_url = :url
    """)
    suspend fun getOpenTasksForList(account: String, url: String): List<CaldavTaskContainer>

    @Query("UPDATE tasks SET hideUntil = :startDate WHERE _id = :task")
    suspend fun setStartDate(task: Long, startDate: Long)

    @Query("""
DELETE FROM alarms
WHERE task IN (SELECT _id FROM tasks WHERE dueDate > 0 AND dueDate % 60000 = 0)
    """)
    suspend fun deleteAlarmsForAllDayTasks()

    @Query("""
SELECT * FROM tasks
WHERE deleted = 0
  AND completed = 0
  AND repeat_from != ${Task.RepeatFrom.COMPLETION_DATE}
  AND dueDate > 0
  AND recurrence LIKE '%FREQ=MONTHLY%'
  AND NOT EXISTS (
      SELECT 1 FROM caldav_tasks
               INNER JOIN caldav_lists ON cdl_uuid = cd_calendar
               INNER JOIN caldav_accounts ON cda_uuid = cdl_account
      WHERE cd_task = tasks._id
        AND cd_deleted = 0
        AND cda_account_type = $TYPE_MICROSOFT
  )
    """)
    suspend fun monthlyRecurringTasksWithDueDate(): List<Task>

    @Transaction
    suspend fun setRecurrence(recurrences: List<Pair<Long, String>>, now: Long) =
        recurrences.forEach { (id, recurrence) -> setRecurrence(id, recurrence, now) }

    @Query("UPDATE tasks SET recurrence = :recurrence, modified = :now WHERE _id = :id")
    suspend fun setRecurrence(id: Long, recurrence: String, now: Long)

    @Query("UPDATE tasks SET status = :status WHERE _id IN (:ids)")
    suspend fun setStatus(ids: List<Long>, status: Int)

    @Query("""
UPDATE tasks SET status = :status
WHERE _id IN (
    SELECT cd_task FROM caldav_tasks
    WHERE cd_calendar = :calendar AND cd_remote_id IN (:remoteIds)
)
    """)
    suspend fun setStatus(calendar: String, remoteIds: List<String>, status: Int)

    @Query("""
UPDATE caldav_lists SET cdl_ctag = NULL
WHERE cdl_account IN (SELECT cda_uuid FROM caldav_accounts WHERE cda_account_type = $TYPE_OPENTASKS)
    """)
    suspend fun clearOpenTasksCtags()

    @Query("""
UPDATE caldav_tasks SET cd_etag = NULL
WHERE cd_calendar IN (
    SELECT cdl_uuid FROM caldav_lists
             INNER JOIN caldav_accounts ON cda_uuid = cdl_account
    WHERE cda_account_type = $TYPE_OPENTASKS
)
    """)
    suspend fun clearOpenTasksEtags()
}
