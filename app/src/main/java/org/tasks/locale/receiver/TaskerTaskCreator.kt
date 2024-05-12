package org.tasks.locale.receiver

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.entity.Task
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskCreator.Companion.getDefaultAlarms
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.data.dao.AlarmDao
import org.tasks.data.createDueDate
import org.tasks.locale.bundle.TaskCreationBundle
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class TaskerTaskCreator @Inject internal constructor(
    private val taskCreator: TaskCreator,
    private val taskDao: TaskDao,
    private val firebase: Firebase,
    private val alarmDao: AlarmDao,
) {
    suspend fun handle(bundle: TaskCreationBundle) {
        val task = taskCreator.basicQuickAddTask(bundle.title)
        val dueDateString = bundle.dueDate
        if (!isNullOrEmpty(dueDateString)) {
            try {
                val dueDate = LocalDate.parse(dueDateString, dateFormatter)
                val dt = DateTime(dueDate.year, dueDate.monthValue, dueDate.dayOfMonth)
                task.dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, dt.millis)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        val dueTimeString = bundle.dueTime
        if (!isNullOrEmpty(dueTimeString)) {
            try {
                val dueTime = LocalTime.parse(dueTimeString, timeFormatter)
                task.dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(if (task.hasDueDate()) task.dueDate else currentTimeMillis())
                                .withHourOfDay(dueTime.hour)
                                .withMinuteOfHour(dueTime.minute)
                                .millis)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        val priorityString = bundle.priority
        if (!isNullOrEmpty(priorityString)) {
            try {
                val priority = priorityString.toInt()
                task.priority = max(Task.Priority.HIGH, min(Task.Priority.NONE, priority))
            } catch (e: NumberFormatException) {
                Timber.e(e)
            }
        }
        task.notes = bundle.description
        taskDao.save(task)
        alarmDao.insert(task.getDefaultAlarms())
        taskCreator.createTags(task)
        firebase.addTask("tasker")
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    }
}