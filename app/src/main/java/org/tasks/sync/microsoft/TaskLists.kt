package org.tasks.sync.microsoft

import com.squareup.moshi.Json

data class TaskLists(
    @field:Json(name = "@odata.context") val context: String,
    val value: List<TaskList>,
    @field:Json(name = "@odata.nextLink") val nextPage: String?,
) {
    data class TaskList(
        @Json(name = "@odata.etag") val etag: String,
        val displayName: String,
        val isOwner: Boolean,
        val isShared: Boolean,
        val wellknownListName: String,
        val id: String,
    )
}