package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.data.db.Table

@Serializable
@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["_id"],
            childColumns = ["task"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "task", index = true)
    @Transient
    val task: Long = 0,
    @ColumnInfo(name = "name")
    val name: String? = null,
    @ColumnInfo(name = "tag_uid")
    val tagUid: String? = null,
    @ColumnInfo(name = "task_uid")
    @Transient
    val taskUid: String? = null,
) {
    companion object {
        const val KEY = "tags-tag" // $NON-NLS-1$
        @JvmField val TABLE = Table("tags")
        @JvmField val TASK = TABLE.column("task")
        @JvmField val TAG_UID = TABLE.column("tag_uid")
        @JvmField val NAME = TABLE.column("name")
    }
}