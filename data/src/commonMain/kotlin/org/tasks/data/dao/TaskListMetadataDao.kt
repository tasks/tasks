package org.tasks.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import org.tasks.data.entity.TaskListMetadata

@Dao
abstract class TaskListMetadataDao {
    @Query("SELECT * from task_list_metadata where tag_uuid = :tagUuid OR filter = :tagUuid LIMIT 1")
    abstract suspend fun fetchByTagOrFilter(tagUuid: String): TaskListMetadata?

    @Query("SELECT * FROM task_list_metadata")
    abstract suspend fun getAll(): List<TaskListMetadata>

    /**
     * Every task list whose default view is pinned to the given filter id (stored as the
     * filter's _id as a string). Used by delete_filter to warn that deleting the filter
     * makes these lists silently fall back to their default view.
     */
    @Query("SELECT * FROM task_list_metadata WHERE filter = :filterId")
    abstract suspend fun findPinnedTo(filterId: String): List<TaskListMetadata>

    @Update
    abstract suspend fun update(taskListMetadata: TaskListMetadata)

    @Insert
    abstract suspend fun insert(taskListMetadata: TaskListMetadata): Long

    suspend fun createNew(taskListMetadata: TaskListMetadata) {
        taskListMetadata.id = insert(taskListMetadata)
    }
}