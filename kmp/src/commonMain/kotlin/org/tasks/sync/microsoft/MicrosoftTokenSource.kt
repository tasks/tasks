package org.tasks.sync.microsoft

import org.tasks.auth.OAuthTokenStore
import org.tasks.data.entity.CaldavAccount

interface MicrosoftTokenSource {
    suspend fun getToken(account: CaldavAccount): String

    suspend fun refreshToken(account: CaldavAccount, rejected: String?): String?

    suspend fun hasCredentials(account: CaldavAccount): Boolean = !account.password.isNullOrBlank()
}

fun OAuthTokenStore.asMicrosoftTokenSource(): MicrosoftTokenSource = object : MicrosoftTokenSource {
    override suspend fun getToken(account: CaldavAccount) =
        this@asMicrosoftTokenSource.getToken(account)

    override suspend fun refreshToken(account: CaldavAccount, rejected: String?) =
        this@asMicrosoftTokenSource.getToken(account, rejected)
}
