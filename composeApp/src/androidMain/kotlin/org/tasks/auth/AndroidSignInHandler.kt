package org.tasks.auth

import android.content.Context
import org.tasks.extensions.bringTasksToForeground
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.http.HttpException
import org.tasks.security.KeyStoreEncryption

private const val TAG = "AndroidSignInHandler"

class AndroidSignInHandler(
    private val context: Context,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val caldavClientProvider: CaldavClientProvider,
    private val purchaseClaim: PurchaseClaim,
) : SignInHandler {

    private val oauthClient = TasksOAuthClient()

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        openUrl: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            else -> throw UnsupportedOperationException("$platform not supported")
        }

        val discoveryUrl = "${serverEnvironment.caldavUrl}${oauthProvider.discoveryPath}"

        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl)
        val authEndpoint = discovery["authorization_endpoint"]!!.jsonPrimitive.content
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content
        val clientId = oauthProvider.clientId.ifEmpty {
            discovery["client_id"]?.jsonPrimitive?.content
                ?: throw Exception("No client_id in discovery document")
        }

        val serverCallback = oauthProvider.serverCallbackPath?.let { "${serverEnvironment.caldavUrl}$it" }
        val codeVerifier = if (oauthProvider.usesPkce || serverCallback != null) PKCE.generateVerifier() else null
        val codeChallenge = codeVerifier?.takeIf { oauthProvider.usesPkce }?.let(PKCE::generateChallenge)
        val nonce = PKCE.generateVerifier()

        val (config, code) = LoopbackRedirect(
            host = oauthProvider.loopbackHost,
        ).awaitAuthorization(openUrl) { redirectUri ->
            val state = if (serverCallback == null) nonce else RedirectState.encode(nonce, redirectUri, codeVerifier!!)
            val config = OAuthConfig(
                authorizationEndpoint = authEndpoint,
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                redirectUri = redirectUri,
                scope = oauthProvider.scope,
                state = state,
            )
            val authUrl = oauthClient.buildAuthUrl(
                config.copy(redirectUri = serverCallback ?: redirectUri),
                codeChallenge,
                state,
                oauthProvider.authParams(),
            )
            Logger.d(TAG) { "Opening browser: $authUrl" }
            config to authUrl
        }

        Logger.d(TAG) { "Got authorization code, exchanging..." }
        val result = oauthClient.exchangeCode(config, code, codeVerifier)

        try {
            try {
                setupTasksAccount(
                    oauthResult = result,
                    issuer = oauthProvider.issuer,
                    caldavUrl = serverEnvironment.caldavUrl,
                    caldavDao = caldavDao,
                    encryption = encryption,
                    provider = caldavClientProvider,
                    purchaseClaim = purchaseClaim,
                )
            } catch (e: HttpException) {
                if (e.code == 402) {
                    throw PaymentRequiredException()
                }
                throw e
            }
            Logger.i(TAG) { "Account created successfully" }
        } finally {
            context.bringTasksToForeground()
        }
    }

}
