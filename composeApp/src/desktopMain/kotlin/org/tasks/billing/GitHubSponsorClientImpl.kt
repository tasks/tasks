package org.tasks.billing

import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.auth.TasksServerEnvironment
import org.tasks.extensions.htmlEscape
import org.tasks.http.OkHttpClientFactory
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.resume

class GitHubSponsorClientImpl(
    private val httpClientFactory: OkHttpClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val desktopEntitlement: DesktopEntitlement,
    private val json: Json,
) : GitHubSponsorClient {
    private val logger = Logger.withTag("GitHubSponsorClient")

    @Serializable
    private data class VerifyRequest(
        val code: String,
        val redirect_uri: String,
    )

    @Serializable
    private data class VerifyResponse(
        val jwt: String,
        val refresh_token: String,
        val sku: String? = null,
        val formatted_price: String? = null,
    )

    companion object {
        private const val GITHUB_CLIENT_ID = "Ov23limM74bOgBgCBrJ5"
        private const val GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
        private const val SIGN_IN_TIMEOUT_MS = 5 * 60 * 1000L
    }

    override suspend fun signIn(openUrl: (String) -> Unit): GitHubSponsorClient.VerifyResult {
        val (code, redirectUri) = try {
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                listenForCallback(openUrl)
            }
        } catch (e: Exception) {
            logger.e(e) { "GitHub OAuth flow failed" }
            return GitHubSponsorClient.VerifyResult.Failed
        }
        return verify(code, redirectUri)
    }

    private suspend fun listenForCallback(
        openUrl: (String) -> Unit,
    ): Pair<String, String> = suspendCancellableCoroutine { cont ->
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = server.address.port
        val redirectUri = "http://127.0.0.1:$port/callback"
        val state = generateState()

        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val params = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val (key, value) = it.split("=", limit = 2)
                    key to URLDecoder.decode(value, "UTF-8")
                }

            val code = params["code"]
            val error = params["error"]
            val returnedState = params["state"]

            val responseBody = when {
                code != null -> """
                    <html><body>
                    <h2>Sign in successful!</h2>
                    <p>You can close this window and return to Tasks.</p>
                    <script>setTimeout(function(){ window.close(); }, 2000)</script>
                    </body></html>
                """.trimIndent()
                else -> """
                    <html><body>
                    <h2>Sign in failed</h2>
                    <p>${(error ?: "Unknown error").htmlEscape()}</p>
                    </body></html>
                """.trimIndent()
            }

            exchange.responseHeaders.add("Content-Type", "text/html")
            val responseBytes = responseBody.toByteArray()
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            exchange.responseBody.use { it.write(responseBytes) }

            Thread { server.stop(1) }.start()

            if (cont.isActive) {
                when {
                    code != null && returnedState == state ->
                        cont.resume(code to redirectUri)
                    code != null ->
                        cont.resumeWith(
                            Result.failure(Exception("OAuth state mismatch"))
                        )
                    else ->
                        cont.resumeWith(
                            Result.failure(Exception(error ?: "Authorization failed"))
                        )
                }
            }
        }

        server.start()
        logger.d { "GitHub OAuth listening on port $port" }

        cont.invokeOnCancellation {
            server.stop(0)
        }

        val authUrl = buildString {
            append(GITHUB_AUTHORIZE_URL)
            append("?client_id=")
            append(URLEncoder.encode(GITHUB_CLIENT_ID, "UTF-8"))
            append("&redirect_uri=")
            append(URLEncoder.encode(redirectUri, "UTF-8"))
            append("&state=")
            append(URLEncoder.encode(state, "UTF-8"))
        }
        openUrl(authUrl)
    }

    private fun generateState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private suspend fun verify(code: String, redirectUri: String): GitHubSponsorClient.VerifyResult =
        withContext(Dispatchers.IO) {
            try {
                val client = httpClientFactory.newClient()
                val url = "${serverEnvironment.caldavUrl}/desktop/github/verify"
                val body = json.encodeToString(
                    VerifyRequest.serializer(),
                    VerifyRequest(code = code, redirect_uri = redirectUri)
                ).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                response.use {
                    when {
                        it.isSuccessful -> {
                            val responseBody = it.body?.string()
                                ?: return@withContext GitHubSponsorClient.VerifyResult.Failed
                            val result = json.decodeFromString(VerifyResponse.serializer(), responseBody)
                            desktopEntitlement.storeEntitlement(
                                jwt = result.jwt,
                                refreshToken = result.refresh_token,
                                sku = result.sku,
                                formattedPrice = result.formatted_price,
                                provider = EntitlementProvider.GITHUB_SPONSOR,
                            )
                            GitHubSponsorClient.VerifyResult.Success
                        }
                        it.code == 402 -> {
                            logger.i { "Not a sponsor" }
                            GitHubSponsorClient.VerifyResult.NotSponsor
                        }
                        else -> {
                            logger.w { "GitHub verify failed: ${it.code}" }
                            GitHubSponsorClient.VerifyResult.Failed
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "GitHub verify request failed" }
                GitHubSponsorClient.VerifyResult.Failed
            }
        }
}
