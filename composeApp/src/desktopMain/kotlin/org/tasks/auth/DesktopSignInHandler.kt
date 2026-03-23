package org.tasks.auth

import co.touchlab.kermit.Logger
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption

class DesktopSignInHandler(
    private val oauthFlow: DesktopOAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
) : SignInHandler {

    override suspend fun signIn(platform: Platform, provider: OAuthProvider?, openUrl: (String) -> Unit) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported on desktop")
        }

        val result = oauthFlow.signIn(oauthProvider)

        setupTasksAccount(
            oauthResult = result,
            issuer = oauthProvider.issuer,
            caldavUrl = serverEnvironment.caldavUrl,
            caldavDao = caldavDao,
            encryption = encryption,
        )
        Logger.i("DesktopSignInHandler") { "Account created successfully" }
    }
}
