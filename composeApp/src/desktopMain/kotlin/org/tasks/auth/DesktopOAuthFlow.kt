package org.tasks.auth

import org.tasks.extensions.desktopOpenUrl

private const val CALLBACK_TIMEOUT_MILLIS = 10 * 60 * 1000L

class DesktopOAuthFlow(
    oauthClient: TasksOAuthClient,
    serverEnvironment: TasksServerEnvironment,
    callbackTimeoutMillis: Long = CALLBACK_TIMEOUT_MILLIS,
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
    ): OAuthResult = authorizer.authorize(provider, openUrl, authHeader = authHeader)
}
