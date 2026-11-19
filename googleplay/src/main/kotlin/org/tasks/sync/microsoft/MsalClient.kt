package org.tasks.sync.microsoft

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MsalClient(
    private val context: Context,
    private val configResourceId: Int,
    private val caldavDao: CaldavDao,
) : MicrosoftTokenSource {
    private val logger = Logger.withTag(TAG)

    @Volatile private var application: IMultipleAccountPublicClientApplication? = null

    private fun application(): IMultipleAccountPublicClientApplication =
        application ?: synchronized(this) {
            application ?: PublicClientApplication
                .createMultipleAccountPublicClientApplication(context, configResourceId)
                .also { application = it }
        }

    override suspend fun hasCredentials(account: CaldavAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            application().accounts.findFor(account.username, account.name) { it.identity() } != null
        } catch (e: Exception) {
            logger.e(e) { "Failed to read the Microsoft account list" }
            true
        }
    }

    override suspend fun getToken(account: CaldavAccount): String = acquireSilently(account, force = false)

    override suspend fun refreshToken(account: CaldavAccount, rejected: String?): String {
        val cached = acquireSilently(account, force = false)
        return if (rejected == null || cached == rejected) acquireSilently(account, force = true) else cached
    }

    private suspend fun acquireSilently(account: CaldavAccount, force: Boolean): String = withContext(Dispatchers.IO) {
        val app = application()
        val result = try {
            val msalAccount = app.accounts.findFor(account.username, account.name) { it.identity() }
                ?: throw UnauthorizedException("No matching account found")
            app.acquireTokenSilent(
                AcquireTokenSilentParameters.Builder()
                    .withScopes(SCOPES)
                    .forAccount(msalAccount)
                    .fromAuthority(msalAccount.authority)
                    .forceRefresh(force)
                    .build()
            )
        } catch (e: MsalException) {
            logger.e(e) { "Silent token acquisition failed" }
            throw e.toTokenException()
        }
        result.accessToken
    }

    override suspend fun subjectsByUsername(): Map<String, String> = withContext(Dispatchers.IO) {
        application().accounts.map { it.identity() }.subjectsByUsername()
    }

    suspend fun signIn(activity: Activity, loginHint: String?, analytics: Analytics): Boolean {
        val signIn = acquireToken(activity, loginHint) ?: return false
        withContext(NonCancellable) {
            setupMsalMicrosoftAccount(signIn, caldavDao) {
                analytics.logEvent(
                    AnalyticsEvents.SYNC_ADD_ACCOUNT,
                    AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_MICROSOFT,
                )
            }
        }
        return true
    }

    private suspend fun acquireToken(activity: Activity, username: String?): MsalSignIn? =
        withContext(Dispatchers.IO) {
            val app = application()
            suspendCancellableCoroutine<MsalSignIn?> { continuation ->
                val parameters = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES)
                    .apply {
                        if (username.isNullOrBlank()) {
                            withPrompt(Prompt.SELECT_ACCOUNT)
                        } else {
                            withLoginHint(username)
                            withPrompt(Prompt.LOGIN)
                        }
                    }
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            val account = authenticationResult.account
                            val username = account.claims?.get(CLAIM_PREFERRED_USERNAME) as? String
                            if (username == null) {
                                continuation.resumeWithException(
                                    IllegalStateException("No email in the Microsoft response")
                                )
                            } else {
                                continuation.resume(MsalSignIn(username, account.identity().subject))
                            }
                        }

                        override fun onError(exception: MsalException?) {
                            continuation.resumeWithException(
                                exception ?: IllegalStateException("Microsoft sign-in failed")
                            )
                        }

                        override fun onCancel() {
                            continuation.resume(null)
                        }
                    })
                    .build()
                app.acquireToken(parameters)
            }
        }

    private fun IAccount.identity() = MsalIdentity(
        subject = claims?.get(CLAIM_SUBJECT) as? String,
        username = username,
    )

    companion object {
        private const val TAG = "MsalClient"
        private const val CLAIM_PREFERRED_USERNAME = "preferred_username"
        private const val CLAIM_SUBJECT = "sub"
        val SCOPES = listOf("https://graph.microsoft.com/.default")
    }
}
