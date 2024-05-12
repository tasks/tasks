package org.tasks.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

private val mutex = Mutex()

suspend fun CaldavDao.setupLocalAccount(context: Context): CaldavAccount = mutex.withLock {
    val account = getLocalAccount()
    getLocalList(context, account)
    return account
}

suspend fun CaldavDao.getLocalList(context: Context) = mutex.withLock {
    getLocalList(context, getLocalAccount())
}

private suspend fun CaldavDao.getLocalAccount() = getAccountByUuid(CaldavDao.LOCAL) ?: CaldavAccount(
    accountType = CaldavAccount.TYPE_LOCAL,
    uuid = CaldavDao.LOCAL,
).let {
    it.copy(id = insert(it))
}

private suspend fun CaldavDao.getLocalList(context: Context, account: CaldavAccount): CaldavCalendar =
    getCalendarsByAccount(account.uuid!!).getOrNull(0)
        ?: CaldavCalendar(
            name = context.getString(R.string.default_list),
            uuid = UUIDHelper.newUUID(),
            account = account.uuid,
        ).apply {
            insert(this)
        }