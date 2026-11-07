package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.extensions.htmlEscape
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class LoopbackGitHubSponsorClient(
    private val verify: suspend (code: String, redirectUri: String) -> GitHubSponsorClient.VerifyResult,
    private val openUrl: (String) -> Unit,
    private val onCallback: () -> Unit = {},
    private val clientId: String = GITHUB_CLIENT_ID,
    private val signInTimeoutMillis: Long = SIGN_IN_TIMEOUT_MILLIS,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GitHubSponsorClient {
    private val logger = Logger.withTag(TAG)
    private val lock = Mutex()
    private var attempt: Deferred<GitHubSponsorClient.VerifyResult>? = null
    private var attemptUrl: String? = null

    override suspend fun signIn(): GitHubSponsorClient.VerifyResult {
        val pending = lock.withLock {
            attempt?.takeIf { it.isActive }?.also { reopenBrowser() }
                ?: scope.async { listenAndVerify() }.also {
                    attempt = it
                    attemptUrl = null
                }
        }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "GitHub OAuth flow failed" }
            GitHubSponsorClient.VerifyResult.Failed
        }
    }

    private suspend fun listenAndVerify(): GitHubSponsorClient.VerifyResult {
        val (code, redirectUri) = listenForCallback()
        return verify(code, redirectUri)
    }

    private suspend fun listenForCallback(): Pair<String, String> = withContext(Dispatchers.IO) {
        ServerSocket(0, BACKLOG, InetAddress.getByName(LOOPBACK)).use { serverSocket ->
            serverSocket.soTimeout = ACCEPT_POLL_MILLIS
            val redirectUri = "http://$LOOPBACK:${serverSocket.localPort}$CALLBACK_PATH"
            val state = generateState()
            logger.d { "GitHub OAuth listening on port ${serverSocket.localPort}" }
            val url = authorizationUrl(redirectUri, state)
            lock.withLock { attemptUrl = url }
            openUrl(url)
            val code = withTimeoutOrNull(signInTimeoutMillis) { serverSocket.awaitCode(state) }
                ?: throw IOException("Timed out waiting for the authorization")
            code to redirectUri
        }
    }

    private suspend fun ServerSocket.awaitCode(state: String): String = coroutineScope {
        val live = ConcurrentHashMap.newKeySet<Socket>()
        val outcome = CompletableDeferred<String>()
        val accepting = launch {
            while (isActive) {
                val socket = try {
                    accept()
                } catch (e: SocketTimeoutException) {
                    continue
                } catch (e: IOException) {
                    break
                }
                live.add(socket)
                launch {
                    try {
                        socket.handle(state, outcome)
                    } finally {
                        live.remove(socket)
                        socket.closeQuietly()
                    }
                }
            }
        }
        try {
            outcome.await()
        } finally {
            accepting.cancel()
            live.forEach { it.closeQuietly() }
        }
    }

    private fun Socket.handle(state: String, outcome: CompletableDeferred<String>) {
        soTimeout = READ_TIMEOUT_MILLIS
        val target = try {
            requestTarget()
        } catch (e: IOException) {
            logger.d { "Ignoring a connection that sent no request: ${e.message}" }
            return
        }
        val params = target.callbackParams()
        if (params == null) {
            logger.d { "Ignoring a request for $target" }
            respondQuietly(NOT_FOUND, "<h2>Not found</h2>")
            return
        }
        val code = params["code"]
        val failure = when {
            code == null -> params["error"] ?: "Authorization failed"
            params["state"] != state -> "OAuth state mismatch"
            else -> null
        }
        returnToApp()
        respondQuietly(OK, page(failure))
        drainQuietly()
        when {
            failure != null -> outcome.completeExceptionally(IOException(failure))
            code != null -> outcome.complete(code)
        }
    }

    private fun page(failure: String?) = if (failure == null) {
        "<h2>Sign in successful!</h2>" +
                "<p>You can close this window and return to Tasks.</p>" +
                "<script>setTimeout(function(){ window.close(); }, 2000)</script>"
    } else {
        "<h2>Sign in failed</h2><p>${failure.htmlEscape()}</p>"
    }

    private fun reopenBrowser() {
        val url = attemptUrl ?: return
        try {
            openUrl(url)
        } catch (e: Exception) {
            logger.w(e) { "Failed to reopen the browser" }
        }
    }

    private fun returnToApp() = try {
        onCallback()
    } catch (e: Exception) {
        logger.w(e) { "Failed to return to the app" }
    }

    private fun Socket.requestTarget(): String =
        BufferedReader(InputStreamReader(getInputStream()))
            .readLine()
            .orEmpty()
            .split(" ")
            .getOrElse(1) { "" }

    private fun String.callbackParams(): Map<String, String>? {
        val uri = try {
            URI(this)
        } catch (e: URISyntaxException) {
            return null
        }
        val path = uri.path.orEmpty().trimEnd('/').ifEmpty { "/" }
        if (path != CALLBACK_PATH) {
            return null
        }
        return uri.rawQuery.orEmpty().params()
    }

    private fun String.params(): Map<String, String> = split("&")
        .filter { it.contains("=") }
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key to URLDecoder.decode(value, "UTF-8")
        }

    private fun authorizationUrl(redirectUri: String, state: String) = buildString {
        append(GITHUB_AUTHORIZE_URL)
        append("?client_id=")
        append(URLEncoder.encode(clientId, "UTF-8"))
        append("&redirect_uri=")
        append(URLEncoder.encode(redirectUri, "UTF-8"))
        append("&state=")
        append(URLEncoder.encode(state, "UTF-8"))
    }

    private fun generateState(): String {
        val bytes = ByteArray(STATE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun Socket.respondQuietly(status: String, body: String) = try {
        val page = "<html><body>$body</body></html>".toByteArray()
        val response = "HTTP/1.1 $status\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: ${page.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        getOutputStream().apply {
            write(response.toByteArray())
            write(page)
            flush()
        }
    } catch (e: IOException) {
        logger.d(e) { "Failed to answer the callback request" }
    }

    private fun Socket.drainQuietly() = try {
        soTimeout = DRAIN_TIMEOUT_MILLIS
        shutdownOutput()
        val buffer = ByteArray(DRAIN_BUFFER_BYTES)
        var drained = 0
        while (drained < MAX_DRAIN_BYTES) {
            val read = getInputStream().read(buffer)
            if (read < 0) {
                break
            }
            drained += read
        }
    } catch (e: IOException) {
        logger.d(e) { "Failed to drain the callback request" }
    }

    private fun Socket.closeQuietly() = try {
        close()
    } catch (e: Exception) {
        logger.d(e) { "Failed to close the callback socket" }
    }

    companion object {
        private const val TAG = "GitHubSponsorClient"
        private const val LOOPBACK = "127.0.0.1"
        private const val CALLBACK_PATH = "/callback"
        private const val GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
        private const val GITHUB_CLIENT_ID = "Ov23limM74bOgBgCBrJ5"
        private const val SIGN_IN_TIMEOUT_MILLIS = 5 * 60 * 1000L
        private const val READ_TIMEOUT_MILLIS = 10 * 1000
        private const val ACCEPT_POLL_MILLIS = 250
        private const val DRAIN_TIMEOUT_MILLIS = 250
        private const val DRAIN_BUFFER_BYTES = 8 * 1024
        private const val MAX_DRAIN_BYTES = 64 * 1024
        private const val BACKLOG = 5
        private const val STATE_BYTES = 32
        private const val OK = "200 OK"
        private const val NOT_FOUND = "404 Not Found"
    }
}
