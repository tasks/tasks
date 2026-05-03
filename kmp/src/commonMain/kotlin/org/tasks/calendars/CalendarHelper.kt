package org.tasks.calendars

import org.tasks.data.entity.Task

interface CalendarHelper {
    fun updateEvent(task: Task) {}
    suspend fun rescheduleRepeatingTask(task: Task) {}
}
