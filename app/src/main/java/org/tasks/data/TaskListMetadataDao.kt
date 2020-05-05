package org.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
abstract class TaskListMetadataDao {
    @Query("SELECT * from task_list_metadata where tag_uuid = :tagUuid OR filter = :tagUuid LIMIT 1")
    abstract fun fetchByTagOrFilter(tagUuid: String): TaskListMetadata?

    @Update
    abstract fun update(taskListMetadata: TaskListMetadata)

    @Insert
    abstract fun insert(taskListMetadata: TaskListMetadata): Long

    fun createNew(taskListMetadata: TaskListMetadata) {
        taskListMetadata.id = insert(taskListMetadata)
    }
}