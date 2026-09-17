package org.tasks.icalendar

import net.fortuna.ical4j.data.DefaultParameterFactorySupplier
import net.fortuna.ical4j.data.DefaultPropertyFactorySupplier
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.ParameterBuilder
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyBuilder
import net.fortuna.ical4j.model.TextList
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.parameter.Related
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Clazz
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DateProperty
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Duration
import net.fortuna.ical4j.model.property.ExDate
import net.fortuna.ical4j.model.property.Organizer
import net.fortuna.ical4j.model.property.RDate
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Repeat
import net.fortuna.ical4j.model.property.Status
import org.tasks.caldav.Task
import org.tasks.repeats.toIcal4j
import org.tasks.repeats.toRecur
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.time.Period
import java.time.temporal.TemporalAmount
import net.fortuna.ical4j.model.Date as Ical4jDate
import net.fortuna.ical4j.model.DateTime as Ical4jDateTime
import net.fortuna.ical4j.model.component.VAlarm as Ical4jVAlarm
import net.fortuna.ical4j.model.property.Geo as Ical4jGeo
import net.fortuna.ical4j.model.property.RelatedTo as Ical4jRelatedTo
import net.fortuna.ical4j.model.property.Trigger as Ical4jTrigger

actual fun parseVTodos(iCalendar: String): List<VTodo> =
    Task.tasksFromReader(StringReader(iCalendar)).map { it.toVTodo() }

actual fun VTodo.serialize(): String =
    toTask().let { task -> ByteArrayOutputStream().also(task::write).toString("UTF-8") }

private val timeZones by lazy { TimeZoneRegistryFactory.getInstance().createRegistry() }

private val propertyFactories by lazy { DefaultPropertyFactorySupplier().get() }

private val parameterFactories by lazy { DefaultParameterFactorySupplier().get() }

private val ALARM_PROPERTIES = setOf(Property.TRIGGER, Property.ACTION, Property.DESCRIPTION, Property.REPEAT, Property.DURATION)

fun Task.toVTodo(): VTodo = VTodo(
    uid = uid,
    sequence = sequence,
    createdAt = createdAt,
    lastModified = lastModified,
    dtStamp = dtStamp,
    summary = summary,
    location = location,
    geoPosition = geoPosition?.let { Geo(it.latitude.toDouble(), it.longitude.toDouble()) },
    description = description,
    color = color,
    url = url,
    organizer = organizer?.toICalProperty(),
    priority = priority,
    classification = classification?.value,
    status = status?.value,
    dtStart = dtStart?.toICalDate(),
    due = due?.toICalDate(),
    duration = duration?.value,
    completedAt = completedAt?.dateTime?.time,
    percentComplete = percentComplete,
    rRule = rRule?.recur?.toRecur(),
    rDates = rDates.mapTo(mutableListOf()) { it.toICalProperty() },
    exDates = exDates.mapTo(mutableListOf()) { it.toICalProperty() },
    categories = categories.toMutableList(),
    comment = comment,
    relatedTo = relatedTo.mapTo(mutableListOf()) {
        RelatedTo(it.value, it.parameters.getParameter<RelType>(Parameter.RELTYPE)?.value)
    },
    unknownProperties = unknownProperties.mapTo(mutableListOf()) { it.toICalProperty() },
    alarms = alarms.mapNotNullTo(mutableListOf()) { it.toVAlarm() },
)

fun VTodo.toTask(): Task = Task(
    uid = uid,
    sequence = sequence,
    createdAt = createdAt,
    lastModified = lastModified,
    dtStamp = dtStamp,
    summary = summary,
    location = location,
    geoPosition = geoPosition?.let { Ical4jGeo(BigDecimal(it.latitude.toString()), BigDecimal(it.longitude.toString())) },
    description = description,
    color = color,
    url = url,
    organizer = organizer?.let { Organizer(it.parameters.toIcal4j(), it.value) },
    priority = priority,
    classification = classification?.let { Clazz(it) },
    status = status?.let { Status(it) },
    dtStart = dtStart?.let { DtStart(it.toIcal4j()) },
    due = due?.let { Due(it.toIcal4j()) },
    duration = duration?.let { Duration(ParameterList(), it) },
    completedAt = completedAt?.let { Completed(utcDateTime(it)) },
    percentComplete = percentComplete,
    rRule = rRule?.let { RRule(it.toIcal4j()) },
    rDates = rDates.mapTo(java.util.LinkedList()) { RDate(it.parameters.toIcal4j(), it.value) },
    exDates = exDates.mapTo(java.util.LinkedList()) { ExDate(it.parameters.toIcal4j(), it.value) },
    categories = java.util.LinkedList(categories),
    comment = comment,
    relatedTo = relatedTo.mapTo(java.util.LinkedList()) { related ->
        Ical4jRelatedTo(ParameterList().apply { related.relType?.let { add(RelType(it)) } }, related.uid)
    },
    unknownProperties = unknownProperties.mapTo(java.util.LinkedList()) { it.toIcal4j() },
    alarms = alarms.mapTo(java.util.LinkedList()) { it.toIcal4j() },
)

