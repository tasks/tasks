package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.tasks.analytics.Analytics
import org.tasks.caldav.CaldavClientFactory
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.PushTokenManager
import org.tasks.security.Encryption

fun interface NativeAuthorizer {
    suspend fun authorize(provider: OAuthProvider, authHeader: String?): OAuthResult
}

interface OAuthSignIn {
    suspend fun authorize(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        loginHint: String? = null,
        authHeader: String? = null,
    ): OAuthResult

    suspend fun createAccount(provider: OAuthProvider, result: OAuthResult): CaldavAccount

    suspend fun createGoogleTasksAccount(result: OAuthResult)

    suspend fun createMicrosoftAccount(result: OAuthResult)
}

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
    private val nativeAuthorizers: Map<OAuthProvider, NativeAuthorizer> = emptyMap(),
) : OAuthSignIn {
    private val logger = Logger.withTag(TAG)
    private val authorizer = OAuthAuthorizer(oauthClient, serverEnvironment, redirect)

    suspend fun signIn(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        loginHint: String? = null,
    ): CaldavAccount = createAccount(provider, authorize(provider, openUrl, loginHint))

    override suspend fun createAccount(
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

    override suspend fun createGoogleTasksAccount(result: OAuthResult) = withContext(Dispatchers.IO) {
        setupGoogleTasksAccount(
            oauthResult = result,
            caldavDao = caldavDao,
            encryption = encryption,
            analytics = analytics,
        )
        logger.i { "Account created successfully" }
    }

    override suspend fun createMicrosoftAccount(result: OAuthResult) = withContext(Dispatchers.IO) {
        setupMicrosoftAccount(
            oauthResult = result,
            caldavDao = caldavDao,
            encryption = encryption,
            analytics = analytics,
        )
        logger.i { "Account created successfully" }
    }

    suspend fun signInToMicrosoft(
        openUrl: (String) -> Unit,
        loginHint: String? = null,
    ) {
        createMicrosoftAccount(authorize(OAuthProvider.MICROSOFT, openUrl, loginHint))
    }

    override suspend fun authorize(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        loginHint: String?,
        authHeader: String?,
    ): OAuthResult {
        val native = nativeAuthorizers[provider]
        return if (native != null) {
            native.authorize(provider, authHeader)
        } else {
            authorizer.authorize(provider, openUrl, loginHint, authHeader)
        }
    }

    companion object {
        private const val TAG = "TasksOAuthSignIn"
    }
}
