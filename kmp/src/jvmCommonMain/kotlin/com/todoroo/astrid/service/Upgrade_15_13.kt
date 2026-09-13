@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.caldav.canonicalUrl
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.Upgrade

class Upgrade_15_13(
    private val caldavDao: CaldavDao,
) : Upgrade {
    override suspend fun run() = canonicalizeUrls()

    suspend fun canonicalizeUrls() {
        for (account in caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS)) {
            account.canonicalized()
                .takeIf { it.url != account.url }
                ?.let { caldavDao.update(it) }
            for (calendar in caldavDao.getCalendarsByAccount(account.uuid!!)) {
                calendar.canonicalized(account)
                    .takeIf { it.url != calendar.url }
                    ?.let { caldavDao.update(it) }
            }
        }
    }

    companion object {
        const val VERSION = 151300

        private val CaldavAccount.hasCaldavUrl: Boolean
            get() = accountType == TYPE_CALDAV || accountType == TYPE_TASKS

        fun CaldavAccount.canonicalized(): CaldavAccount =
            if (hasCaldavUrl) copy(url = url?.canonicalUrl()) else this

        fun CaldavCalendar.canonicalized(account: CaldavAccount?): CaldavCalendar =
            if (account?.hasCaldavUrl == true) copy(url = url?.canonicalUrl()) else this
    }
}
