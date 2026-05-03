package org.tasks.auth

import co.touchlab.kermit.Logger
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class DesktopSignInHandler(
    private val oauthFlow: DesktopOAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val syncAdapters: SyncAdapters,
    private val caldavClientProvider: CaldavClientProvider,
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
            provider = caldavClientProvider,
        )
        Logger.i("DesktopSignInHandler") { "Account created successfully" }
        syncAdapters.sync(SyncSource.ACCOUNT_ADDED)
        bringAppToForeground()
    }

    private fun bringAppToForeground() {
        java.awt.EventQueue.invokeLater {
            java.awt.Frame.getFrames().forEach { frame ->
                frame.toFront()
                frame.requestFocus()
            }
        }
    }
}
