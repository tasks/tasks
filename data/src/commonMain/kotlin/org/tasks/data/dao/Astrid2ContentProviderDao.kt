package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Query
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task

@Dao
interface Astrid2ContentProviderDao {
    @Query("SELECT name FROM tags WHERE task = :taskId ORDER BY UPPER(name) ASC")
    suspend fun getTagNames(taskId: Long): List<String>

    @Query("""
        SELECT *
        FROM tasks
        WHERE completed = 0
          AND deleted = 0
          AND hideUntil < (strftime('%s', 'now') * 1000)
        ORDER BY (CASE
                      WHEN (dueDate = 0) THEN
                          (strftime('%s', 'now') * 1000) * 2
                      ELSE ((CASE WHEN (dueDate / 1000) % 60 > 0 THEN dueDate ELSE (dueDate + 43140000) END)) END) +
                 172800000 * importance
            ASC
        LIMIT 100""")
    suspend fun getAstrid2TaskProviderTasks(): List<Task>

    @Query("SELECT * FROM tagdata WHERE name IS NOT NULL AND name != '' ORDER BY UPPER(name) ASC")
    suspend fun tagDataOrderedByName(): List<TagData>
}
