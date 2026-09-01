package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import org.tasks.data.CaldavTaskContainer
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
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
}
