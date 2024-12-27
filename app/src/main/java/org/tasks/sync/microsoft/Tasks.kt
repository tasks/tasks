package org.tasks.sync.microsoft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tasks(
    val value: List<Task>,
    @SerialName("@odata.nextLink") val nextPage: String? = null,
    @SerialName("@odata.deltaLink") val nextDelta: String? = null,
) {
    @Serializable
    data class Task(
        @SerialName("@odata.etag") val etag: String? = null,
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
        @SerialName("@removed") val removed: Removed? = null,
    ) {
        @Serializable
        data class Body(
            val content: String,
            val contentType: String,
        )

        @Serializable
        data class LinkedResource(
            val applicationName: String,
            val displayName: String,
            val externalId: String,
            val id: String,
        )

        @Serializable
        data class Removed(
            val reason: String,
        )

        @Serializable
        data class DateTime(
            val dateTime: String,
            val timeZone: String,
        )

        @Serializable
        data class Recurrence(
            val pattern: Pattern,
        )

        @Serializable
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