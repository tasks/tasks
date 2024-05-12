package org.tasks.sync.microsoft

import com.squareup.moshi.Json
import kotlinx.serialization.Serializable
import org.tasks.data.entity.CaldavCalendar

data class TaskLists(
    @field:Json(name = "@odata.context") val context: String,
    val value: List<TaskList>,
    @field:Json(name = "@odata.nextLink") val nextPage: String?,
) {
    @Serializable
    data class TaskList(
        @Json(name = "@odata.etag") val etag: String? = null,
        val displayName: String? = null,
        val isOwner: Boolean? = null,
        val isShared: Boolean? = null,
        val wellknownListName: String? = null,
        val id: String? = null,
    ) {
        fun applyTo(list: CaldavCalendar) {
            with (list) {
                name = displayName
                url = this@TaskList.id
                uuid = this@TaskList.id
                access = when {
                    isOwner == true -> CaldavCalendar.ACCESS_OWNER
                    isShared == true -> CaldavCalendar.ACCESS_READ_WRITE
                    else -> CaldavCalendar.ACCESS_UNKNOWN
                }
            }
        }
    }
}