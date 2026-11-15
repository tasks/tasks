@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.service.Upgrade

class Upgrade_15_14_1(
    private val caldavDao: CaldavDao,
) : Upgrade {
    override suspend fun run() = downloadAgain()

    suspend fun downloadAgain() {
        caldavDao.clearETags(ACCOUNT_TYPES)
        caldavDao.replaceCTags(ACCOUNT_TYPES, CTAG)
    }

    companion object {
        const val VERSION = 151405
        const val CTAG = "download-again"

        private val ACCOUNT_TYPES = listOf(TYPE_CALDAV, TYPE_TASKS)
    }
}
