package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.icalendar.TodoStatus
import org.tasks.icalendar.VTodo
import org.tasks.icalendar.toICalDate
import org.tasks.icalendar.toICalDateTime
import org.tasks.repeats.Recur
import org.tasks.time.DateTime

@Suppress("ClassName")
object iCalMaker {
    val TITLE: Property<VTodo, String?> = newProperty()
    val DESCRIPTION: Property<VTodo, String?> = newProperty()
    val DUE_DATE: Property<VTodo, DateTime?> = newProperty()
    val DUE_TIME: Property<VTodo, DateTime?> = newProperty()
    val START_DATE: Property<VTodo, DateTime?> = newProperty()
    val START_TIME: Property<VTodo, DateTime?> = newProperty()
    val CREATED_AT: Property<VTodo, DateTime?> = newProperty()
    val COMPLETED_AT: Property<VTodo, DateTime?> = newProperty()
    val ORDER: Property<VTodo, Long?> = newProperty()
    val PARENT: Property<VTodo, String?> = newProperty()
    val PRIORITY: Property<VTodo, Int> = newProperty()
    val COLLAPSED: Property<VTodo, Boolean> = newProperty()
    val LAST_MODIFIED: Property<VTodo, DateTime?> = newProperty()
    val DT_STAMP: Property<VTodo, DateTime?> = newProperty()
    val RRULE: Property<VTodo, String?> = newProperty()
    val STATUS: Property<VTodo, String?> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<VTodo> ->
        val task = VTodo()
        lookup.valueOf(CREATED_AT, null as DateTime?)?.let {
            task.createdAt = it.millis
        }
        lookup.valueOf(DUE_DATE, null as DateTime?)?.let {
            task.due = it.millis.toICalDate()
        }
        lookup.valueOf(DUE_TIME, null as DateTime?)?.let {
            task.due = it.millis.toICalDateTime()
        }
        lookup.valueOf(START_DATE, null as DateTime?)?.let {
            task.dtStart = it.millis.toICalDate()
        }
        lookup.valueOf(START_TIME, null as DateTime?)?.let {
            task.dtStart = it.millis.toICalDateTime()
        }
        lookup.valueOf(COMPLETED_AT, null as DateTime?)?.let {
            task.completedAt = it.millis
            task.status = TodoStatus.COMPLETED
        }
        task.order = lookup.valueOf(ORDER, null as Long?)
        task.summary = lookup.valueOf(TITLE, null as String?)
        task.parent = lookup.valueOf(PARENT, null as String?)
        task.description = lookup.valueOf(DESCRIPTION, null as String?)
        task.priority = lookup.valueOf(PRIORITY, 0)
        task.collapsed = lookup.valueOf(COLLAPSED, false)
        lookup.valueOf(LAST_MODIFIED, null as DateTime?)?.let {
            task.lastModified = it.millis
        }
        lookup.valueOf(DT_STAMP, null as DateTime?)?.let {
            task.dtStamp = it.millis
        }
        task.rRule = lookup.valueOf(RRULE, null as String?)?.let { Recur.parse(it) }
        task.status = lookup.valueOf(STATUS, null as String?)
        task
    }
    fun newIcal(vararg properties: PropertyValue<in VTodo?, *>): VTodo {
        return Maker.make(instantiator, *properties)
    }
}