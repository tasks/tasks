package org.tasks.api

import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter

interface ListManager {
    suspend fun create(account: CaldavAccount, title: String, color: Int, icon: String): CaldavCalendar

    suspend fun update(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        title: String,
        color: Int,
        icon: String,
    ): CaldavCalendar

    suspend fun delete(account: CaldavAccount, calendar: CaldavCalendar)
}

class LocalListManager(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
) : ListManager {
    override suspend fun create(
        account: CaldavAccount,
        title: String,
        color: Int,
        icon: String,
    ): CaldavCalendar {
        requireLocal(account)
        val calendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            account = account.uuid,
            name = title,
            color = color,
            icon = icon,
        )
        caldavDao.insert(calendar)
        return caldavDao.getCalendarByUuid(calendar.uuid!!) ?: calendar
    }

    override suspend fun update(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        title: String,
        color: Int,
        icon: String,
    ): CaldavCalendar {
        requireLocal(account)
        val updated = calendar.copy(name = title, color = color, icon = icon)
        caldavDao.update(updated)
        return updated
    }

    override suspend fun delete(account: CaldavAccount, calendar: CaldavCalendar) {
        requireLocal(account)
        taskDeleter.delete(calendar)
    }

    private fun requireLocal(account: CaldavAccount) {
        if (!account.isLocalList) {
            throw UnsupportedOperationException("Lists on synced accounts can't be managed here")
        }
    }
}
