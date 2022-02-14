package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyLookup
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import com.todoroo.astrid.data.Task.Companion.NO_UUID
import org.tasks.Strings
import org.tasks.date.DateTimeUtils
import org.tasks.makers.Maker.make
import org.tasks.time.DateTime

object TaskMaker {
    val ID: Property<Task, Long> = newProperty()
    val DUE_DATE: Property<Task, DateTime?> = newProperty()
    val DUE_TIME: Property<Task, DateTime?> = newProperty()
    val START_DATE: Property<Task, DateTime?> = newProperty()
    val REMINDER_LAST: Property<Task, DateTime?> = newProperty()
    val HIDE_TYPE: Property<Task, Int> = newProperty()
    val REMINDERS: Property<Task, Int> = newProperty()
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
        lookup.valueOf(START_DATE, null as DateTime?)?.let {
            task.hideUntil = task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY, it.millis)
        }
        val hideType = lookup.valueOf(HIDE_TYPE, -1)
        if (hideType >= 0) {
            task.hideUntil = task.createHideUntil(hideType, 0)
        }
        val reminderFlags = lookup.valueOf(REMINDERS, -1)
        if (reminderFlags >= 0) {
            task.ringFlags = reminderFlags
        }
        val reminderLast = lookup.valueOf(REMINDER_LAST, null as DateTime?)
        if (reminderLast != null) {
            task.reminderLast = reminderLast.millis
        }
        lookup.valueOf(RECUR, null as String?)?.let {
            task.setRecurrence(it, lookup.valueOf(AFTER_COMPLETE, false))
        }
        task.notes = lookup.valueOf(DESCRIPTION, null as String?)
        task.isCollapsed = lookup.valueOf(COLLAPSED, false)
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