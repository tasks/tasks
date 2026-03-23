package org.tasks.auth

import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.URLDecoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "DesktopOAuthFlow"

class DesktopOAuthFlow(
    private val oauthClient: TasksOAuthClient = TasksOAuthClient(),
    private val serverEnvironment: TasksServerEnvironment,
    private val openUrl: (String) -> Unit = { url ->
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("mac") -> ProcessBuilder("open", url)
            os.contains("win") -> ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url)
            else -> ProcessBuilder("xdg-open", url)
        }.start()
    },
) {
    suspend fun signIn(provider: OAuthProvider): OAuthResult = withContext(Dispatchers.IO) {
        val discoveryUrl = "${serverEnvironment.caldavUrl}${provider.discoveryPath}"

        Logger.d(TAG) { "Fetching discovery from $discoveryUrl" }
        val discovery = oauthClient.fetchDiscovery(discoveryUrl)
        val authEndpoint = discovery["authorization_endpoint"]!!.jsonPrimitive.content
        val tokenEndpoint = discovery["token_endpoint"]!!.jsonPrimitive.content
        val clientId = provider.clientId.ifEmpty {
            discovery["client_id"]?.jsonPrimitive?.content
                ?: throw Exception("No client_id in discovery document")
        }

        val codeVerifier = PKCE.generateVerifier()
        val codeChallenge = PKCE.generateChallenge(codeVerifier)

        val (config, code) = listenForCallback { port ->
            val redirectUri = "http://127.0.0.1:$port"
            val config = OAuthConfig(
                authorizationEndpoint = authEndpoint,
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                redirectUri = redirectUri,
                scope = provider.scope,
            )
            val authUrl = oauthClient.buildAuthUrl(config, codeVerifier, codeChallenge)
            Logger.d(TAG) { "Opening browser: $authUrl" }
            openUrl(authUrl)
            config
        }

        Logger.d(TAG) { "Got authorization code, exchanging..." }
        oauthClient.exchangeCode(config, code, codeVerifier)
    }

    private fun bringAppToForeground() {
        java.awt.EventQueue.invokeLater {
            java.awt.Frame.getFrames().forEach { frame ->
                frame.toFront()
                frame.requestFocus()
            }
        }
    }

    private suspend fun listenForCallback(
        onReady: (port: Int) -> OAuthConfig
    ): Pair<OAuthConfig, String> = suspendCancellableCoroutine { cont ->
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = server.address.port
        var config: OAuthConfig? = null

        server.createContext("/") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val params = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val (key, value) = it.split("=", limit = 2)
                    key to URLDecoder.decode(value, "UTF-8")
                }

            val code = params["code"]
            val error = params["error"]

            val responseBody = when {
                code != null -> """
                    <html><body>
                    <h2>Sign in successful!</h2>
                    <p>You can close this window and return to Tasks.</p>
                    <script>window.close()</script>
                    </body></html>
                """.trimIndent()
                else -> """
                    <html><body>
                    <h2>Sign in failed</h2>
                    <p>${error ?: "Unknown error"}</p>
                    </body></html>
                """.trimIndent()
            }

            exchange.responseHeaders.add("Content-Type", "text/html")
            exchange.sendResponseHeaders(200, responseBody.length.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray()) }

            server.stop(1)

            if (code != null && config != null) {
                bringAppToForeground()
                cont.resume(config!! to code)
            } else {
                cont.resumeWithException(
                    Exception(error ?: "Authorization failed")
                )
            }
        }

        server.start()
        Logger.d(TAG) { "Listening on port $port" }

        cont.invokeOnCancellation {
            server.stop(0)
        }

        config = onReady(port)
    }
}
