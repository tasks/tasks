package org.tasks.data.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import org.tasks.data.entity.Task
import org.tasks.data.entity.TagData

@Dao
interface ContentProviderDao {
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

    @Query("SELECT * FROM tasks")
    fun getTasks(): Cursor

    @Query("""
        SELECT caldav_lists.*, caldav_accounts.cda_name
        FROM caldav_lists
          INNER JOIN caldav_accounts ON cdl_account = cda_uuid""")
    fun getLists(): Cursor

    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): Cursor
}