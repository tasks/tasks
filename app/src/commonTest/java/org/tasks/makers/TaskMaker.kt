package org.tasks.makers

import com.google.ical.values.RRule
import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NO_UUID
import org.tasks.Strings
import org.tasks.date.DateTimeUtils
import org.tasks.makers.Maker.make
import org.tasks.time.DateTime

object TaskMaker {
    val ID: Property<Task, Long> = newProperty()
    val DUE_DATE: Property<Task, DateTime?> = newProperty()
    val DUE_TIME: Property<Task, DateTime?> = newProperty()
    val REMINDER_LAST: Property<Task, DateTime?> = newProperty()
    val RANDOM_REMINDER_PERIOD: Property<Task, Long> = newProperty()
    val HIDE_TYPE: Property<Task, Int> = newProperty()
    val REMINDERS: Property<Task, Int> = newProperty()
    val MODIFICATION_TIME: Property<Task, DateTime> = newProperty()
    val CREATION_TIME: Property<Task, DateTime> = newProperty()
    val COMPLETION_TIME: Property<Task, DateTime> = newProperty()
    val DELETION_TIME: Property<Task, DateTime?> = newProperty()
    val SNOOZE_TIME: Property<Task, DateTime?> = newProperty()
    val RRULE: Property<Task, RRule?> = newProperty()
    val AFTER_COMPLETE: Property<Task, Boolean> = newProperty()
    val TITLE: Property<Task, String?> = newProperty()
    val PRIORITY: Property<Task, Int> = newProperty()
    val PARENT: Property<Task, Long> = newProperty()
    val UUID: Property<Task, String> = newProperty()

    private val instantiator = Instantiator { lookup: PropertyLookup<Task> ->
        val task = Task()
        val title = lookup.valueOf(TITLE, null as String?)
        if (!Strings.isNullOrEmpty(title)) {
            task.title = title!!
        }
        val id = lookup.valueOf(ID, Task.NO_ID)
        if (id != Task.NO_ID) {
            task.id = id
        }
        val priority = lookup.valueOf(PRIORITY, -1)
        if (priority >= 0) {
            task.priority = priority
        }
        val dueDate = lookup.valueOf(DUE_DATE, null as DateTime?)
        if (dueDate != null) {
            task.dueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate.millis)
        }
        val dueTime = lookup.valueOf(DUE_TIME, null as DateTime?)
        if (dueTime != null) {
            task.dueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueTime.millis)
        }
        val completionTime = lookup.valueOf(COMPLETION_TIME, null as DateTime?)
        if (completionTime != null) {
            task.completionDate = completionTime.millis
        }
        val deletedTime = lookup.valueOf(DELETION_TIME, null as DateTime?)
        if (deletedTime != null) {
            task.deletionDate = deletedTime.millis
        }
        val snoozeTime = lookup.valueOf(SNOOZE_TIME, null as DateTime?)
        if (snoozeTime != null) {
            task.reminderSnooze = snoozeTime.millis
        }
        val hideType = lookup.valueOf(HIDE_TYPE, -1)
        if (hideType >= 0) {
            task.hideUntil = task.createHideUntil(hideType, 0)
        }
        val reminderFlags = lookup.valueOf(REMINDERS, -1)
        if (reminderFlags >= 0) {
            task.reminderFlags = reminderFlags
        }
        val reminderLast = lookup.valueOf(REMINDER_LAST, null as DateTime?)
        if (reminderLast != null) {
            task.reminderLast = reminderLast.millis
        }
        val randomReminderPeriod = lookup.valueOf(RANDOM_REMINDER_PERIOD, 0L)
        if (randomReminderPeriod > 0) {
            task.reminderPeriod = randomReminderPeriod
        }
        val rrule = lookup.valueOf(RRULE, null as RRule?)
        if (rrule != null) {
            task.setRecurrence(rrule, lookup.valueOf(AFTER_COMPLETE, false))
        }
        task.uuid = lookup.valueOf(UUID, NO_UUID)
        val creationTime = lookup.valueOf(CREATION_TIME, DateTimeUtils.newDateTime())
        task.creationDate = creationTime.millis
        task.modificationDate = lookup.valueOf(MODIFICATION_TIME, creationTime).millis
        task.parent = lookup.valueOf(PARENT, 0L)
        task
    }

    fun newTask(vararg properties: PropertyValue<in Task?, *>): Task {
        return make(instantiator, *properties)
    }
}