package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.astrid.helper.UUIDHelper

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
    @Ignore
    constructor(parcel: Parcel) : this(
        remoteId = parcel.readString()!!,
        name = parcel.readString()!!,
        uri = parcel.readString()!!,
    )

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(remoteId)
        parcel.writeString(name)
        parcel.writeString(uri)
    }

    companion object {
        const val KEY = "attachment"
        const val FILES_DIRECTORY_DEFAULT = "attachments"

        @JvmField
        val CREATOR = object : Parcelable.Creator<TaskAttachment> {
            override fun createFromParcel(parcel: Parcel) = TaskAttachment(parcel)

            override fun newArray(size: Int): Array<TaskAttachment?> = arrayOfNulls(size)
        }
    }
}