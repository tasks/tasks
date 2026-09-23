@file:Suppress("ClassName")

package com.todoroo.astrid.service

import co.touchlab.kermit.Logger
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
        val accounts = caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS)
        val accountUrls = accounts.mapNotNullTo(mutableSetOf()) { it.url }
        for (account in accounts) {
            account
                .canonicalized()
                .takeIf { it.url != account.url && accountUrls.claim(account.url, it.url!!) }
                ?.let { caldavDao.update(it) }
            val calendars = caldavDao.getCalendarsByAccount(account.uuid!!)
            val calendarUrls = calendars.mapNotNullTo(mutableSetOf()) { it.url }
            for (calendar in calendars) {
                calendar
                    .canonicalized(account)
                    .takeIf { it.url != calendar.url && calendarUrls.claim(calendar.url, it.url!!) }
                    ?.let { caldavDao.update(it) }
            }
        }
    }

    companion object {
        const val VERSION = 151300

        private fun MutableSet<String>.claim(old: String?, canonical: String): Boolean =
            if (add(canonical)) {
                old?.let { remove(it) }
                true
            } else {
                Logger.w(tag = "Upgrade_15_13") { "not canonicalizing $old: $canonical already exists" }
                false
            }

        private val CaldavAccount.hasCaldavUrl: Boolean
            get() = accountType == TYPE_CALDAV || accountType == TYPE_TASKS

        fun CaldavAccount.canonicalized(): CaldavAccount =
            if (hasCaldavUrl) copy(url = url?.canonicalUrl()) else this

        fun CaldavCalendar.canonicalized(account: CaldavAccount?): CaldavCalendar =
            if (account?.hasCaldavUrl == true) copy(url = url?.canonicalUrl()) else this
    }
}
