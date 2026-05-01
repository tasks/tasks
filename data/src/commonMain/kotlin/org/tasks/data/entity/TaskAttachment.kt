package org.tasks.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.Redacted
import org.tasks.data.UUIDHelper

@Serializable
@CommonParcelize
@Entity(tableName = "attachment_file")
data class TaskAttachment(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "file_id")
    @Transient
    val id: Long? = null,
    @ColumnInfo(name = "file_uuid")
    val remoteId: String = UUIDHelper.newUUID(),
    @Redacted
    @ColumnInfo(name = "filename")
    val name: String,
    @ColumnInfo(name = "uri")
    val uri: String,
) : CommonParcelable {
    companion object {
        const val KEY = "attachment"
        const val FILES_DIRECTORY_DEFAULT = "attachments"
    }
}