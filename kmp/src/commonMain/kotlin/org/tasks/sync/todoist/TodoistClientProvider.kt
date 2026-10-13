package org.tasks.sync.todoist

import org.tasks.data.entity.CaldavAccount

interface TodoistClientProvider {
    suspend fun getService(account: CaldavAccount): TodoistService

    suspend fun getService(token: String): TodoistService

    suspend fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()
}
