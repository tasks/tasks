package org.tasks.time

import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis

fun dueDateOverdue(dueDate: Long): Boolean = when {
    Task.hasDueTime(dueDate) -> dueDate < currentTimeMillis()
    dueDate > 0 -> dueDate.endOfDay() < currentTimeMillis()
    else -> false
}
