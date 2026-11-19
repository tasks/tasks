package org.tasks.sync.microsoft

import android.app.Activity
import org.tasks.analytics.Analytics
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    private val msal: MsalClient,
) : MicrosoftTokenSource by msal {
    suspend fun signIn(activity: Activity, username: String?, analytics: Analytics): Boolean =
        msal.signIn(activity, username, analytics)
}
