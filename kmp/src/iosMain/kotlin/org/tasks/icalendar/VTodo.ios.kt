package org.tasks.icalendar

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import libical.ICAL_ACTION_PROPERTY
import libical.ICAL_ANY_PARAMETER
import libical.ICAL_ANY_PROPERTY
import libical.ICAL_CATEGORIES_PROPERTY
import libical.ICAL_CLASS_PROPERTY
import libical.ICAL_COMMENT_PROPERTY
import libical.ICAL_COMPLETED_PROPERTY
import libical.ICAL_CREATED_PROPERTY
import libical.ICAL_DESCRIPTION_PROPERTY
import libical.ICAL_DTSTAMP_PROPERTY
import libical.ICAL_DTSTART_PROPERTY
import libical.ICAL_DUE_PROPERTY
import libical.ICAL_DURATION_PROPERTY
import libical.ICAL_EXDATE_PROPERTY
import libical.ICAL_GEO_PROPERTY
import libical.ICAL_LASTMODIFIED_PROPERTY
import libical.ICAL_LOCATION_PROPERTY
import libical.ICAL_ORGANIZER_PROPERTY
import libical.ICAL_PERCENTCOMPLETE_PROPERTY
import libical.ICAL_PRIORITY_PROPERTY
import libical.ICAL_PRODID_PROPERTY
import libical.ICAL_RDATE_PROPERTY
import libical.ICAL_RELATEDTO_PROPERTY
import libical.ICAL_RELATED_END
import libical.ICAL_RELATED_PARAMETER
import libical.ICAL_RELTYPE_PARAMETER
import libical.ICAL_REPEAT_PROPERTY
import libical.ICAL_RRULE_PROPERTY
import libical.ICAL_SEQUENCE_PROPERTY
import libical.ICAL_STATUS_PROPERTY
import libical.ICAL_SUMMARY_PROPERTY
import libical.ICAL_TEXT_VALUE
import libical.ICAL_TRIGGER_PROPERTY
import libical.ICAL_TZID_PARAMETER
import libical.ICAL_TZID_PROPERTY
import libical.ICAL_UID_PROPERTY
import libical.ICAL_URL_PROPERTY
import libical.ICAL_XLICERROR_PROPERTY
import libical.ICAL_X_PROPERTY
import libical.icalcomponent
import libical.icalcomponent_add_component
import libical.icalcomponent_add_property
import libical.icalcomponent_as_ical_string
import libical.icalcomponent_clone
import libical.icalcomponent_free
import libical.icalcomponent_get_first_component
import libical.icalcomponent_get_first_property
import libical.icalcomponent_get_next_component
import libical.icalcomponent_get_next_property
import libical.icalcomponent_get_timezone
import libical.icalcomponent_isa
import libical.icalcomponent_kind
import libical.icalcomponent_new_valarm
import libical.icalcomponent_new_vcalendar
import libical.icalcomponent_new_vtodo
import libical.icaldurationtype
import libical.icalparameter_as_ical_string
import libical.icalparameter_get_related
import libical.icalparameter_get_tzid
import libical.icalparameter_kind
import libical.icalparameter_new_from_string
import libical.icalparameter_related
import libical.icalparser_parse_string
import libical.icalproperty
import libical.icalproperty_add_parameter
import libical.icalproperty_get_categories
import libical.icalproperty_get_comment
import libical.icalproperty_get_completed
import libical.icalproperty_get_created
import libical.icalproperty_get_description
import libical.icalproperty_get_dtstamp
import libical.icalproperty_get_dtstart
import libical.icalproperty_get_due
import libical.icalproperty_get_duration
import libical.icalproperty_get_first_parameter
import libical.icalproperty_get_geo
import libical.icalproperty_get_lastmodified
import libical.icalproperty_get_location
import libical.icalproperty_get_next_parameter
import libical.icalproperty_get_percentcomplete
import libical.icalproperty_get_priority
import libical.icalproperty_get_property_name
import libical.icalproperty_get_repeat
import libical.icalproperty_get_sequence
import libical.icalproperty_get_summary
import libical.icalproperty_get_trigger
import libical.icalproperty_get_uid
import libical.icalproperty_get_url
import libical.icalproperty_get_value_as_string
import libical.icalproperty_get_x
import libical.icalproperty_get_x_name
import libical.icalproperty_isa
import libical.icalproperty_kind
import libical.icalproperty_kind_to_value_kind
import libical.icalproperty_new_completed
import libical.icalproperty_new_created
import libical.icalproperty_new_description
import libical.icalproperty_new_dtstamp
import libical.icalproperty_new_from_string
import libical.icalproperty_new_lastmodified
import libical.icalproperty_new_location
import libical.icalproperty_new_percentcomplete
import libical.icalproperty_new_priority
import libical.icalproperty_new_prodid
import libical.icalproperty_new_sequence
import libical.icalproperty_new_summary
import libical.icalproperty_new_uid
import libical.icalproperty_new_url
import libical.icalproperty_new_version
import libical.icalproperty_new_x
import libical.icalproperty_set_tzid
import libical.icalproperty_set_x_name
import libical.icalproperty_string_to_kind
import libical.icaltime_as_timet_with_zone
import libical.icaltime_from_string
import libical.icaltime_is_null_time
import libical.icaltime_is_utc
import libical.icaltimetype
import org.tasks.kmp.PROD_ID
import org.tasks.repeats.Recur
import org.tasks.repeats.serializeRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import platform.Foundation.NSUUID
import tasks.kmp.generated.resources.Res

