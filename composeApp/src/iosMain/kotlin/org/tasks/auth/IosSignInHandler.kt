package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientFactory
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.fcm.PushTokenManager
import org.tasks.security.Encryption

class IosSignInHandler(
    private val oauthFlow: OAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val caldavClientFactory: CaldavClientFactory,
    private val pushTokenManager: PushTokenManager,
    private val purchaseClaim: PurchaseClaim,
    private val reporting: Reporting,
) : SignInHandler {
    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        loginHint: String?,
        onAuthorized: () -> Unit,
    ) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            Platform.GOOGLE_TASKS -> OAuthProvider.GOOGLE_TASKS
            else -> throw UnsupportedOperationException("$platform not supported on iOS")
        }
        val result = oauthFlow.signIn(oauthProvider, loginHint = loginHint)
        onAuthorized()
        withContext(NonCancellable) {
            when (platform) {
                Platform.GOOGLE_TASKS -> setupGoogleTasksAccount(
                    oauthResult = result,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    analytics = reporting,
                )
                else -> setupTasksAccount(
                    oauthResult = result,
                    issuer = oauthProvider.issuer,
                    caldavUrl = serverEnvironment.caldavUrl,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    provider = caldavClientFactory,
                    purchaseClaim = purchaseClaim,
                    pushTokenManager = pushTokenManager,
                    analytics = reporting,
                )
            }
        }
        Logger.i("IosSignInHandler") { "Account created successfully" }
    }
}
