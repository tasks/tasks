package org.tasks.data.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.data.UUIDHelper

@Serializable
@Parcelize
@Entity(tableName = "attachment_file")
data class TaskAttachment(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "file_id")
    @Transient
    val id: Long? = null,
    @ColumnInfo(name = "file_uuid")
    val remoteId: String = UUIDHelper.newUUID(),
    @ColumnInfo(name = "filename")
    val name: String,
    @ColumnInfo(name = "uri")
    val uri: String,
) : Parcelable {
    companion object {
        const val KEY = "attachment"
        const val FILES_DIRECTORY_DEFAULT = "attachments"
    }
}