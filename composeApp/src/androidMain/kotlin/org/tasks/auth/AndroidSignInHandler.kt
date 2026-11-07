package org.tasks.auth

import org.tasks.compose.accounts.Platform

class AndroidSignInHandler(
    private val signIn: TasksOAuthSignIn,
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        openUrl: (String) -> Unit,
    ) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported")
        }
        signIn.signIn(oauthProvider, openUrl)
    }
}
