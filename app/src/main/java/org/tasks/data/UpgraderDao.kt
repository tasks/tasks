package org.tasks.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UpgraderDao {
    @Query("""
SELECT task.*, caldav_task.*
FROM tasks AS task
         INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task
WHERE cd_deleted = 0
  AND cd_vtodo IS NOT NULL
  AND cd_vtodo != ''
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

    @Query("UPDATE tasks SET hideUntil = :startDate WHERE _id = :task")
    fun setStartDate(task: Long, startDate: Long)
}