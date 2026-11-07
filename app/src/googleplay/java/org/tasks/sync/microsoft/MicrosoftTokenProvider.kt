package org.tasks.sync.microsoft

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MicrosoftTokenProvider @Inject constructor(
    @ApplicationContext context: Context,
) : MicrosoftTokenSource {
    private val msal = MsalClient(context, R.raw.microsoft_config)

    override suspend fun hasCredentials(account: CaldavAccount): Boolean = msal.hasCredentials(account)

    override suspend fun getToken(account: CaldavAccount): String = msal.getToken(account)

    override suspend fun refreshToken(account: CaldavAccount, rejected: String?): String =
        msal.refreshToken(account, rejected)

    suspend fun signIn(activity: android.app.Activity, username: String?): String =
        msal.signIn(activity, username)
}
