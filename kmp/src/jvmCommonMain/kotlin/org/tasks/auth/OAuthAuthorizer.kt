package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

class OAuthAuthorizer(
    private val oauthClient: TasksOAuthClient,
    private val serverEnvironment: TasksServerEnvironment,
    private val redirect: OAuthRedirect,
) {
    private val logger = Logger.withTag(TAG)

    suspend fun authorize(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        authHeader: String? = null,
    ): OAuthResult = withContext(Dispatchers.IO) {
        val client = redirect.client(provider)
        val discoveryUrl = provider.discoveryUrl
            ?: "${serverEnvironment.caldavUrl}${client.discoveryPath}"
        logger.d { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl, authHeader)
        val authEndpoint = discovery.endpoint(AUTHORIZATION_ENDPOINT)
        val tokenEndpoint = discovery.endpoint(TOKEN_ENDPOINT)
        val clientId = client.clientId.ifEmpty {
            discovery[CLIENT_ID]?.jsonPrimitive?.content
                ?: throw IllegalStateException("No client_id in discovery document")
        }

        val serverCallback =
            provider.serverCallbackPath?.let { "${serverEnvironment.caldavUrl}$it" }
        val codeVerifier =
            if (provider.usesPkce || serverCallback != null) PKCE.generateVerifier() else null
        val codeChallenge = codeVerifier?.takeIf { provider.usesPkce }?.let(PKCE::generateChallenge)
        val nonce = PKCE.generateVerifier()

        val (config, code) = redirect
            .awaitAuthorization(provider, openUrl) { redirectUri ->
                val state = if (serverCallback == null) {
                    nonce
                } else {
                    RedirectState.encode(nonce, redirectUri, codeVerifier!!)
                }
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
                logger.d { "Opening browser: $authUrl" }
                config to authUrl
            }

        logger.d { "Got authorization code, exchanging..." }
        oauthClient.exchangeCode(config, code, codeVerifier, authHeader)
    }

    private fun JsonObject.endpoint(name: String): String =
        get(name)?.jsonPrimitive?.content
            ?: throw IOException("The discovery document has no $name")

    companion object {
        private const val TAG = "OAuthAuthorizer"
        private const val AUTHORIZATION_ENDPOINT = "authorization_endpoint"
        private const val TOKEN_ENDPOINT = "token_endpoint"
        private const val CLIENT_ID = "client_id"
    }
}
