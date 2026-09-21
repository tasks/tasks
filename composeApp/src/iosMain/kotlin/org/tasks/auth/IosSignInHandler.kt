package org.tasks.auth

import co.touchlab.kermit.Logger
import org.tasks.caldav.CaldavClientFactory
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.fcm.PushTokenManager
import org.tasks.http.HttpException
import org.tasks.security.Encryption

class IosSignInHandler(
    private val oauthFlow: OAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val caldavClientFactory: CaldavClientFactory,
    private val pushTokenManager: PushTokenManager,
) : SignInHandler {
    override suspend fun signIn(platform: Platform, provider: OAuthProvider?, openUrl: (String) -> Unit) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported on iOS")
        }
        val result = oauthFlow.signIn(oauthProvider)
        val account = try {
            setupTasksAccount(
                oauthResult = result,
                issuer = oauthProvider.issuer,
                caldavUrl = serverEnvironment.caldavUrl,
                caldavDao = caldavDao,
                encryption = encryption,
                provider = caldavClientFactory,
            )
        } catch (e: HttpException) {
            if (e.code == 402) throw PaymentRequiredException()
            throw e
        }
        pushTokenManager.registerTokenForAccount(account)
        Logger.i("IosSignInHandler") { "Account created successfully" }
    }
}
