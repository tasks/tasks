package org.tasks.sync.todoist

import org.tasks.data.entity.CaldavAccount

interface TodoistClientProvider {
    suspend fun getService(account: CaldavAccount): TodoistService

    /**
     * A service backed by a raw, not-yet-stored token, for validating what the user
     * pasted before an account row exists.
     */
    suspend fun getService(token: String): TodoistService

    suspend fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()
}
