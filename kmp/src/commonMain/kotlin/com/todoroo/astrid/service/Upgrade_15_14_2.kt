@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.service.Upgrade

class Upgrade_15_14_2(
    private val caldavDao: CaldavDao,
) : Upgrade {
    override suspend fun run() = downloadAgain()

    suspend fun downloadAgain() {
        caldavDao.clearCTags(ACCOUNT_TYPES)
    }

    companion object {
        const val VERSION = 151412

        private val ACCOUNT_TYPES = listOf(TYPE_MICROSOFT)
    }
}
