package org.tasks.data

import androidx.room.*
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper

@Dao
abstract class TaskAttachmentDao {
    @Query("SELECT * FROM task_attachments WHERE task_id = :taskUuid")
    abstract fun getAttachments(taskUuid: String): List<TaskAttachment>

    @Query("SELECT task_attachments.* FROM task_attachments INNER JOIN tasks ON tasks._id = :task WHERE task_id = tasks.remoteId")
    abstract fun getAttachments(task: Long): List<TaskAttachment>

    @Query("SELECT * FROM task_attachments")
    abstract fun getAttachments(): List<TaskAttachment>

    @Delete
    abstract fun delete(taskAttachment: TaskAttachment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(attachment: TaskAttachment)

    @Update
    abstract fun update(attachment: TaskAttachment)

    fun createNew(attachment: TaskAttachment) {
        if (Task.isUuidEmpty(attachment.remoteId)) {
            attachment.remoteId = UUIDHelper.newUUID()
        }
        insert(attachment)
    }
}