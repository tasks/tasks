package org.tasks.auth

import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import org.tasks.extensions.htmlEscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.caldav.CaldavClientProvider
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AndroidSignInHandler"

class AndroidSignInHandler(
    private val context: Context,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val serverEnvironment: TasksServerEnvironment,
    private val syncAdapters: SyncAdapters,
    private val caldavClientProvider: CaldavClientProvider,
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

        val codeVerifier = PKCE.generateVerifier()
        val codeChallenge = PKCE.generateChallenge(codeVerifier)
        val state = PKCE.generateVerifier()

        val (config, code) = listenForCallback(state) { port ->
            val redirectUri = "http://127.0.0.1:$port"
            val config = OAuthConfig(
                authorizationEndpoint = authEndpoint,
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                redirectUri = redirectUri,
                scope = oauthProvider.scope,
                state = state,
            )
            val authUrl = oauthClient.buildAuthUrl(config, codeChallenge, state)
            Logger.d(TAG) { "Opening browser: $authUrl" }
            openUrl(authUrl)
            config
        }

        Logger.d(TAG) { "Got authorization code, exchanging..." }
        val result = oauthClient.exchangeCode(config, code, codeVerifier)

        setupTasksAccount(
            oauthResult = result,
            issuer = oauthProvider.issuer,
            caldavUrl = serverEnvironment.caldavUrl,
            caldavDao = caldavDao,
            encryption = encryption,
            provider = caldavClientProvider,
        )
        Logger.i(TAG) { "Account created successfully" }
        syncAdapters.sync(SyncSource.ACCOUNT_ADDED)
        bringAppToForeground()
    }

    private fun bringAppToForeground() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (intent != null) {
            context.startActivity(intent)
        }
    }

    private suspend fun listenForCallback(
        expectedState: String,
        onReady: (port: Int) -> OAuthConfig,
    ): Pair<OAuthConfig, String> = suspendCancellableCoroutine { cont ->
        val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        serverSocket.soTimeout = 300_000 // 5 minute timeout
        val port = serverSocket.localPort

        cont.invokeOnCancellation {
            serverSocket.close()
        }

        Logger.d(TAG) { "Listening on port $port" }
        val config = onReady(port)

        val socket = serverSocket.accept()
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: ""
            val path = requestLine.split(" ").getOrElse(1) { "" }
            val query = path.substringAfter("?", "")
            val params = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val (key, value) = it.split("=", limit = 2)
                    key to URLDecoder.decode(value, "UTF-8")
                }

            val code = params["code"]
            val error = params["error"]
            val returnedState = params["state"]

            val responseBody = if (code != null) {
                "<html><body>" +
                        "<h2>Sign in successful!</h2>" +
                        "<p>You can close this window and return to Tasks.</p>" +
                        "<script>window.close()</script>" +
                        "</body></html>"
            } else {
                "<html><body>" +
                        "<h2>Sign in failed</h2>" +
                        "<p>${(error ?: "Unknown error").htmlEscape()}</p>" +
                        "</body></html>"
            }

            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: ${responseBody.toByteArray().size}\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseBody
            socket.getOutputStream().write(response.toByteArray())
            socket.getOutputStream().flush()

            if (code != null) {
                if (returnedState != expectedState) {
                    cont.resumeWithException(Exception("OAuth state mismatch"))
                } else {
                    cont.resume(config to code)
                }
            } else {
                cont.resumeWithException(Exception(error ?: "Authorization failed"))
            }
        } finally {
            socket.close()
            serverSocket.close()
        }
    }
}
