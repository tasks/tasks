package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import org.tasks.data.entity.Task.Companion.NO_UUID
import org.tasks.data.createDueDate
import org.tasks.data.createHideUntil
import org.tasks.date.DateTimeUtils
import org.tasks.makers.Maker.make
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime

object TaskMaker {
    val ID: Property<Task, Long> = newProperty()
    val DUE_DATE: Property<Task, DateTime?> = newProperty()
    val DUE_TIME: Property<Task, DateTime?> = newProperty()
    val START_DATE: Property<Task, DateTime?> = newProperty()
    val REMINDER_LAST: Property<Task, DateTime?> = newProperty()
    val HIDE_TYPE: Property<Task, Int> = newProperty()
    val MODIFICATION_TIME: Property<Task, DateTime> = newProperty()
    val CREATION_TIME: Property<Task, DateTime> = newProperty()
    val COMPLETION_TIME: Property<Task, DateTime> = newProperty()
    val DELETION_TIME: Property<Task, DateTime?> = newProperty()
    val RECUR: Property<Task, String?> = newProperty()
    val AFTER_COMPLETE: Property<Task, Boolean> = newProperty()
    val TITLE: Property<Task, String?> = newProperty()
    val PRIORITY: Property<Task, Int> = newProperty()
    val PARENT: Property<Task, Long> = newProperty()
    val UUID: Property<Task, String> = newProperty()
    val COLLAPSED: Property<Task, Boolean> = newProperty()
    val DESCRIPTION: Property<Task, String?> = newProperty()
    val ORDER: Property<Task, Long> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Task> ->
        val creationTime = lookup.valueOf(CREATION_TIME, DateTimeUtils.newDateTime())
        val task = Task(
            id = lookup.valueOf(ID, Task.NO_ID),
            title = lookup.valueOf(TITLE, null as String?),
            priority = lookup.valueOf(PRIORITY, Task.Priority.NONE),
            dueDate = lookup.valueOf(DUE_DATE, null as DateTime?)
                ?.let { createDueDate(Task.URGENCY_SPECIFIC_DAY, it.millis) }
                ?: lookup.valueOf(DUE_TIME, null as DateTime?)
                ?.let { createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, it.millis) }
                ?: 0L,
            completionDate = lookup.valueOf(COMPLETION_TIME, null as DateTime?)?.millis ?: 0L,
            deletionDate = lookup.valueOf(DELETION_TIME, null as DateTime?)?.millis ?: 0L,
            reminderLast = lookup.valueOf(REMINDER_LAST, null as DateTime?)?.millis ?: 0L,
            recurrence = lookup.valueOf(RECUR, null as String?)?.let { newRecur(it).toString() },
            repeatFrom = if (lookup.valueOf(AFTER_COMPLETE, false))
                Task.RepeatFrom.COMPLETION_DATE
            else
                Task.RepeatFrom.DUE_DATE,
            notes = lookup.valueOf(DESCRIPTION, null as String?),
            isCollapsed = lookup.valueOf(COLLAPSED, false),
            remoteId = lookup.valueOf(UUID, NO_UUID),
            parent = lookup.valueOf(PARENT, 0L),
            order = lookup.valueOf(ORDER, null as Long?),
            creationDate = creationTime.millis,
            modificationDate = lookup.valueOf(MODIFICATION_TIME, creationTime).millis,
        )
        lookup.valueOf(START_DATE, null as DateTime?)?.let {
            task.hideUntil = task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY, it.millis)
        }
        val hideType = lookup.valueOf(HIDE_TYPE, -1)
        if (hideType >= 0) {
            task.hideUntil = task.createHideUntil(hideType, 0)
        }
        task
    }

    fun newTask(vararg properties: PropertyValue<in Task?, *>): Task {
        return make(instantiator, *properties)
    }
}