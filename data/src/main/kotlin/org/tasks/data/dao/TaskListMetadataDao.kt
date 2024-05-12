package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.tasks.data.entity.TaskListMetadata

@Dao
abstract class TaskListMetadataDao {
    @Query("SELECT * from task_list_metadata where tag_uuid = :tagUuid OR filter = :tagUuid LIMIT 1")
    abstract suspend fun fetchByTagOrFilter(tagUuid: String): TaskListMetadata?

    @Query("SELECT * FROM task_list_metadata")
    abstract suspend fun getAll(): List<TaskListMetadata>

    @Update
    abstract suspend fun update(taskListMetadata: TaskListMetadata)

    @Insert
    abstract suspend fun insert(taskListMetadata: TaskListMetadata): Long

    suspend fun createNew(taskListMetadata: TaskListMetadata) {
        taskListMetadata.id = insert(taskListMetadata)
    }
}