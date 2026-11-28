package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8

private const val TAG = "IosOAuthFlow"

class IosOAuthFlow(
    private val oauthClient: TasksOAuthClient,
    private val serverEnvironment: TasksServerEnvironment,
    redirect: OAuthRedirect,
    private val appleSignIn: AppleSignIn = AppleSignIn(),
) : OAuthFlow {
    private val authorizer = OAuthAuthorizer(oauthClient, serverEnvironment, redirect)

    override suspend fun signIn(
        provider: OAuthProvider,
        authHeader: String?,
        loginHint: String?,
    ): OAuthResult =
        if (provider == OAuthProvider.APPLE) {
            signInWithApple(provider, authHeader)
        } else {
            authorizer.authorize(provider, openUrl = {}, loginHint = loginHint, authHeader = authHeader)
        }

    private suspend fun signInWithApple(provider: OAuthProvider, authHeader: String?): OAuthResult {
        val discoveryUrl = "${serverEnvironment.caldavUrl}${provider.ios.discoveryPath}"
        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl, authHeader)
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content

        val nonce = PKCE.generateVerifier()
        val credential = appleSignIn.authorize(nonceHash = nonce.encodeUtf8().sha256().hex())

        Logger.d(TAG) { "Got Apple credential, exchanging..." }
        return oauthClient.exchangeAppleCredential(
            tokenEndpoint = tokenEndpoint,
            clientId = provider.ios.clientId,
            authorizationCode = credential.authorizationCode,
            identityToken = credential.identityToken,
            nonce = nonce,
            email = credential.email,
            fullName = credential.fullName,
            authHeader = authHeader,
        )
    }
}
