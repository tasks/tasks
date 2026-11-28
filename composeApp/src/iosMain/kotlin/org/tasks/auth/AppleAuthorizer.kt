package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8

private const val TAG = "AppleAuthorizer"

class AppleAuthorizer(
    private val oauthClient: TasksOAuthClient,
    private val serverEnvironment: TasksServerEnvironment,
    private val appleSignIn: AppleSignIn = AppleSignIn(),
) : NativeAuthorizer {

    override suspend fun authorize(provider: OAuthProvider, authHeader: String?): OAuthResult {
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
