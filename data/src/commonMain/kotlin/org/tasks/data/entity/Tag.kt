package org.tasks.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.data.Redacted
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
    @Redacted
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
        val TABLE = Table("tags")
        val TASK = TABLE.column("task")
        val TAG_UID = TABLE.column("tag_uid")
        val NAME = TABLE.column("name")
    }
}