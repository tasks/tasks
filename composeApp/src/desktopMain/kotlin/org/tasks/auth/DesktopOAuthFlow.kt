package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.extensions.desktopOpenUrl
import java.io.IOException

private const val TAG = "DesktopOAuthFlow"
private const val CALLBACK_TIMEOUT_MILLIS = 10 * 60 * 1000L

class DesktopOAuthFlow(
    private val oauthClient: TasksOAuthClient,
    private val serverEnvironment: TasksServerEnvironment,
    private val callbackTimeoutMillis: Long = CALLBACK_TIMEOUT_MILLIS,
    private val openUrl: (String) -> Unit = ::desktopOpenUrl,
) : OAuthFlow {
    override suspend fun signIn(
        provider: OAuthProvider,
        authHeader: String?,
    ): OAuthResult = withContext(Dispatchers.IO) {
        val discoveryUrl = provider.discoveryUrl
            ?: "${serverEnvironment.caldavUrl}${provider.discoveryPath}"

        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl, authHeader)
        val authEndpoint = discovery["authorization_endpoint"]!!.jsonPrimitive.content
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content
        val clientId = provider.clientId.ifEmpty {
            discovery["client_id"]?.jsonPrimitive?.content
                ?: throw Exception("No client_id in discovery document")
        }

        val serverCallback = provider.serverCallbackPath?.let { "${serverEnvironment.caldavUrl}$it" }
        val codeVerifier = if (provider.usesPkce || serverCallback != null) PKCE.generateVerifier() else null
        val codeChallenge = codeVerifier?.takeIf { provider.usesPkce }?.let(PKCE::generateChallenge)
        val nonce = PKCE.generateVerifier()

        val (config, code) = LoopbackRedirect(
            host = provider.loopbackHost,
            timeoutMillis = callbackTimeoutMillis,
        ).awaitAuthorization(openUrl) { redirectUri ->
            val state = if (serverCallback == null) nonce else RedirectState.encode(nonce, redirectUri, codeVerifier!!)
            val config = OAuthConfig(
                authorizationEndpoint = authEndpoint,
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                redirectUri = redirectUri,
                scope = provider.scope,
                state = state,
            )
            val authUrl = oauthClient.buildAuthUrl(
                config.copy(redirectUri = serverCallback ?: redirectUri),
                codeChallenge,
                state,
                provider.authParams(),
            )
            Logger.d(TAG) { "Opening browser: $authUrl" }
            config to authUrl
        }

        Logger.d(TAG) { "Got authorization code, exchanging..." }
        oauthClient.exchangeCode(config, code, codeVerifier, authHeader)
    }

}
