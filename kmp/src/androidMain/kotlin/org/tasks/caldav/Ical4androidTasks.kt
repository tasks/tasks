package org.tasks.caldav

import org.tasks.icalendar.VTodo
import org.tasks.icalendar.toTask
import org.tasks.icalendar.toVTodo
import java.util.LinkedList
import at.bitfire.ical4android.Task as Ical4androidTask

fun Ical4androidTask.toVTodo(): VTodo = toTasksTask().toVTodo()

fun VTodo.toIcal4android(): Ical4androidTask = toTask().toIcal4android()

private fun Ical4androidTask.toTasksTask(): Task = Task(
    uid = uid,
    sequence = sequence,
    createdAt = createdAt,
    lastModified = lastModified,
    summary = summary,
    location = location,
    geoPosition = geoPosition,
    description = description,
    color = color,
    url = url,
    organizer = organizer,
    priority = priority,
    classification = classification,
    status = status,
    dtStart = dtStart,
    due = due,
    duration = duration,
    completedAt = completedAt,
    percentComplete = percentComplete,
    rRule = rRule,
    rDates = LinkedList(rDates),
    exDates = LinkedList(exDates),
    categories = LinkedList(categories),
    comment = comment,
    relatedTo = LinkedList(relatedTo),
    unknownProperties = LinkedList(unknownProperties),
    alarms = LinkedList(alarms),
).also { it.repairDates() }

private fun Task.toIcal4android(): Ical4androidTask = Ical4androidTask().also {
    it.uid = uid
    it.sequence = sequence
    it.createdAt = createdAt
    it.lastModified = lastModified
    it.summary = summary
    it.location = location
    it.geoPosition = geoPosition
    it.description = description
    it.color = color
    it.url = url
    it.organizer = organizer
    it.priority = priority
    it.classification = classification
    it.status = status
    it.dtStart = dtStart
    it.due = due
    it.duration = duration
    it.completedAt = completedAt
    it.percentComplete = percentComplete
    it.rRule = rRule
    it.rDates.addAll(rDates)
    it.exDates.addAll(exDates)
    it.categories.addAll(categories)
    it.comment = comment
    it.relatedTo.addAll(relatedTo)
    it.unknownProperties.addAll(unknownProperties)
    it.alarms.addAll(alarms)
}
