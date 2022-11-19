package org.tasks.makers

import at.bitfire.ical4android.Task
import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Priority
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Status
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.toDate

@Suppress("ClassName")
object iCalMaker {
    val TITLE: Property<Task, String?> = newProperty()
    val DESCRIPTION: Property<Task, String?> = newProperty()
    val DUE_DATE: Property<Task, DateTime?> = newProperty()
    val DUE_TIME: Property<Task, DateTime?> = newProperty()
    val START_DATE: Property<Task, DateTime?> = newProperty()
    val START_TIME: Property<Task, DateTime?> = newProperty()
    val CREATED_AT: Property<Task, DateTime?> = newProperty()
    val COMPLETED_AT: Property<Task, DateTime?> = newProperty()
    val ORDER: Property<Task, Long?> = newProperty()
    val PARENT: Property<Task, String?> = newProperty()
    val PRIORITY: Property<Task, Int> = newProperty()
    val COLLAPSED: Property<Task, Boolean> = newProperty()
    val RRULE: Property<Task, String?> = newProperty()
    val STATUS: Property<Task, Status?> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Task> ->
        val task = Task()
        lookup.valueOf(CREATED_AT, null as DateTime?)?.let {
            task.createdAt = it.millis
        }
        lookup.valueOf(DUE_DATE, null as DateTime?)?.let {
            task.due = Due(it.millis.toDate())
        }
        lookup.valueOf(DUE_TIME, null as DateTime?)?.let {
            task.due = Due(iCalendar.getDateTime(it.millis))
        }
        lookup.valueOf(START_DATE, null as DateTime?)?.let {
            task.dtStart = DtStart(it.millis.toDate())
        }
        lookup.valueOf(START_TIME, null as DateTime?)?.let {
            task.dtStart = DtStart(iCalendar.getDateTime(it.millis))
        }
        lookup.valueOf(COMPLETED_AT, null as DateTime?)?.let {
            task.completedAt = Completed(iCalendar.getDateTime(it.millis))
            task.status = Status.VTODO_COMPLETED
        }
        task.order = lookup.valueOf(ORDER, null as Long?)
        task.summary = lookup.valueOf(TITLE, null as String?)
        task.parent = lookup.valueOf(PARENT, null as String?)
        task.description = lookup.valueOf(DESCRIPTION, null as String?)
        task.priority = lookup.valueOf(PRIORITY, Priority.UNDEFINED.level)
        task.collapsed = lookup.valueOf(COLLAPSED, false)
        task.rRule = lookup.valueOf(RRULE, null as String?)?.let { RRule(it) }
        task.status = lookup.valueOf(STATUS, null as Status?)
        task
    }
    fun newIcal(vararg properties: PropertyValue<in Task?, *>): Task {
        return Maker.make(instantiator, *properties)
    }
}