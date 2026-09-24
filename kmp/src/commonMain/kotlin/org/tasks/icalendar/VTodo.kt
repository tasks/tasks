package org.tasks.icalendar

import org.tasks.repeats.Recur

sealed interface ICalDate {
    data class Date(val year: Int, val month: Int, val day: Int) : ICalDate

    data class DateTime(
        val millis: Long,
        val tzId: String? = null,
        val floating: Boolean = false,
    ) : ICalDate
}

data class ICalProperty(
    val name: String,
    val value: String,
    val parameters: List<Pair<String, String>> = emptyList(),
)

data class RelatedTo(
    val uid: String,
    val relType: String? = null,
    val parameters: List<Pair<String, String>> = emptyList(),
)

data class Geo(val latitude: Double, val longitude: Double)

sealed interface Trigger {
    data class Absolute(val millis: Long) : Trigger

    data class Relative(val millis: Long, val relatedToEnd: Boolean = false) : Trigger
}

data class VAlarm(
    val trigger: Trigger,
    val action: String? = null,
    val description: String? = null,
    val repeat: Int? = null,
    val duration: Long? = null,
    val otherProperties: List<ICalProperty> = emptyList(),
)

object TodoStatus {
    const val NEEDS_ACTION = "NEEDS-ACTION"
    const val COMPLETED = "COMPLETED"
    const val IN_PROCESS = "IN-PROCESS"
    const val CANCELLED = "CANCELLED"
}

data class VTodo(
    var uid: String? = null,
    var sequence: Int? = null,
    var createdAt: Long? = null,
    var lastModified: Long? = null,
    var dtStamp: Long? = null,
    var summary: String? = null,
    var location: String? = null,
    var geoPosition: Geo? = null,
    var description: String? = null,
    var color: Int? = null,
    var url: String? = null,
    var organizer: ICalProperty? = null,
    var priority: Int = 0,
    var classification: String? = null,
    var status: String? = null,
    var dtStart: ICalDate? = null,
    var due: ICalDate? = null,
    var duration: String? = null,
    var completedAt: Long? = null,
    var percentComplete: Int? = null,
    var rRule: Recur? = null,
    val rDates: MutableList<ICalProperty> = mutableListOf(),
    val exDates: MutableList<ICalProperty> = mutableListOf(),
    val categories: MutableList<String> = mutableListOf(),
    var comment: String? = null,
    val relatedTo: MutableList<RelatedTo> = mutableListOf(),
    val unknownProperties: MutableList<ICalProperty> = mutableListOf(),
    val alarms: MutableList<VAlarm> = mutableListOf(),
)

expect fun parseVTodos(iCalendar: String): List<VTodo>

expect fun VTodo.serialize(): String
