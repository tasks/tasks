package org.tasks.auth

import co.touchlab.kermit.Logger
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.googleapis.ProxyAuthProvider
import org.tasks.http.HttpException
import org.tasks.security.KeyStoreEncryption

class DesktopSignInHandler(
    private val oauthFlow: OAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val caldavClientProvider: CaldavClientProvider,
    private val proxyAuthProvider: ProxyAuthProvider,
    private val reporting: Reporting,
    private val purchaseClaim: PurchaseClaim,
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        openUrl: (String) -> Unit,
    ) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            Platform.GOOGLE_TASKS -> OAuthProvider.GOOGLE_TASKS
            Platform.MICROSOFT -> OAuthProvider.MICROSOFT
            else -> throw UnsupportedOperationException("$platform not supported on desktop")
        }

        val authHeader = when (platform) {
            Platform.GOOGLE_TASKS -> proxyAuthProvider.getAuthHeader()
                ?: throw Exception("Sign in to tasks.org or link your desktop to use Google Tasks")
            else -> null
        }

        val result = oauthFlow.signIn(oauthProvider, authHeader = authHeader)

        try {
            when (platform) {
                Platform.GOOGLE_TASKS -> setupGoogleTasksAccount(
                    oauthResult = result,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    analytics = reporting,
                )
                Platform.MICROSOFT -> setupMicrosoftAccount(
                    oauthResult = result,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    analytics = reporting,
                )
                else -> try {
                    setupTasksAccount(
                        oauthResult = result,
                        issuer = oauthProvider.issuer,
                        caldavUrl = serverEnvironment.caldavUrl,
                        caldavDao = caldavDao,
                        encryption = encryption,
                        provider = caldavClientProvider,
                        purchaseClaim = purchaseClaim,
                    )
                } catch (e: HttpException) {
                    if (e.code == 402) {
                        throw PaymentRequiredException()
                    }
                    throw e
                }
            }
            Logger.i(TAG) { "Signed in" }
        } finally {
            bringAppToForeground()
        }
    }

    private fun bringAppToForeground() {
        java.awt.EventQueue.invokeLater {
            java.awt.Frame.getFrames().forEach { frame ->
                frame.toFront()
                frame.requestFocus()
            }
        }
    }

    companion object {
        private const val TAG = "DesktopSignInHandler"
    }
}
