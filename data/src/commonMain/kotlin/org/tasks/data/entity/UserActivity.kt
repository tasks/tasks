package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.db.Table

@Serializable
@CommonParcelize
@Entity(tableName = "userActivity")
data class UserActivity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long? = null,
    @ColumnInfo(name = "remoteId")
    var remoteId: String? = Task.NO_UUID,
    @ColumnInfo(name = "message")
    var message: String? = "",
    @ColumnInfo(name = "picture")
    var picture: String? = "",
    @ColumnInfo(name = "target_id")
    @Transient
    var targetId: String? = Task.NO_UUID,
    @ColumnInfo(name = "created_at")
    var created: Long? = 0L,
) : CommonParcelable {
    companion object {
        @JvmField val TABLE = Table("userActivity")
        @JvmField val TASK = TABLE.column("target_id")
        @JvmField val MESSAGE = TABLE.column("message")
    }
}