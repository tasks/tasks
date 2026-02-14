@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.dao.UpgraderDao
import org.tasks.preferences.Preferences
import javax.inject.Inject

class Upgrade_14_13 @Inject constructor(
    private val preferences: Preferences,
    private val upgraderDao: UpgraderDao,
) {
    internal suspend fun deleteAlarmsForAllDayTasks() {
        if (!preferences.isDefaultDueTimeEnabled) {
            upgraderDao.deleteAlarmsForAllDayTasks()
        }
    }

    companion object {
        const val VERSION = 141300
    }
}
