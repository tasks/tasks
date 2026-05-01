package org.tasks.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Entity(tableName = "task_list_metadata")
class TaskListMetadata {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long? = null

    @ColumnInfo(name = "tag_uuid")
    var tagUuid: String? = Task.NO_UUID

    @ColumnInfo(name = "filter")
    var filter: String? = ""

    @ColumnInfo(name = "task_ids")
    var taskIds: String? = "[]"

    companion object {
        const val FILTER_ID_ALL = "all"
        const val FILTER_ID_TODAY = "today"
    }
}