@OptIn(ExperimentalForeignApi::class)
actual fun parseVTodos(iCalendar: String): List<VTodo> {
    val root = icalparser_parse_string(iCalendar.repairICalendar()) ?: return emptyList()
    try {
        return root.todos().map { it.toVTodo(root) }
    } finally {
        icalcomponent_free(root)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun VTodo.serialize(): String {
    val calendar = icalcomponent_new_vcalendar()!!
    try {
        calendar.add(icalproperty_new_version("2.0"))
        calendar.add(icalproperty_new_prodid(PROD_ID))
        val todo = icalcomponent_new_vtodo()!!
        icalcomponent_add_component(calendar, todo)
        todo.add(icalproperty_new_dtstamp(icaltime_from_string(utcText(currentTimeMillis()))))
        uid?.let { todo.add(icalproperty_new_uid(it)) }
        sequence?.takeIf { it != 0 }?.let { todo.add(icalproperty_new_sequence(it)) }
        createdAt?.let { todo.add(icalproperty_new_created(icaltime_from_string(utcText(it)))) }
        lastModified?.let { todo.add(icalproperty_new_lastmodified(icaltime_from_string(utcText(it)))) }
        summary?.let { todo.add(icalproperty_new_summary(it)) }
        location?.let { todo.add(icalproperty_new_location(it)) }
        geoPosition?.let { todo.add(icalproperty_new_from_string("GEO:${it.latitude};${it.longitude}")) }
        description?.let { todo.add(icalproperty_new_description(it)) }
        url?.let { todo.add(icalproperty_new_url(it)) }
        organizer?.let { todo.add(it.toProperty()) }
        if (priority != 0) todo.add(icalproperty_new_priority(priority))
        classification?.let { todo.add(icalproperty_new_from_string("CLASS:$it")) }
        status?.let { todo.add(icalproperty_new_from_string("STATUS:$it")) }
        rRule?.let { todo.add(icalproperty_new_from_string("RRULE:${serializeRecur(it)}")) }
        rDates.forEach { todo.add(it.toProperty()) }
        exDates.forEach { todo.add(it.toProperty()) }
        comment?.let { todo.add(icalproperty_new_from_string("COMMENT:${it.escapeICalText()}")) }
        relatedTo.forEach { related ->
            todo.add(icalproperty_new_from_string("RELATED-TO:${related.uid}")!!.also { property ->
                related.relType?.let { icalproperty_add_parameter(property, icalparameter_new_from_string("RELTYPE=$it")) }
            })
        }
        unknownProperties.forEach { todo.add(it.toProperty()) }
        val zones = mutableSetOf<String>()
        due?.let { todo.add(it.toProperty("DUE", zones)) }
        duration?.let { todo.add(icalproperty_new_from_string("DURATION:$it")) }
        dtStart?.let { todo.add(it.toProperty("DTSTART", zones)) }
        completedAt?.let { todo.add(icalproperty_new_completed(icaltime_from_string(utcText(it)))) }
        percentComplete?.let { todo.add(icalproperty_new_percentcomplete(it)) }
        alarms.forEach { icalcomponent_add_component(todo, it.toComponent()) }
        zones.forEach { tzId -> vTimeZone(tzId)?.let { icalcomponent_add_component(calendar, it) } }
        val text = icalcomponent_as_ical_string(calendar)!!.toKString()
        if (categories.isEmpty()) return text
        val end = text.indexOf("END:VTODO")
        return text.substring(0, end) +
            foldContentLine("CATEGORIES:" + categories.joinToString(",") { it.escapeICalText() }) +
            text.substring(end)
    } finally {
        icalcomponent_free(calendar)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.todos(): List<CPointer<icalcomponent>> = when (icalcomponent_isa(this)) {
    icalcomponent_kind.ICAL_VTODO_COMPONENT -> listOf(this)
    else -> components(icalcomponent_kind.ICAL_ANY_COMPONENT).flatMap { it.todos() }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.components(kind: icalcomponent_kind): List<CPointer<icalcomponent>> =
    generateSequence(icalcomponent_get_first_component(this, kind)) { icalcomponent_get_next_component(this, kind) }.toList()

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.properties(): List<CPointer<icalproperty>> =
    generateSequence(icalcomponent_get_first_property(this, ICAL_ANY_PROPERTY)) {
        icalcomponent_get_next_property(this, ICAL_ANY_PROPERTY)
    }.toList()

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.add(property: CPointer<icalproperty>?) {
    icalcomponent_add_property(this, property ?: return)
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.toVTodo(calendar: CPointer<icalcomponent>): VTodo {
    val todo = VTodo(sequence = 0)
    for (property in properties()) {
        when (icalproperty_isa(property)) {
            ICAL_UID_PROPERTY -> todo.uid = icalproperty_get_uid(property)?.toKString()
            ICAL_SEQUENCE_PROPERTY -> todo.sequence = icalproperty_get_sequence(property)
            ICAL_CREATED_PROPERTY -> todo.createdAt = property.utcMillis(icalproperty_get_created(property), calendar)
            ICAL_LASTMODIFIED_PROPERTY -> todo.lastModified = property.utcMillis(icalproperty_get_lastmodified(property), calendar)
            ICAL_DTSTAMP_PROPERTY -> todo.dtStamp = property.utcMillis(icalproperty_get_dtstamp(property), calendar)
            ICAL_SUMMARY_PROPERTY -> todo.summary = icalproperty_get_summary(property)?.toKString()
            ICAL_LOCATION_PROPERTY -> todo.location = icalproperty_get_location(property)?.toKString()
            ICAL_GEO_PROPERTY -> todo.geoPosition = icalproperty_get_geo(property).useContents {
                Geo(lat.toKString().toDouble(), lon.toKString().toDouble())
            }
            ICAL_DESCRIPTION_PROPERTY -> todo.description = icalproperty_get_description(property)?.toKString()
            ICAL_URL_PROPERTY -> todo.url = icalproperty_get_url(property)?.toKString()
            ICAL_ORGANIZER_PROPERTY -> todo.organizer = property.toICalProperty()
            ICAL_PRIORITY_PROPERTY -> todo.priority = icalproperty_get_priority(property)
            ICAL_CLASS_PROPERTY -> todo.classification = property.valueText()
            ICAL_STATUS_PROPERTY -> todo.status = property.valueText()
            ICAL_DUE_PROPERTY -> todo.due = property.toICalDate(icalproperty_get_due(property), calendar)
            ICAL_DURATION_PROPERTY -> todo.duration = property.valueText()
            ICAL_DTSTART_PROPERTY -> todo.dtStart = property.toICalDate(icalproperty_get_dtstart(property), calendar)
            ICAL_COMPLETED_PROPERTY -> todo.completedAt = property.utcMillis(icalproperty_get_completed(property), calendar)
            ICAL_PERCENTCOMPLETE_PROPERTY -> todo.percentComplete = icalproperty_get_percentcomplete(property)
            ICAL_RRULE_PROPERTY -> todo.rRule = property.valueText()?.let { Recur.parse(it) }
            ICAL_RDATE_PROPERTY -> todo.rDates += property.toICalProperty()
            ICAL_EXDATE_PROPERTY -> todo.exDates += property.toICalProperty()
            ICAL_CATEGORIES_PROPERTY -> icalproperty_get_categories(property)?.toKString()?.let { todo.categories += it }
            ICAL_COMMENT_PROPERTY -> todo.comment = icalproperty_get_comment(property)?.toKString()
            ICAL_RELATEDTO_PROPERTY -> property.valueText()?.let { uid ->
                todo.relatedTo += RelatedTo(uid, property.parameter(ICAL_RELTYPE_PARAMETER))
            }
            ICAL_PRODID_PROPERTY, ICAL_XLICERROR_PROPERTY -> {}
            else -> todo.unknownProperties += property.toICalProperty()
        }
    }
    if (todo.uid == null) todo.uid = NSUUID().UUIDString
    components(icalcomponent_kind.ICAL_VALARM_COMPONENT).mapNotNullTo(todo.alarms) { it.toVAlarm(calendar) }
    val dtStart = todo.dtStart
    val due = todo.due
    if (dtStart != null && due != null) {
        if (dtStart is ICalDate.Date && due is ICalDate.DateTime) {
            todo.dtStart = ICalDate.DateTime(dtStart.startOfDay(due.tzId), due.tzId, floating = due.tzId == null)
        } else if (dtStart is ICalDate.DateTime && due is ICalDate.Date) {
            todo.due = ICalDate.DateTime(due.startOfDay(dtStart.tzId), dtStart.tzId, floating = dtStart.tzId == null)
        }
        if (todo.due!!.millis() < todo.dtStart!!.millis()) todo.dtStart = null
    }
    if (todo.duration != null && todo.dtStart == null) todo.duration = null
    return todo
}

private fun ICalDate.Date.startOfDay(tzId: String?): Long =
    DateTime(year, month, day, timeZone = tzId?.let { TimeZone.of(it) } ?: TimeZone.currentSystemDefault()).millis

private fun ICalDate.millis(): Long = when (this) {
    is ICalDate.Date -> startOfDay(null)
    is ICalDate.DateTime -> millis
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalcomponent>.toVAlarm(calendar: CPointer<icalcomponent>): VAlarm? {
    var trigger: Trigger? = null
    var action: String? = null
    var description: String? = null
    var repeat: Int? = null
    var duration: Long? = null
    val others = mutableListOf<ICalProperty>()
    for (property in properties()) {
        when (icalproperty_isa(property)) {
            ICAL_TRIGGER_PROPERTY -> trigger = icalproperty_get_trigger(property).useContents {
                if (icaltime_is_null_time(this.time.readValue())) {
                    Trigger.Relative(this.duration.millis(), property.related() == ICAL_RELATED_END)
                } else {
                    Trigger.Absolute(property.utcMillis(this.time.readValue(), calendar))
                }
            }
            ICAL_ACTION_PROPERTY -> action = property.valueText()
            ICAL_DESCRIPTION_PROPERTY -> description = icalproperty_get_description(property)?.toKString()
            ICAL_REPEAT_PROPERTY -> repeat = icalproperty_get_repeat(property)
            ICAL_DURATION_PROPERTY -> duration = icalproperty_get_duration(property).useContents { millis() }
            ICAL_XLICERROR_PROPERTY -> {}
            else -> others += property.toICalProperty()
        }
    }
    return VAlarm(trigger ?: return null, action, description, repeat, duration, others)
}

@OptIn(ExperimentalForeignApi::class)
private fun VAlarm.toComponent(): CPointer<icalcomponent> {
    val alarm = icalcomponent_new_valarm()!!
    alarm.add(when (val trigger = trigger) {
        is Trigger.Absolute -> icalproperty_new_from_string("TRIGGER;VALUE=DATE-TIME:${utcText(trigger.millis)}")
        is Trigger.Relative -> icalproperty_new_from_string(
            "TRIGGER;RELATED=${if (trigger.relatedToEnd) "END" else "START"}:${formatICalDuration(trigger.millis)}",
        )
    })
    action?.let { alarm.add(icalproperty_new_from_string("ACTION:$it")) }
    description?.let { alarm.add(icalproperty_new_description(it)) }
    repeat?.let { alarm.add(icalproperty_new_from_string("REPEAT:$it")) }
    duration?.let { alarm.add(icalproperty_new_from_string("DURATION:${formatICalDuration(it)}")) }
    otherProperties.forEach { alarm.add(it.toProperty()) }
    return alarm
}

@OptIn(ExperimentalForeignApi::class)
private fun icaldurationtype.millis(): Long =
    ((weeks.toLong() * 7 + days.toLong()) * 86_400 + hours.toLong() * 3_600 + minutes.toLong() * 60 + seconds.toLong()) *
        1_000 * (if (is_neg != 0) -1 else 1)

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.valueText(): String? = icalproperty_get_value_as_string(this)?.toKString()

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.parameters(): List<Pair<String, String>> =
    generateSequence(icalproperty_get_first_parameter(this, ICAL_ANY_PARAMETER)) {
        icalproperty_get_next_parameter(this, ICAL_ANY_PARAMETER)
    }.map { parameter ->
        val text = icalparameter_as_ical_string(parameter)!!.toKString()
        text.substringBefore('=') to text.substringAfter('=', "").removeSurrounding("\"")
    }.toList()

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.parameter(kind: icalparameter_kind): String? =
    icalproperty_get_first_parameter(this, kind)?.let { icalparameter_as_ical_string(it)!!.toKString().substringAfter('=', "").removeSurrounding("\"") }

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.related(): icalparameter_related? =
    icalproperty_get_first_parameter(this, ICAL_RELATED_PARAMETER)?.let { icalparameter_get_related(it) }

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.toICalProperty(): ICalProperty =
    if (icalproperty_isa(this) == ICAL_X_PROPERTY) {
        ICalProperty(icalproperty_get_x_name(this)!!.toKString(), icalproperty_get_x(this)?.toKString().orEmpty(), parameters())
    } else {
        ICalProperty(icalproperty_get_property_name(this)!!.toKString(), valueText().orEmpty(), parameters())
    }

@OptIn(ExperimentalForeignApi::class)
private fun ICalProperty.toProperty(): CPointer<icalproperty>? {
    val property = if (name.startsWith("X-", ignoreCase = true)) {
        icalproperty_new_x(value)?.also { icalproperty_set_x_name(it, name) }
    } else {
        val text = if (icalproperty_kind_to_value_kind(icalproperty_string_to_kind(name)) == ICAL_TEXT_VALUE) value.escapeICalText() else value
        icalproperty_new_from_string("$name:$text")
    } ?: return null
    parameters.forEach { (parameterName, parameterValue) ->
        val quoted = if (parameterValue.any { it == ':' || it == ';' || it == ',' }) "\"$parameterValue\"" else parameterValue
        icalproperty_add_parameter(property, icalparameter_new_from_string("$parameterName=$quoted"))
    }
    return property
}

@OptIn(ExperimentalForeignApi::class)
private fun ICalDate.toProperty(name: String, zones: MutableSet<String>): CPointer<icalproperty>? = when (this) {
    is ICalDate.Date -> icalproperty_new_from_string("$name;VALUE=DATE:${year.pad(4)}${month.pad(2)}${day.pad(2)}")
    is ICalDate.DateTime -> {
        val zone = tzId?.let { id -> runCatching { TimeZone.of(id) }.getOrNull()?.takeIf { zoneText(id) != null } }
        when {
            zone != null -> {
                zones += tzId
                icalproperty_new_from_string("$name;TZID=$tzId:${DateTime(millis, zone).floatingText()}")
            }
            floating -> icalproperty_new_from_string(
                "$name:${DateTime(millis, TimeZone.currentSystemDefault()).floatingText()}"
            )
            else -> icalproperty_new_from_string("$name:${utcText(millis)}")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.toICalDate(time: CValue<icaltimetype>, calendar: CPointer<icalcomponent>): ICalDate = time.useContents {
    if (is_date != 0) {
        ICalDate.Date(year, month, day)
    } else {
        val tzId = parameter(ICAL_TZID_PARAMETER)
        when {
            icaltime_is_utc(time) -> ICalDate.DateTime(utcMillis())
            tzId == null -> ICalDate.DateTime(localMillis(TimeZone.currentSystemDefault()), floating = true)
            else -> zoneMillis(tzId, time, calendar)?.let { ICalDate.DateTime(it, tzId) }
                ?: ICalDate.DateTime(localMillis(TimeZone.currentSystemDefault()), floating = true)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<icalproperty>.utcMillis(time: CValue<icaltimetype>, calendar: CPointer<icalcomponent>): Long = time.useContents {
    val tzId = parameter(ICAL_TZID_PARAMETER)
    if (tzId != null && !icaltime_is_utc(time)) zoneMillis(tzId, time, calendar) ?: utcMillis() else utcMillis()
}

@OptIn(ExperimentalForeignApi::class)
private fun icaltimetype.zoneMillis(tzId: String, time: CValue<icaltimetype>, calendar: CPointer<icalcomponent>): Long? =
    runCatching { TimeZone.of(tzId) }.getOrNull()?.let { localMillis(it) }
        ?: icalcomponent_get_timezone(calendar, tzId)?.let { icaltime_as_timet_with_zone(time, it) * 1000L }

@OptIn(ExperimentalForeignApi::class)
private fun icaltimetype.localMillis(zone: TimeZone): Long =
    DateTime(year, month, day, hour, minute, second, timeZone = zone).millis

@OptIn(ExperimentalForeignApi::class)
private fun icaltimetype.utcMillis(): Long = localMillis(DateTime.UTC)

private val zoneTexts = mutableMapOf<String, String?>()

private fun zoneText(tzId: String): String? = zoneTexts.getOrPut(tzId) {
    runCatching { runBlocking { Res.readBytes("files/zoneinfo/$tzId.ics") } }.getOrNull()?.decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
private fun vTimeZone(tzId: String): CPointer<icalcomponent>? {
    val calendar = icalparser_parse_string(zoneText(tzId) ?: return null) ?: return null
    try {
        val zone = icalcomponent_get_first_component(calendar, icalcomponent_kind.ICAL_VTIMEZONE_COMPONENT) ?: return null
        val copy = icalcomponent_clone(zone)!!
        icalcomponent_get_first_property(copy, ICAL_TZID_PROPERTY)?.let { icalproperty_set_tzid(it, tzId) }
        return copy
    } finally {
        icalcomponent_free(calendar)
    }
}

private fun utcText(millis: Long): String = DateTime(millis, DateTime.UTC).floatingText() + "Z"

private fun DateTime.floatingText(): String =
    "${year.pad(4)}${monthOfYear.pad(2)}${dayOfMonth.pad(2)}T${hourOfDay.pad(2)}${minuteOfHour.pad(2)}${secondOfMinute.pad(2)}"

private fun Int.pad(width: Int) = toString().padStart(width, '0')
