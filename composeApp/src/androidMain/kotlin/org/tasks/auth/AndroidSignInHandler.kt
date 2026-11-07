package org.tasks.auth

import org.tasks.compose.accounts.Platform

class AndroidSignInHandler(
    private val signIn: TasksOAuthSignIn,
    private val microsoftSignIn: MicrosoftSignIn?,
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        openUrl: (String) -> Unit,
    ) {
        if (platform == Platform.MICROSOFT) {
            val microsoft = microsoftSignIn
                ?: throw UnsupportedOperationException("$platform not supported")
            microsoft.signIn(openUrl)
            return
        }
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported")
        }
        signIn.signIn(oauthProvider, openUrl)
    }
}
