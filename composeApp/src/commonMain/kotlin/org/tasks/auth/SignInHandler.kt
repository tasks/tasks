package org.tasks.auth

import org.tasks.compose.accounts.Platform

interface SignInHandler {
    suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider? = null,
        openUrl: (String) -> Unit = {},
    )
}
