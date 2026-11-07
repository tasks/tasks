package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.caldav.CaldavClientFactory
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.security.Encryption

class TasksOAuthSignIn(
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val clientFactory: CaldavClientFactory,
    private val purchaseClaim: PurchaseClaim? = null,
    oauthClient: TasksOAuthClient,
    private val onFinished: () -> Unit = {},
) {
    private val logger = Logger.withTag(TAG)
    private val authorizer = OAuthAuthorizer(oauthClient, serverEnvironment)

    suspend fun signIn(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
    ): CaldavAccount = createAccount(provider, authorize(provider, openUrl))

    suspend fun createAccount(
        provider: OAuthProvider,
        result: OAuthResult,
    ): CaldavAccount = withContext(Dispatchers.IO) {
        try {
            val account = paymentRequiredOn402 {
                setupTasksAccount(
                    oauthResult = result,
                    issuer = provider.issuer,
                    caldavUrl = serverEnvironment.caldavUrl,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    provider = clientFactory,
                    purchaseClaim = purchaseClaim,
                )
            }
            logger.i { "Account created successfully" }
            account
        } finally {
            onFinished()
        }
    }

    suspend fun authorize(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
    ): OAuthResult = authorizer.authorize(provider, openUrl)

    companion object {
        private const val TAG = "TasksOAuthSignIn"
    }
}
