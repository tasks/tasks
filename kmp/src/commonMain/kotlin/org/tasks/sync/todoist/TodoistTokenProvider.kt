package org.tasks.sync.todoist

import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import org.tasks.security.Encryption

class TodoistTokenProvider(
    private val encryption: Encryption,
) {
    fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()

    suspend fun getToken(account: CaldavAccount): String =
        encryption.decrypt(account.password)?.takeIf { it.isNotBlank() }
            ?: throw UnauthorizedException("Missing Todoist credentials")
}
