/*
 * Based on ical4android which is released under GPLv3.
 * Copyright © ical4android contributors. See https://github.com/bitfireAT/ical4android
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.tasks.caldav

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.data.CalendarParserFactory
import net.fortuna.ical4j.data.ContentHandlerContext
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.ComponentList
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.TextList
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.Daylight
import net.fortuna.ical4j.model.component.Observance
import net.fortuna.ical4j.model.component.Standard
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VToDo
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.Categories
import net.fortuna.ical4j.model.property.Clazz
import net.fortuna.ical4j.model.property.Comment
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.Created
import net.fortuna.ical4j.model.property.DateProperty
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Duration
import net.fortuna.ical4j.model.property.ExDate
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.LastModified
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.Organizer
import net.fortuna.ical4j.model.property.PercentComplete
import net.fortuna.ical4j.model.property.Priority
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.RDate
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Sequence
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Url
import net.fortuna.ical4j.model.property.Version
import net.fortuna.ical4j.validate.ValidationException
import java.io.OutputStream
import java.io.Reader
import java.net.URI
import java.net.URISyntaxException
import java.util.LinkedList
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

data class Task(
    override var uid: String? = null,
    override var sequence: Int? = null,

    override var createdAt: Long? = null,
    override var lastModified: Long? = null,

    override var summary: String? = null,
    override var location: String? = null,
    override var geoPosition: Geo? = null,
    override var description: String? = null,
    override var color: Int? = null,
    override var url: String? = null,
    override var organizer: Organizer? = null,

    override var priority: Int = Priority.UNDEFINED.level,

    override var classification: Clazz? = null,
    override var status: Status? = null,

    override var dtStart: DtStart? = null,
    override var due: Due? = null,
    override var duration: Duration? = null,
    override var completedAt: Completed? = null,

    override var percentComplete: Int? = null,

    override var rRule: RRule? = null,
    override val rDates: LinkedList<RDate> = LinkedList(),
    override val exDates: LinkedList<ExDate> = LinkedList(),

    override val categories: LinkedList<String> = LinkedList(),
    override var comment: String? = null,
    override var relatedTo: LinkedList<RelatedTo> = LinkedList(),
    override val unknownProperties: LinkedList<Property> = LinkedList(),

    override val alarms: LinkedList<VAlarm> = LinkedList(),
) : VTodoTask {

    fun generateUID() {
        uid = UUID.randomUUID().toString()
    }

    override fun write(os: OutputStream) {
        val ical = Calendar()
        ical.properties += Version.VERSION_2_0
        ical.properties += prodId

        val vTodo = VToDo(true /* generates DTSTAMP */)
        ical.components += vTodo
        val props = vTodo.properties

        uid?.let { props += Uid(it) }
        sequence?.let {
            if (it != 0)
                props += Sequence(it)
        }

        createdAt?.let { props += Created(DateTime(it)) }
        lastModified?.let { props += LastModified(DateTime(it)) }

        summary?.let { props += Summary(it) }
        location?.let { props += Location(it) }
        geoPosition?.let { props += it }
        description?.let { props += Description(it) }
        url?.let {
            try {
                props += Url(URI(it))
            } catch (e: URISyntaxException) {
                logger.log(Level.WARNING, "Ignoring invalid task URL: $url", e)
            }
        }
        organizer?.let { props += it }

        if (priority != Priority.UNDEFINED.level)
            props += Priority(priority)
        classification?.let { props += it }
        status?.let { props += it }

        rRule?.let { props += it }
        rDates.forEach { props += it }
        exDates.forEach { props += it }

        if (categories.isNotEmpty())
            props += Categories(TextList(categories.toTypedArray()))
        comment?.let { props += Comment(it) }
        props.addAll(relatedTo)
        props.addAll(unknownProperties)

        // remember used time zones
        val usedTimeZones = HashSet<TimeZone>()
        due?.let {
            props += it
            it.timeZone?.let(usedTimeZones::add)
        }
        duration?.let(props::add)
        dtStart?.let {
            props += it
            it.timeZone?.let(usedTimeZones::add)
        }
        completedAt?.let {
            props += it
            it.timeZone?.let(usedTimeZones::add)
        }
        percentComplete?.let { props += PercentComplete(it) }

        if (alarms.isNotEmpty())
            vTodo.components.addAll(alarms)

        // determine earliest referenced date
        val earliest = arrayOf(
            dtStart?.date,
            due?.date,
            completedAt?.date
        ).filterNotNull().minOrNull()
        // add VTIMEZONE components
        for (tz in usedTimeZones)
            ical.components += minifyVTimeZone(tz.vTimeZone, earliest)

        CalendarOutputter(false).output(ical, os)
    }

    companion object {

        private val logger
            get() = Logger.getLogger(Task::class.java.name)

        var prodId: ProdId = ProdId("+//IDN bitfire.at//ical4android")

        fun tasksFromReader(reader: Reader): List<Task> {
            val preprocessed = ICalPreprocessor.preprocessStream(reader)

            val calendar = CalendarBuilder(
                CalendarParserFactory.getInstance().get(),
                ContentHandlerContext().withSupressInvalidProperties(true),
                TimeZoneRegistryFactory.getInstance().createRegistry()
            ).build(preprocessed)

            try {
                ICalPreprocessor.preprocessCalendar(calendar)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Couldn't pre-process iCalendar", e)
            }

            val vToDos = calendar.getComponents<VToDo>(Component.VTODO)
            return vToDos.mapTo(LinkedList()) { fromVToDo(it) }
        }

        private fun fromVToDo(todo: VToDo): Task {
            val t = Task()

            if (todo.uid != null)
                t.uid = todo.uid.value
            else {
                logger.warning("Received VTODO without UID, generating new one")
                t.generateUID()
            }

            // sequence must only be null for locally created, not-yet-synchronized events
            t.sequence = 0

            for (prop in todo.properties)
                when (prop) {
                    is Sequence -> t.sequence = prop.sequenceNo
                    is Created -> t.createdAt = prop.dateTime.time
                    is LastModified -> t.lastModified = prop.dateTime.time
                    is Summary -> t.summary = prop.value
                    is Location -> t.location = prop.value
                    is Geo -> t.geoPosition = prop
                    is Description -> t.description = prop.value
                    is Url -> t.url = prop.value
                    is Organizer -> t.organizer = prop
                    is Priority -> t.priority = prop.level
                    is Clazz -> t.classification = prop
                    is Status -> t.status = prop
                    is Due -> t.due = prop
                    is Duration -> t.duration = prop
                    is DtStart -> t.dtStart = prop
                    is Completed -> t.completedAt = prop
                    is PercentComplete -> t.percentComplete = prop.percentage
                    is RRule -> t.rRule = prop
                    is RDate -> t.rDates += prop
                    is ExDate -> t.exDates += prop
                    is Categories ->
                        for (category in prop.categories)
                            t.categories += category
                    is Comment -> t.comment = prop.value
                    is RelatedTo -> t.relatedTo.add(prop)
                    is Uid, is ProdId, is DtStamp -> { /* don't save these as unknown properties */ }
                    else -> t.unknownProperties += prop
                }

            t.alarms.addAll(todo.alarms)

            // There seem to be many invalid tasks out there because of some defect clients, do some validation.
            val dtStart = t.dtStart
            val due = t.due

            if (dtStart != null && due != null) {
                if (isDate(dtStart) && isDateTime(due)) {
                    logger.warning("DTSTART is DATE but DUE is DATE-TIME, rewriting DTSTART to DATE-TIME")
                    t.dtStart = DtStart(DateTime(dtStart.value, due.timeZone))
                } else if (isDateTime(dtStart) && isDate(due)) {
                    logger.warning("DTSTART is DATE-TIME but DUE is DATE, rewriting DUE to DATE-TIME")
                    t.due = Due(DateTime(due.value, dtStart.timeZone))
                }

                if (due.date < dtStart.date) {
                    logger.warning("Found invalid DUE <= DTSTART; dropping DTSTART")
                    t.dtStart = null
                }
            }

            if (t.duration != null && t.dtStart == null) {
                logger.warning("Found DURATION without DTSTART; ignoring")
                t.duration = null
            }

            return t
        }

        private fun isDate(date: DateProperty?) =
            date != null && date.date is Date && date.date !is DateTime

        private fun isDateTime(date: DateProperty?) =
            date != null && date.date is DateTime

        fun minifyVTimeZone(originalTz: VTimeZone, start: Date?): VTimeZone {
            var newTz: VTimeZone? = null
            val keep = mutableSetOf<Observance>()

            if (start != null) {
                var latestDaylight: Pair<Date, Observance>? = null
                var latestStandard: Pair<Date, Observance>? = null
                for (observance in originalTz.observances) {
                    val latest = observance.getLatestOnset(start)

                    if (latest == null)
                        keep += observance
                    else
                        when (observance) {
                            is Standard ->
                                if (latestStandard == null || latest > latestStandard.first)
                                    latestStandard = Pair(latest, observance)
                            is Daylight ->
                                if (latestDaylight == null || latest > latestDaylight.first)
                                    latestDaylight = Pair(latest, observance)
                        }
                }

                latestStandard?.second?.let { keep += it }

                latestDaylight?.second?.let { daylight ->
                    if (latestStandard != null) {
                        val latestStandardOnset = latestStandard.second.getLatestOnset(start)
                        val latestDaylightOnset = daylight.getLatestOnset(start)
                        if (latestStandardOnset != null && latestDaylightOnset != null && latestDaylightOnset > latestStandardOnset) {
                            keep += daylight
                            return@let
                        }
                    }

                    for (rRule in daylight.getProperties<RRule>(Property.RRULE)) {
                        val nextDstOnset = rRule.recur.getNextDate(daylight.startDate.date, start)
                        if (nextDstOnset != null) {
                            keep += daylight
                            return@let
                        }
                    }
                    for (rDate in daylight.getProperties<RDate>(Property.RDATE)) {
                        if (rDate.dates.any { it >= start }) {
                            keep += daylight
                            return@let
                        }
                    }
                }

                val relevantProperties = PropertyList<Property>().apply {
                    add(originalTz.timeZoneId)
                }
                val relevantObservances = ComponentList<Observance>().apply {
                    addAll(keep)
                }
                newTz = VTimeZone(relevantProperties, relevantObservances)

                try {
                    newTz.validate()
                } catch (e: ValidationException) {
                    logger.log(Level.WARNING, "Minified timezone is invalid, using original one", e)
                    newTz = null
                }
            }

            return newTz ?: originalTz
        }
    }
}
