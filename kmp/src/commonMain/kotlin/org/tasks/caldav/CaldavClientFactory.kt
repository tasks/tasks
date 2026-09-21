package org.tasks.caldav

import org.tasks.data.entity.CaldavAccount

interface CaldavClientFactory {
    suspend fun forUrl(url: String?, username: String?, password: String?): CaldavDiscoveryClient
    suspend fun forAccount(account: CaldavAccount, url: String? = account.url): CaldavCollectionClient
    suspend fun forTasksAccount(account: CaldavAccount): TasksAccountClient
}
