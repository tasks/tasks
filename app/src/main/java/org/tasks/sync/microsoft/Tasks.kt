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
        val completedDateTime: DateTime? = null,
        val dueDateTime: DateTime? = null,
        val linkedResources: List<LinkedResource>? = null,
        val recurrence: Recurrence? = null,
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

        data class DateTime(
            val dateTime: String,
            val timeZone: String,
        )

        data class Recurrence(
            val pattern: Pattern,
        )

        data class Pattern(
            val type: RecurrenceType,
            val interval: Int,
            val month: Int = 0,
            val dayOfMonth: Int = 0,
            val daysOfWeek: List<RecurrenceDayOfWeek>,
            val firstDayOfWeek: RecurrenceDayOfWeek = RecurrenceDayOfWeek.sunday,
            val index: RecurrenceIndex = RecurrenceIndex.first,
        )

        enum class RecurrenceType {
            daily,
            weekly,
            absoluteMonthly,
            relativeMonthly,
            absoluteYearly,
            relativeYearly,
        }

        enum class RecurrenceIndex {
            first,
            second,
            third,
            fourth,
            last,
        }

        enum class RecurrenceDayOfWeek {
            sunday,
            monday,
            tuesday,
            wednesday,
            thursday,
            friday,
            saturday,
        }

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