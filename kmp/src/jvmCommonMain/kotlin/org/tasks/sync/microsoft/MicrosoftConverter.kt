package org.tasks.sync.microsoft

import org.tasks.data.createDueDate
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceDayOfWeek
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceType
import org.tasks.repeats.ByDay
import org.tasks.repeats.Frequency
import org.tasks.repeats.Recur
import org.tasks.repeats.Weekday
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

object MicrosoftConverter {

    private const val TYPE_TEXT = "text"
    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS0000"
    private const val DATE_TIME_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS0000'Z'"

    fun Task.applySubtask(
        parent: Long,
        parentCompletionDate: Long,
        checklistItem: Tasks.Task.ChecklistItem,
    ) {
        this.parent = parent
        title = checklistItem.displayName
        completionDate = if (checklistItem.isChecked) {
            checklistItem.checkedDateTime.parseDateTime()
        } else {
            parentCompletionDate
        }
        creationDate = checklistItem.createdDateTime.parseDateTime()
    }

    fun Task.applyRemote(
        remote: Tasks.Task,
        defaultPriority: Int,
    ) {
        title = remote.title
        notes = remote.body?.content?.takeIf { remote.body.contentType == "text" && it.isNotBlank() }
        priority = when {
            remote.importance == Tasks.Task.Importance.high -> Task.Priority.HIGH
            priority != Task.Priority.HIGH -> priority
            defaultPriority != Task.Priority.HIGH -> defaultPriority
            else -> Task.Priority.NONE
        }
        completionDate = remote.completedDateTime.toLong(currentTimeMillis())
        remote.dueDateTime.toLong(0L).let {
            if (it > 0 && hasDueTime()) {
                val oldDate = DateTimeUtils.newDateTime(dueDate)
                val newDate = DateTimeUtils.newDateTime(it)
                        .withHourOfDay(oldDate.hourOfDay)
                        .withMinuteOfHour(oldDate.minuteOfHour)
                        .withSecondOfMinute(oldDate.secondOfMinute)
                setDueDateAdjustingHideUntil(
                        createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDate.millis)
                )
            } else {
                setDueDateAdjustingHideUntil(it)
            }
        }
        creationDate = remote.createdDateTime.parseDateTime()
        modificationDate = remote.lastModifiedDateTime.parseDateTime()
        recurrence = remote.recurrence?.let { recurrence ->
            val pattern = recurrence.pattern
            val frequency = when (pattern.type) {
                RecurrenceType.daily -> Frequency.DAILY
                RecurrenceType.weekly -> Frequency.WEEKLY
                RecurrenceType.absoluteMonthly -> Frequency.MONTHLY
                RecurrenceType.absoluteYearly -> Frequency.YEARLY
                else -> return@let null
            }
            val byDay = pattern.daysOfWeek.mapNotNull {
                when (it) {
                    RecurrenceDayOfWeek.sunday -> Weekday.SU
                    RecurrenceDayOfWeek.monday -> Weekday.MO
                    RecurrenceDayOfWeek.tuesday -> Weekday.TU
                    RecurrenceDayOfWeek.wednesday -> Weekday.WE
                    RecurrenceDayOfWeek.thursday -> Weekday.TH
                    RecurrenceDayOfWeek.friday -> Weekday.FR
                    RecurrenceDayOfWeek.saturday -> Weekday.SA
                }
            }
            Recur(
                frequency = frequency,
                interval = pattern.interval.takeIf { it > 1 },
                byDay = byDay.map { ByDay(it) },
            ).toString()
        }
        // sync reminders
        // sync files
    }

    fun Task.toRemote(
        caldavTask: CaldavTask,
        tags: List<TagData>,
    ): Tasks.Task {
        return Tasks.Task(
            id = caldavTask.remoteId,
            title = title,
            body = notes?.let {
                Tasks.Task.Body(
                    content = it,
                    contentType = TYPE_TEXT,
                )
            },
            importance = when (priority) {
                Task.Priority.HIGH -> Tasks.Task.Importance.high
                Task.Priority.MEDIUM -> Tasks.Task.Importance.normal
                else -> Tasks.Task.Importance.low
            },
            status = if (isCompleted) {
                Tasks.Task.Status.completed
            } else {
                Tasks.Task.Status.notStarted
            },
            categories = tags.map { it.name!! }.takeIf { it.isNotEmpty() } ?: emptyList(),
            dueDateTime = if (hasDueDate()) {
                Tasks.Task.DateTime(
                        dateTime = DateTime(dueDate).startOfDay().toString(DATE_TIME_FORMAT),
                        timeZone = TimeZone.getDefault().id
                )
            } else if (isRecurring) { // fallback: recurring task must have due date
                Tasks.Task.DateTime(
                        dateTime = DateTime().startOfDay().toString(DATE_TIME_FORMAT),
                        timeZone = TimeZone.getDefault().id
                )
            } else {
                null
            },
            lastModifiedDateTime = DateTime(modificationDate).toUTC().toString(DATE_TIME_UTC_FORMAT),
            createdDateTime = DateTime(creationDate).toUTC().toString(DATE_TIME_UTC_FORMAT),
            completedDateTime = if (isCompleted) {
                Tasks.Task.DateTime(
                    dateTime = DateTime(completionDate).toString(DATE_TIME_FORMAT),
                    timeZone = TimeZone.getDefault().id,
                )
            } else {
                null
            },
            recurrence = if (isRecurring) {
                val recur = Recur.parse(recurrence!!)
                when (recur.frequency) {
                    Frequency.DAILY -> RecurrenceType.daily
                    Frequency.WEEKLY -> RecurrenceType.weekly
                    Frequency.MONTHLY -> RecurrenceType.absoluteMonthly
                    Frequency.YEARLY -> RecurrenceType.absoluteYearly
                    else -> null
                }?.let { frequency ->
                    val dueDateTime = if (hasDueDate()) DateTime(dueDate) else DateTime()
                    Tasks.Task.Recurrence(
                        pattern = Tasks.Task.Pattern(
                            type = frequency,
                            interval = (recur.interval ?: 1).coerceAtLeast(1),
                            daysOfWeek = recur.byDay.mapNotNull {
                                when (it.takeIf { it.offset == 0 }?.day) {
                                    Weekday.SU -> RecurrenceDayOfWeek.sunday
                                    Weekday.MO -> RecurrenceDayOfWeek.monday
                                    Weekday.TU -> RecurrenceDayOfWeek.tuesday
                                    Weekday.WE -> RecurrenceDayOfWeek.wednesday
                                    Weekday.TH -> RecurrenceDayOfWeek.thursday
                                    Weekday.FR -> RecurrenceDayOfWeek.friday
                                    Weekday.SA -> RecurrenceDayOfWeek.saturday
                                    null -> null
                                }
                            },
                            month = when (frequency) {
                                RecurrenceType.absoluteYearly -> dueDateTime.monthOfYear
                                else -> 0
                            },
                            dayOfMonth = when (frequency) {
                                RecurrenceType.absoluteYearly,
                                RecurrenceType.absoluteMonthly -> dueDateTime.dayOfMonth
                                else -> 0
                            }
                        ),
                    )
                }
            } else {
                null
            },
//            isReminderOn =
            // reminders
            // files
        )
    }

    fun Task.toChecklistItem(id: String?) =
        Tasks.Task.ChecklistItem(
            id = id,
            displayName = title ?: "",
            createdDateTime = DateTime(creationDate).toUTC().toString(DATE_TIME_UTC_FORMAT),
            isChecked = isCompleted,
            checkedDateTime = if (isCompleted) {
                DateTime(completionDate).toUTC().toString(DATE_TIME_UTC_FORMAT)
            } else {
                null
            },
        )

    private fun String?.parseDateTime(): Long =
        this
            ?.let { ZonedDateTime.parse(this).toInstant().toEpochMilli() }
            ?: currentTimeMillis()

    private fun Tasks.Task.DateTime?.toLong(default: Long): Long =
        this
            ?.let { task ->
                val tz = TimeZone.getTimeZone(task.timeZone)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ssssss", Locale.US)
                    .apply { timeZone = tz }
                    .parse(task.dateTime)
                    ?.time
                    ?: default
            }
            ?: 0L
}
