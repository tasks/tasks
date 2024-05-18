@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.reminders
import org.tasks.data.CaldavTaskContainer
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.UpgraderDao
import javax.inject.Inject

class Upgrade_12_4 @Inject constructor(
    private val alarmDao: AlarmDao,
    private val taskDao: TaskDao,
    private val upgraderDao: UpgraderDao,
    private val vtodoCache: VtodoCache,
) {
    internal suspend fun syncExistingAlarms() {
        val existingAlarms = alarmDao.getActiveAlarms()
        upgraderDao.tasksWithVtodos().map(CaldavTaskContainer::caldavTask).forEach { task ->
            val remoteTask =
                vtodoCache.getVtodo(task)?.let(iCalendar::fromVtodo) ?: return@forEach
            remoteTask
                .reminders
                .filter { existingAlarms.find { e -> e.task == task.task && e.same(it) } == null }
                .map { it.copy(task = task.task) }
                .let { alarmDao.insert(it) }
        }
        taskDao.touch(existingAlarms.map { it.task }.toSet().toList())
    }

    companion object {
        const val VERSION = 120400
    }
}