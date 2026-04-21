package org.tasks.service

import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class TaskMigrator(
    private val clientProvider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
    private val syncAdapters: SyncAdapters,
    private val taskDeleter: TaskDeleter,
) {
    suspend fun migrateLocalTasks(toAccount: CaldavAccount) {
        val caldavClient = clientProvider.forAccount(toAccount)
        val fromAccount = caldavDao
            .getAccounts(TYPE_LOCAL)
            .firstOrNull()
            ?: return
        caldavDao.getCalendarsByAccount(fromAccount.uuid!!).forEach {
            caldavDao.update(
                it.copy(
                    url = caldavClient.makeCollection(it.name!!, it.color, it.icon),
                    account = toAccount.uuid,
                )
            )
        }
        taskDeleter.delete(fromAccount)
        syncAdapters.sync(SyncSource.ACCOUNT_ADDED)
    }
}
