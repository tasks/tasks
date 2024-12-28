package org.tasks.sync.microsoft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.tasks.data.Redacted
import org.tasks.data.entity.CaldavCalendar

@Serializable
data class TaskLists(
    @SerialName("@odata.context") val context: String,
    val value: List<TaskList>,
    @SerialName("@odata.nextLink") val nextPage: String?,
) {
    @Serializable
    data class TaskList(
        @SerialName("@odata.etag") val etag: String? = null,
        @Redacted val displayName: String? = null,
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
