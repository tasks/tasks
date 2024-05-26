package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Query
import org.tasks.data.CaldavTaskContainer

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
}