package org.tasks.sync.microsoft

import com.squareup.moshi.Json

data class Tasks(
    val value: List<Task>,
    @field:Json(name = "@odata.nextLink") val nextPage: String?,
    @field:Json(name = "@odata.deltaLink") val nextDelta: String?,
) {
    data class Task(
        @field:Json(name = "@odata.etag") val etag: String? = null,
        val id: String? = null,
        val title: String? = null,
        val body: Body? = null,
        val importance: Importance = Importance.low,
        val status: Status = Status.notStarted,
        val categories: List<String>? = null,
        val isReminderOn: Boolean = false,
        val createdDateTime: String? = null,
        val lastModifiedDateTime: String? = null,
        val completedDateTime: CompletedDateTime? = null,
        val linkedResources: List<LinkedResource>? = null,
        @field:Json(name = "@removed") val removed: Removed? = null,
    ) {
        data class Body(
            val content: String,
            val contentType: String,
        )

        data class LinkedResource(
            val applicationName: String,
            val displayName: String,
            val externalId: String,
            val id: String,
        )

        data class Removed(
            val reason: String,
        )

        data class CompletedDateTime(
            val dateTime: String,
            val timeZone: String,
        )

        enum class Importance {
            low,
            normal,
            high,
        }
        enum class Status {
            notStarted,
            inProgress,
            completed,
            waitingOnOthers,
            deferred,
        }
    }
}