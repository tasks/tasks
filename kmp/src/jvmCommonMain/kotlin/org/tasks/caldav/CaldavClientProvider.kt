package org.tasks.caldav

import org.tasks.data.entity.CaldavAccount

interface CaldavClientProvider {
    suspend fun forUrl(
        url: String?,
        username: String? = null,
        password: String? = null,
    ): CaldavClient

    suspend fun forAccount(
        account: CaldavAccount,
        url: String? = account.url,
    ): CaldavClient

    suspend fun forTasksAccount(account: CaldavAccount): TasksClient
}
