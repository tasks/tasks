package org.tasks.etebase

import org.tasks.data.entity.CaldavAccount

interface EtebaseClientFactory {
    suspend fun forAccount(account: CaldavAccount): EtebaseAccountClient
    suspend fun forUrl(
        url: String,
        username: String,
        password: String?,
        session: String? = null,
        foreground: Boolean = false,
    ): EtebaseAccountClient
}
