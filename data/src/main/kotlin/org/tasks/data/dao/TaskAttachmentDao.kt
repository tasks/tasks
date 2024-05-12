package org.tasks.data.dao

import androidx.room.*
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.Attachment

@Dao
interface TaskAttachmentDao {
    @Query("SELECT * FROM attachment WHERE task = :task")
    suspend fun getAttachmentsForTask(task: Long): List<Attachment>

    @Query("SELECT attachment_file.* FROM attachment_file INNER JOIN attachment ON attachment_file.file_uuid = attachment.file_uuid WHERE task = :task")
    suspend fun getAttachments(task: Long): List<TaskAttachment>

    @Query("SELECT * FROM attachment_file")
    suspend fun getAttachments(): List<TaskAttachment>

    @Query("SELECT * FROM attachment_file WHERE file_uuid = :remoteId")
    suspend fun getAttachment(remoteId: String): TaskAttachment?

    @Query("DELETE FROM attachment WHERE task = :taskId AND file_uuid = :attachment")
    suspend fun delete(taskId: Long, attachment: String)

    @Query("DELETE FROM attachment WHERE task = :taskId AND file_uuid IN (:attachments)")
    suspend fun delete(taskId: Long, attachments: List<String>)

    @Insert
    suspend fun insert(attachments: List<Attachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: TaskAttachment)

    @Update
    suspend fun update(attachment: TaskAttachment)

    @Delete
    fun delete(value: List<TaskAttachment>)
}