package org.tasks.auth

interface OAuthFlow {
    suspend fun signIn(
        provider: OAuthProvider,
        extraAuthParams: Map<String, String> = provider.extraAuthParams,
        authHeader: String? = null,
    ): OAuthResult
}
