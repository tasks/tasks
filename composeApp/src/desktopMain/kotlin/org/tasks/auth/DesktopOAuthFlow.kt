package org.tasks.auth

import org.tasks.extensions.desktopOpenUrl

class DesktopOAuthFlow(
    oauthClient: TasksOAuthClient,
    serverEnvironment: TasksServerEnvironment,
    callbackTimeoutMillis: Long = REDIRECT_TIMEOUT_MILLIS,
    private val openUrl: (String) -> Unit = ::desktopOpenUrl,
) : OAuthFlow {
    private val authorizer = OAuthAuthorizer(
        oauthClient = oauthClient,
        serverEnvironment = serverEnvironment,
        redirect = LoopbackRedirect(timeoutMillis = callbackTimeoutMillis),
    )

    override suspend fun signIn(
        provider: OAuthProvider,
        authHeader: String?,
        loginHint: String?,
    ): OAuthResult = authorizer.authorize(provider, openUrl, loginHint, authHeader)
}
