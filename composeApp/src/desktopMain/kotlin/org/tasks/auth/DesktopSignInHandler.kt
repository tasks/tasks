package org.tasks.auth

import at.bitfire.dav4jvm.okhttp.exception.HttpException
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.getString
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.googleapis.GoogleTasksTokenData
import org.tasks.googleapis.ProxyAuthProvider
import org.tasks.security.KeyStoreEncryption
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.google_tasks_permission_not_granted

class DesktopSignInHandler(
    private val oauthFlow: DesktopOAuthFlow,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val caldavClientProvider: CaldavClientProvider,
    private val proxyAuthProvider: ProxyAuthProvider,
) : SignInHandler {

    override suspend fun signIn(platform: Platform, provider: OAuthProvider?, openUrl: (String) -> Unit) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            Platform.GOOGLE_TASKS -> OAuthProvider.GOOGLE_TASKS
            else -> throw UnsupportedOperationException("$platform not supported on desktop")
        }

        val authHeader = when (platform) {
            Platform.GOOGLE_TASKS -> proxyAuthProvider.getAuthHeader()
                ?: throw Exception("Sign in to tasks.org or link your desktop to use Google Tasks")
            else -> null
        }

        val result = oauthFlow.signIn(oauthProvider, authHeader = authHeader)

        when (platform) {
            Platform.GOOGLE_TASKS -> setupGoogleTasksAccount(result)
            else -> try {
                setupTasksAccount(
                    oauthResult = result,
                    issuer = oauthProvider.issuer,
                    caldavUrl = serverEnvironment.caldavUrl,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    provider = caldavClientProvider,
                )
            } catch (e: HttpException) {
                if (e.statusCode == 402) {
                    throw PaymentRequiredException()
                }
                throw e
            }
        }
        Logger.i(TAG) { "Account created successfully" }
        bringAppToForeground()
    }

    private suspend fun setupGoogleTasksAccount(result: OAuthResult) {
        val email = result.idToken?.email
            ?: throw Exception("No email in Google Tasks OAuth response")
        val refreshToken = result.refreshToken
            ?: throw Exception("No refresh_token — consent may not have been granted")
        val grantedScopes = result.grantedScopes
        if (grantedScopes == null) {
            Logger.w(TAG) { "No scope field in token response — cannot verify granted scopes" }
        } else if (GOOGLE_TASKS_SCOPE !in grantedScopes) {
            Logger.e(TAG) { "Tasks scope missing. Granted scopes: $grantedScopes" }
            throw Exception(getString(Res.string.google_tasks_permission_not_granted))
        } else {
            Logger.i(TAG) { "Granted scopes: $grantedScopes" }
        }

        val tokenData = GoogleTasksTokenData(
            accessToken = result.accessToken,
            refreshToken = refreshToken,
            tokenEndpoint = result.tokenEndpoint
                ?: throw Exception("No token_endpoint in OAuth result"),
            clientId = result.clientId
                ?: throw Exception("No client_id in OAuth result"),
            expiresAt = result.expiresIn
                ?.let { currentTimeMillis() + it * 1000 }
                ?: 0,
        )
        val encrypted = encryption.encrypt(tokenData.serialize())

        val existing = caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, email)
        if (existing != null) {
            caldavDao.update(existing.copy(
                password = encrypted,
                error = "",
            ))
        } else {
            val account = CaldavAccount(
                accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
                uuid = email,
                name = email,
                username = email,
                password = encrypted,
            )
            caldavDao.insert(account)
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
