package org.tasks.data

import androidx.room.*
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskAttachmentDao {
    @Query("SELECT * FROM task_attachments WHERE task_id = :taskUuid")
    abstract suspend fun getAttachments(taskUuid: String): List<TaskAttachment>

    @Query("SELECT task_attachments.* FROM task_attachments INNER JOIN tasks ON tasks._id = :task WHERE task_id = tasks.remoteId")
    abstract suspend fun getAttachments(task: Long): List<TaskAttachment>

    @Query("SELECT * FROM task_attachments")
    abstract suspend fun getAttachments(): List<TaskAttachment>

    @Query("SELECT * FROM task_attachments WHERE task_id = :taskUuid")
    abstract fun watchAttachments(taskUuid: String): Flow<List<TaskAttachment>>

    @Delete
    abstract suspend fun delete(taskAttachment: TaskAttachment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(attachment: TaskAttachment)

    @Update
    abstract suspend fun update(attachment: TaskAttachment)

    suspend fun createNew(attachment: TaskAttachment) {
        if (Task.isUuidEmpty(attachment.remoteId)) {
            attachment.remoteId = UUIDHelper.newUUID()
        }
        insert(attachment)
    }
}