package org.tasks.sync.microsoft

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.UnauthorizedException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MsalClient(
    private val context: Context,
    private val configResourceId: Int,
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
            application().accounts.any { it.username == account.username }
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
            val msalAccount = app.accounts.firstOrNull { it.username == account.username }
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

    suspend fun signIn(activity: Activity, username: String? = null): String =
        withContext(Dispatchers.IO) {
            val app = application()
            suspendCancellableCoroutine { continuation ->
                val parameters = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES)
                    .apply {
                        if (username == null) {
                            withPrompt(Prompt.SELECT_ACCOUNT)
                        } else {
                            withLoginHint(username)
                            withPrompt(Prompt.LOGIN)
                        }
                    }
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            val email = authenticationResult
                                .account
                                .claims
                                ?.get(CLAIM_PREFERRED_USERNAME) as? String
                            if (email == null) {
                                continuation.resumeWithException(
                                    IllegalStateException("No email in the Microsoft response")
                                )
                            } else {
                                continuation.resume(email)
                            }
                        }

                        override fun onError(exception: MsalException?) {
                            continuation.resumeWithException(
                                exception ?: IllegalStateException("Microsoft sign-in failed")
                            )
                        }

                        override fun onCancel() {
                            continuation.cancel(CancellationException("Sign in cancelled"))
                        }
                    })
                    .build()
                app.acquireToken(parameters)
            }
        }

    companion object {
        private const val TAG = "MsalClient"
        private const val CLAIM_PREFERRED_USERNAME = "preferred_username"
        val SCOPES = listOf("https://graph.microsoft.com/.default")
    }
}
