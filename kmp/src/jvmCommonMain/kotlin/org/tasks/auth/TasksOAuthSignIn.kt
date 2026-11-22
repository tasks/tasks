package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.analytics.Analytics
import org.tasks.caldav.CaldavClientFactory
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.PushTokenManager
import org.tasks.security.Encryption

class TasksOAuthSignIn(
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val clientFactory: CaldavClientFactory,
    private val purchaseClaim: PurchaseClaim?,
    private val pushTokenManager: PushTokenManager,
    private val analytics: Analytics,
    oauthClient: TasksOAuthClient,
    redirect: OAuthRedirect,
) {
    private val logger = Logger.withTag(TAG)
    private val authorizer = OAuthAuthorizer(oauthClient, serverEnvironment, redirect)

    suspend fun signIn(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        loginHint: String? = null,
    ): CaldavAccount = createAccount(provider, authorize(provider, openUrl, loginHint))

    suspend fun createAccount(
        provider: OAuthProvider,
        result: OAuthResult,
    ): CaldavAccount = withContext(Dispatchers.IO) {
        val account = setupTasksAccount(
            oauthResult = result,
            issuer = provider.issuer,
            caldavUrl = serverEnvironment.caldavUrl,
            caldavDao = caldavDao,
            encryption = encryption,
            provider = clientFactory,
            purchaseClaim = purchaseClaim,
            pushTokenManager = pushTokenManager,
            analytics = analytics,
        )
        logger.i { "Account created successfully" }
        account
    }

    suspend fun signInToMicrosoft(
        openUrl: (String) -> Unit,
        loginHint: String? = null,
    ) {
        val result = authorize(OAuthProvider.MICROSOFT, openUrl, loginHint)
        withContext(Dispatchers.IO) {
            setupMicrosoftAccount(
                oauthResult = result,
                caldavDao = caldavDao,
                encryption = encryption,
                analytics = analytics,
            )
        }
    }

    suspend fun authorize(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        loginHint: String? = null,
    ): OAuthResult = authorizer.authorize(provider, openUrl, loginHint)

    companion object {
        private const val TAG = "TasksOAuthSignIn"
    }
}
