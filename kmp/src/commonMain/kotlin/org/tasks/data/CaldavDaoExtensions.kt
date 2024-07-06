package org.tasks.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.default_list

private val mutex = Mutex()

suspend fun CaldavDao.setupLocalAccount(): CaldavAccount = mutex.withLock {
    val account = getLocalAccount()
    getLocalList(account)
    return account
}

suspend fun CaldavDao.getLocalList() = mutex.withLock {
    getLocalList(getLocalAccount())
}

private suspend fun CaldavDao.getLocalAccount() = getAccountByUuid(CaldavDao.LOCAL) ?: CaldavAccount(
    accountType = CaldavAccount.TYPE_LOCAL,
    uuid = CaldavDao.LOCAL,
).let {
    it.copy(id = insert(it))
}

private suspend fun CaldavDao.getLocalList(account: CaldavAccount): CaldavCalendar =
    getCalendarsByAccount(account.uuid!!).getOrNull(0)
        ?: CaldavCalendar(
            name = getString(Res.string.default_list),
            uuid = UUIDHelper.newUUID(),
            account = account.uuid,
        ).apply {
            insert(this)
        }