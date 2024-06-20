/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.text.format.Time
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_HOUR
import timber.log.Timber
import java.util.TimeZone
import javax.inject.Inject

class GCalHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val permissionChecker: PermissionChecker,
    private val calendarEventProvider: CalendarEventProvider) {

    private val cr: ContentResolver = context.contentResolver

    private suspend fun getTaskEventUri(task: Task) =
            if (!task.calendarURI.isNullOrBlank()) {
                task.calendarURI
            } else {
                taskDao.fetch(task.id)
                        ?.calendarURI
            }

    suspend fun createTaskEventIfEnabled(t: Task) {
        if (!t.hasDueDate()) {
            return
        }
        createTaskEventIfEnabled(t, true)
    }

    private suspend fun createTaskEventIfEnabled(t: Task, deleteEventIfExists: Boolean) {
        if (preferences.isDefaultCalendarSet) {
            val calendarUri = createTaskEvent(t, ContentValues(), deleteEventIfExists)
            if (calendarUri != null) {
                t.calendarURI = calendarUri.toString()
            }
        }
    }

    suspend fun createTaskEvent(task: Task, calendarId: String?): Uri? {
        val values = ContentValues()
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
        return createTaskEvent(task, values, true)
    }

    private suspend fun createTaskEvent(task: Task, values: ContentValues, deleteEventIfExists: Boolean): Uri? {
        if (!permissionChecker.canAccessCalendars()) {
            return null
        }
        val eventuri = getTaskEventUri(task)
        if (!isNullOrEmpty(eventuri) && deleteEventIfExists) {
            calendarEventProvider.deleteEvent(task)
        }
        try {
            values.put(CalendarContract.Events.TITLE, task.title)
            values.put(CalendarContract.Events.DESCRIPTION, task.notes)
            values.put(CalendarContract.Events.HAS_ALARM, 0)
            val valuesContainCalendarId = (values.containsKey(CalendarContract.Events.CALENDAR_ID)
                    && !isNullOrEmpty(values.getAsString(CalendarContract.Events.CALENDAR_ID)))
            if (!valuesContainCalendarId) {
                val calendarId = preferences.defaultCalendar
                if (!isNullOrEmpty(calendarId)) {
                    values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
                }
            }
            createStartAndEndDate(task, values)
            val eventUri = cr.insert(CalendarContract.Events.CONTENT_URI, values)
            cr.notifyChange(eventUri!!, null)
            return eventUri
        } catch (e: Exception) {
            // won't work on emulator
            Timber.e(e)
        }
        return null
    }

    fun updateEvent(task: Task) {
        val uri = task.calendarURI?.takeIf { it.isNotBlank() } ?: return
        if (!permissionChecker.canAccessCalendars()) {
            return
        }
        try {
            val updateValues = ContentValues()
            updateValues.put(CalendarContract.Events.TITLE, if (task.isCompleted) {
                context.getString(R.string.gcal_completed_title, task.title)
            } else {
                task.title
            })
            updateValues.put(CalendarContract.Events.DESCRIPTION, task.notes)
            createStartAndEndDate(task, updateValues)
            cr.update(Uri.parse(uri), updateValues, null, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update calendar: %s [%s]", uri, task)
        }
    }

    suspend fun rescheduleRepeatingTask(task: Task) {
        val taskUri = getTaskEventUri(task)
        if (isNullOrEmpty(taskUri)) {
            return
        }
        val eventUri = Uri.parse(taskUri)
        val event = calendarEventProvider.getEvent(eventUri)
        if (event == null) {
            task.calendarURI = ""
            return
        }
        val cv = ContentValues()
        cv.put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
        val uri = createTaskEvent(task, cv, false)
        task.calendarURI = uri.toString()
    }

    private fun createStartAndEndDate(task: Task, values: ContentValues) {
        val dueDate = task.dueDate
        val tzCorrectedDueDate = dueDate + TimeZone.getDefault().getOffset(dueDate)
        val tzCorrectedDueDateNow = currentTimeMillis() + TimeZone.getDefault().getOffset(
            currentTimeMillis()
        )
        // FIXME: doesn't respect timezones, see story 17443653
        if (task.hasDueDate()) {
            if (task.hasDueTime()) {
                var estimatedTime = task.estimatedSeconds * 1000.toLong()
                if (estimatedTime <= 0) {
                    estimatedTime = DEFAULT_CAL_TIME
                }
                if (preferences.getBoolean(R.string.p_end_at_deadline, true)) {
                    values.put(CalendarContract.Events.DTSTART, dueDate)
                    values.put(CalendarContract.Events.DTEND, dueDate + estimatedTime)
                } else {
                    values.put(CalendarContract.Events.DTSTART, dueDate - estimatedTime)
                    values.put(CalendarContract.Events.DTEND, dueDate)
                }
                // setting a duetime to a previously timeless event requires explicitly setting allDay=0
                values.put(CalendarContract.Events.ALL_DAY, "0")
                values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            } else {
                values.put(CalendarContract.Events.DTSTART, tzCorrectedDueDate)
                values.put(CalendarContract.Events.DTEND, tzCorrectedDueDate)
                values.put(CalendarContract.Events.ALL_DAY, "1")
            }
        } else {
            values.put(CalendarContract.Events.DTSTART, tzCorrectedDueDateNow)
            values.put(CalendarContract.Events.DTEND, tzCorrectedDueDateNow)
            values.put(CalendarContract.Events.ALL_DAY, "1")
        }
        if ("1" == values[CalendarContract.Events.ALL_DAY]) {
            values.put(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
        } else {
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
    }

    companion object {
        /** If task has no estimated time, how early to set a task in calendar (seconds)  */
        private const val DEFAULT_CAL_TIME = ONE_HOUR
    }
}