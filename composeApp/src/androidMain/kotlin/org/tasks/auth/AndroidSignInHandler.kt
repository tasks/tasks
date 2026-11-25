package org.tasks.auth

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.analytics.Analytics
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.gtasks.GoogleAccountPicker
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account

class AndroidSignInHandler(
    private val signIn: TasksOAuthSignIn,
    private val microsoftSignIn: MicrosoftSignIn?,
    private val googleAccountPicker: GoogleAccountPicker,
    private val caldavDao: CaldavDao,
    private val analytics: Analytics,
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        loginHint: String?,
        onAuthorized: () -> Unit,
    ) {
        if (platform == Platform.MICROSOFT) {
            val microsoft = microsoftSignIn
                ?: throw UnsupportedOperationException("$platform not supported")
            microsoft.signIn(loginHint)
            return
        }
        if (platform == Platform.GOOGLE_TASKS) {
            val accountName = try {
                googleAccountPicker.pick(loginHint)
            } catch (e: GoogleAccountManager.AccountNotFoundException) {
                throw DisplayableException(Res.string.cannot_access_account)
            }
            onAuthorized()
            withContext(NonCancellable) {
                setupGoogleTasksAccount(accountName, caldavDao, analytics)
            }
            return
        }
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported")
        }
        val result = signIn.authorize(oauthProvider, openUrl = {}, loginHint = loginHint)
        onAuthorized()
        withContext(NonCancellable) {
            signIn.createAccount(oauthProvider, result)
        }
    }
}
