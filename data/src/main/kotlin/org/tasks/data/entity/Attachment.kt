package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskAttachment::class,
            parentColumns = ["file_id"],
            childColumns = ["file"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["task", "file"], unique = true)],
)
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attachment_id")
    @Transient
    val id: Long? = null,
    @ColumnInfo(name = "task", index = true)
    @Transient
    val task: Long,
    @ColumnInfo(name = "file", index = true)
    @Transient
    val fileId: Long,
    @ColumnInfo(name = "file_uuid")
    val attachmentUid: String,
)