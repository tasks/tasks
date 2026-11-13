package org.tasks.auth

interface OAuthFlow {
    suspend fun signIn(
        provider: OAuthProvider,
        authHeader: String? = null,
        loginHint: String? = null,
    ): OAuthResult
}
