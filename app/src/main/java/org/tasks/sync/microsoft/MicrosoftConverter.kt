package org.tasks.sync.microsoft

import org.tasks.data.entity.Task
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.WeekDayList
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceDayOfWeek
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceType
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
        dueDate = remote.dueDateTime.toLong(0L)
        creationDate = remote.createdDateTime.parseDateTime()
        modificationDate = remote.lastModifiedDateTime.parseDateTime()
        recurrence = remote.recurrence?.let { recurrence ->
            val pattern = recurrence.pattern
            val frequency = when (pattern.type) {
                RecurrenceType.daily -> Recur.Frequency.DAILY
                RecurrenceType.weekly -> Recur.Frequency.WEEKLY
                RecurrenceType.absoluteMonthly -> Recur.Frequency.MONTHLY
                RecurrenceType.absoluteYearly -> Recur.Frequency.YEARLY
                else -> return@let null
            }
            val dayList = pattern.daysOfWeek.mapNotNull {
                when (it) {
                    RecurrenceDayOfWeek.sunday -> WeekDay.SU
                    RecurrenceDayOfWeek.monday -> WeekDay.MO
                    RecurrenceDayOfWeek.tuesday -> WeekDay.TU
                    RecurrenceDayOfWeek.wednesday -> WeekDay.WE
                    RecurrenceDayOfWeek.thursday -> WeekDay.TH
                    RecurrenceDayOfWeek.friday -> WeekDay.FR
                    RecurrenceDayOfWeek.saturday -> WeekDay.SA
                }
            }
            Recur.Builder()
                .frequency(frequency)
                .interval(pattern.interval.takeIf { it > 1 })
                .dayList(WeekDayList(*dayList.toTypedArray()))
                .build()
                .toString()
        }
        // checklist to subtasks
        // sync reminders
        // sync files
    }

    fun Task.toRemote(caldavTask: CaldavTask, tags: List<TagData>): Tasks.Task {
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
            categories = tags.map { it.name!! }.takeIf { it.isNotEmpty() },
            dueDateTime = if (hasDueDate()) {
                Tasks.Task.DateTime(
                        dateTime = DateTime(dueDate).startOfDay().toUTC().toString(DATE_TIME_FORMAT),
                        timeZone = "UTC"
                )
            } else if (isRecurring) {
                Tasks.Task.DateTime(
                        dateTime = DateTime().startOfDay().toUTC().toString(DATE_TIME_FORMAT),
                        timeZone = "UTC"
                )
            } else {
                null
            },
            lastModifiedDateTime = DateTime(modificationDate).toUTC().toString(DATE_TIME_UTC_FORMAT),
            createdDateTime = DateTime(creationDate).toUTC().toString(DATE_TIME_UTC_FORMAT),
            completedDateTime = if (isCompleted) {
                Tasks.Task.DateTime(
                    dateTime = DateTime(completionDate).toUTC().toString(DATE_TIME_FORMAT),
                    timeZone = "UTC",
                )
            } else {
                null
            },
            recurrence = if (isRecurring) {
                val recur = Recur(recurrence)
                when (recur.frequency) {
                    Recur.Frequency.DAILY -> RecurrenceType.daily
                    Recur.Frequency.WEEKLY -> RecurrenceType.weekly
                    Recur.Frequency.MONTHLY -> RecurrenceType.absoluteMonthly
                    Recur.Frequency.YEARLY -> RecurrenceType.absoluteYearly
                    else -> null
                }?.let { frequency ->
                    val dueDateTime = if (hasDueDate()) DateTime(dueDate) else DateTime()
                    Tasks.Task.Recurrence(
                        pattern = Tasks.Task.Pattern(
                            type = frequency,
                            interval = recur.interval.coerceAtLeast(1),
                            daysOfWeek = recur.dayList.mapNotNull {
                                when (it) {
                                    WeekDay.SU -> RecurrenceDayOfWeek.sunday
                                    WeekDay.MO -> RecurrenceDayOfWeek.monday
                                    WeekDay.TU -> RecurrenceDayOfWeek.tuesday
                                    WeekDay.WE -> RecurrenceDayOfWeek.wednesday
                                    WeekDay.TH -> RecurrenceDayOfWeek.thursday
                                    WeekDay.FR -> RecurrenceDayOfWeek.friday
                                    WeekDay.SA -> RecurrenceDayOfWeek.saturday
                                    else -> null
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
            }
            // subtasks to checklist
//            isReminderOn =
            // reminders
            // files
        )
    }

    private fun String?.parseDateTime(): Long =
        this
            ?.let { ZonedDateTime.parse(this).toInstant().toEpochMilli() }
            ?: currentTimeMillis()

    private fun Tasks.Task.DateTime?.toLong(default: Long): Long =
        this
            ?.let {
                val tz = TimeZone.getTimeZone(it.timeZone)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ssssss", Locale.US)
                    .apply { timeZone = tz }
                    .parse(it.dateTime)
                    ?.time
                    ?.let { ts -> DateTime(ts, tz).toLocal().millis }
                    ?: default
            }
            ?: 0L
}