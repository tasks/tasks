@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.entity.Task
import org.tasks.repeats.Frequency
import org.tasks.repeats.RecurrenceUtils.LAST_DAY_OF_MONTH
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.service.Upgrade
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class Upgrade_15_11(
    private val upgraderDao: UpgraderDao,
    private val dirtyDao: DirtyDao,
) : Upgrade {
    override suspend fun run() = migrateLastDayOfMonthRecurrence()

    suspend fun migrateLastDayOfMonthRecurrence() {
        val migrated = upgraderDao
            .monthlyRecurringTasksWithDueDate()
            .mapNotNull { task -> task.lastDayOfMonthRecurrence()?.let { task.id to it } }
        if (migrated.isEmpty()) {
            return
        }
        upgraderDao.setRecurrence(migrated, currentTimeMillis())
        dirtyDao.setDirty(migrated.map { it.first })
    }

    private fun Task.lastDayOfMonthRecurrence(): String? {
        if (!DateTime(dueDate).isLastDayOfMonth) {
            return null
        }
        val recur = try {
            newRecur(recurrence ?: return null)
        } catch (e: Exception) {
            return null
        }
        if (recur.frequency != Frequency.MONTHLY ||
            recur.byDay.isNotEmpty() ||
            recur.byMonthDay.isNotEmpty()
        ) {
            return null
        }
        return recur.copy(byMonthDay = listOf(LAST_DAY_OF_MONTH)).toString()
    }

    companion object {
        const val VERSION = 151100
    }
}
