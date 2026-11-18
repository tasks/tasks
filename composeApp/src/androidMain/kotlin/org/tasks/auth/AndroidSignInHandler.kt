package org.tasks.auth

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.compose.accounts.Platform

class AndroidSignInHandler(
    private val signIn: TasksOAuthSignIn,
    private val microsoftSignIn: MicrosoftSignIn?,
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