fun DateProperty.toICalDate(): ICalDate = when (val date = date) {
    is Ical4jDateTime -> ICalDate.DateTime(date.time, date.timeZone?.id?.takeUnless { date.isUtc })
    else -> date.toString().let { ICalDate.Date(it.substring(0, 4).toInt(), it.substring(4, 6).toInt(), it.substring(6, 8).toInt()) }
}

private fun ICalDate.toIcal4j(): Ical4jDate = when (this) {
    is ICalDate.Date -> Ical4jDate("${year.pad(4)}${month.pad(2)}${day.pad(2)}")
    is ICalDate.DateTime -> tzId?.let { timeZones.getTimeZone(it) }
        ?.let { zone -> Ical4jDateTime(millis).apply { timeZone = zone } }
        ?: utcDateTime(millis)
}

private fun utcDateTime(millis: Long) = Ical4jDateTime(true).apply { time = millis }

private fun Int.pad(width: Int) = toString().padStart(width, '0')

private fun Property.toICalProperty() = ICalProperty(name, value, parameters.map { it.name to it.value })

private fun ICalProperty.toIcal4j(): Property = PropertyBuilder()
    .factories(propertyFactories)
    .name(name)
    .value(value)
    .apply { parameters.toIcal4j().forEach { parameter(it) } }
    .build()

private fun List<Pair<String, String>>.toIcal4j() = ParameterList().also { list ->
    forEach { (name, value) -> list.add(ParameterBuilder().factories(parameterFactories).name(name).value(value).build()) }
}

private fun Ical4jVAlarm.toVAlarm(): VAlarm? {
    val trigger = trigger ?: return null
    val kind = when {
        trigger.dateTime != null -> Trigger.Absolute(trigger.dateTime.time)
        trigger.duration != null -> Trigger.Relative(
            trigger.duration.toMillis(),
            trigger.parameters.getParameter<Related>(Related.RELATED) == Related.END,
        )
        else -> return null
    }
    return VAlarm(
        trigger = kind,
        action = action?.value,
        description = description?.value,
        repeat = repeat?.count,
        duration = duration?.duration?.toMillis(),
        otherProperties = properties.filterNot { it.name in ALARM_PROPERTIES }.map { it.toICalProperty() },
    )
}

private fun VAlarm.toIcal4j(): Ical4jVAlarm = Ical4jVAlarm().also { alarm ->
    with(alarm.properties) {
        add(when (val trigger = trigger) {
            is Trigger.Absolute -> Ical4jTrigger(utcDateTime(trigger.millis))
            is Trigger.Relative -> Ical4jTrigger(
                ParameterList().apply { add(if (trigger.relatedToEnd) Related.END else Related.START) },
                java.time.Duration.ofMillis(trigger.millis),
            )
        })
        action?.let { add(Action(it)) }
        description?.let { add(Description(it)) }
        repeat?.let { add(Repeat(it)) }
        duration?.let { add(Duration(java.time.Duration.ofMillis(it))) }
        otherProperties.forEach { add(it.toIcal4j()) }
    }
}

private fun TemporalAmount.toMillis(): Long = when (this) {
    is java.time.Duration -> toMillis()
    is Period -> java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = 0
        add(java.util.Calendar.DAY_OF_MONTH, days)
        add(java.util.Calendar.MONTH, months)
        add(java.util.Calendar.YEAR, years)
    }.timeInMillis
    else -> throw IllegalArgumentException("TemporalAmount must be Period or Duration")
}
