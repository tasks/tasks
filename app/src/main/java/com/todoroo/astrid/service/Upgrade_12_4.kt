@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.reminders
import org.tasks.data.AlarmDao
import org.tasks.data.TaskDao
import org.tasks.data.UpgraderDao
import javax.inject.Inject

class Upgrade_12_4 @Inject constructor(
    private val alarmDao: AlarmDao,
    private val taskDao: TaskDao,
    private val upgraderDao: UpgraderDao,
) {
    internal suspend fun syncExistingAlarms() {
        val existingAlarms = alarmDao.getActiveAlarms()
        upgraderDao.tasksWithVtodos().forEach { caldav ->
            val remoteTask = caldav.vtodo?.let(iCalendar::fromVtodo) ?: return@forEach
            remoteTask
                .reminders
                .filter { existingAlarms.find { e -> e.task == caldav.id && e.same(it) } == null }
                .onEach { it.task = caldav.id }
                .let { alarmDao.insert(it) }
        }
        taskDao.touch(existingAlarms.map { it.task }.toSet().toList())
    }

    companion object {
        const val VERSION = 120400
    }
}