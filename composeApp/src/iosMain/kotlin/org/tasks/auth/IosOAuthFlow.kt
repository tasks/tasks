package org.tasks.auth

import co.touchlab.kermit.Logger
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8
import org.tasks.extensions.keyWindow
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "IosOAuthFlow"

class IosOAuthFlow(
    private val oauthClient: TasksOAuthClient,
    private val serverEnvironment: TasksServerEnvironment,
    private val appleSignIn: AppleSignIn = AppleSignIn(),
) : OAuthFlow {
    override suspend fun signIn(
        provider: OAuthProvider,
        extraAuthParams: Map<String, String>,
        authHeader: String?,
    ): OAuthResult {
        if (provider == OAuthProvider.APPLE) {
            return signInWithApple(provider, authHeader)
        }
        val clientId = provider.iosClientId.ifEmpty { throw Exception("${provider.issuer} sign-in is not configured for iOS") }
        val redirectUri = provider.iosRedirectUri ?: googleRedirectUri(clientId)
        val discoveryUrl = provider.discoveryUrl ?: "${serverEnvironment.caldavUrl}${provider.iosDiscoveryPath}"
        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl, authHeader)
        val authEndpoint = discovery["authorization_endpoint"]!!.jsonPrimitive.content
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content

        val codeVerifier = PKCE.generateVerifier()
        val state = PKCE.generateVerifier()
        val config = OAuthConfig(
            authorizationEndpoint = authEndpoint,
            tokenEndpoint = tokenEndpoint,
            clientId = clientId,
            redirectUri = redirectUri,
            scope = provider.scope,
            state = state,
        )
        val defaultParams = if (extraAuthParams.containsKey("prompt")) emptyMap() else mapOf("prompt" to "select_account")
        val authUrl = oauthClient.buildAuthUrl(config, PKCE.generateChallenge(codeVerifier), state, defaultParams + extraAuthParams)

        val callback = authenticate(authUrl, redirectUri.substringBefore(':'))
        val parameters = Url(callback).parameters
        val error = parameters["error"]
        val errorDescription = parameters["error_description"]
        val code = parameters["code"]
            ?: throw (ConditionalAccess.devicePolicyException(error, errorDescription)
                ?: Exception(errorDescription ?: error ?: "Authorization failed"))
        if (parameters["state"] != state) throw Exception("OAuth state mismatch")

        Logger.d(TAG) { "Got authorization code, exchanging..." }
        return oauthClient.exchangeCode(config, code, codeVerifier, authHeader)
    }

    private suspend fun signInWithApple(provider: OAuthProvider, authHeader: String?): OAuthResult {
        val discoveryUrl = "${serverEnvironment.caldavUrl}${provider.iosDiscoveryPath}"
        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl, authHeader)
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content

        val nonce = PKCE.generateVerifier()
        val credential = appleSignIn.authorize(nonceHash = nonce.encodeUtf8().sha256().hex())

        Logger.d(TAG) { "Got Apple credential, exchanging..." }
        return oauthClient.exchangeAppleCredential(
            tokenEndpoint = tokenEndpoint,
            clientId = provider.iosClientId,
            authorizationCode = credential.authorizationCode,
            identityToken = credential.identityToken,
            nonce = nonce,
            email = credential.email,
            fullName = credential.fullName,
            authHeader = authHeader,
        )
    }

    private fun googleRedirectUri(clientId: String) =
        "com.googleusercontent.apps.${clientId.removeSuffix(".apps.googleusercontent.com")}:/oauth2redirect"
}

private class KeyWindowProvider : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
        keyWindow()
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun authenticate(authUrl: String, callbackScheme: String): String = withContext(Dispatchers.Main) {
    val contextProvider = KeyWindowProvider()
    suspendCancellableCoroutine { continuation ->
        val session = ASWebAuthenticationSession(
            uRL = NSURL.URLWithString(authUrl)!!,
            callbackURLScheme = callbackScheme,
        ) { callbackUrl: NSURL?, error: NSError? ->
            when {
                callbackUrl != null -> continuation.resume(callbackUrl.absoluteString!!)
                error?.code == ASWebAuthenticationSessionErrorCodeCanceledLogin ->
                    continuation.resumeWithException(CancellationException("Sign in cancelled"))
                else -> continuation.resumeWithException(Exception(error?.localizedDescription ?: "Authorization failed"))
            }
        }
        session.presentationContextProvider = contextProvider
        session.prefersEphemeralWebBrowserSession = false
        continuation.invokeOnCancellation { session.cancel() }
        if (!session.start()) {
            continuation.resumeWithException(Exception("Could not start the sign-in session"))
        }
    }
}
