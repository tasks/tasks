package org.tasks.sync.todoist

import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import org.tasks.security.Encryption

/**
 * Decrypts the stored Todoist bearer token.
 *
 * Unlike Microsoft there is no refresh step: a personal API token does not expire, so
 * this is a straight decrypt. If OAuth is added later, the refresh belongs here, and
 * must persist the rotated refresh token under `NonCancellable` -- Todoist rotates it
 * on every use, so losing the new one locks the account out.
 */
class TodoistTokenProvider(
    private val encryption: Encryption,
) {
    fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()

    suspend fun getToken(account: CaldavAccount): String =
        encryption.decrypt(account.password)?.takeIf { it.isNotBlank() }
            ?: throw UnauthorizedException("Missing Todoist credentials")
}
