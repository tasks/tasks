package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import java.util.concurrent.ConcurrentHashMap

class LoopbackRedirect(
    private val host: String = LOOPBACK_HOST,
    private val path: String = "",
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
    private val onRedirect: () -> Unit = {},
    private val closeBrowserTab: Boolean = true,
) : OAuthRedirect {
    private val logger = Logger.withTag(TAG)

    override suspend fun await(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult = await(openUrl, authUrl, expectedState)

    suspend fun await(
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult = withContext(Dispatchers.IO) {
        ServerSocket(0, BACKLOG, InetAddress.getByName(host)).use { serverSocket ->
            serverSocket.soTimeout = ACCEPT_POLL_MILLIS
            val redirectUri = "http://$host:${serverSocket.localPort}$path"
            logger.d { "Listening on ${serverSocket.localPort}" }

            openUrl(authUrl(redirectUri))

            val result = withTimeoutOrNull(timeoutMillis) {
                serverSocket.awaitRedirect(redirectUri, expectedState)
            } ?: throw IOException("Timed out waiting for the sign-in to finish")
            returnToApp()
            result
        }
    }

    private fun returnToApp() = try {
        onRedirect()
    } catch (e: Exception) {
        logger.w(e) { "Failed to return to the app" }
    }

    private suspend fun ServerSocket.awaitRedirect(
        redirectUri: String,
        expectedState: () -> String?,
    ): RedirectResult = coroutineScope {
        val live = ConcurrentHashMap.newKeySet<Socket>()
        val redirect = CompletableDeferred<RedirectResult>()
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
                if (!isActive) {
                    live.remove(socket)
                    socket.closeQuietly()
                    break
                }
                launch {
                    try {
                        socket.readRedirect(redirectUri, expectedState, redirect)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.w(e) { "Ignoring a request that could not be handled" }
                    } finally {
                        live.remove(socket)
                        socket.closeQuietly()
                    }
                }
            }
        }
        try {
            redirect.await()
        } finally {
            accepting.cancel()
            live.forEach { it.closeQuietly() }
        }
    }

    private fun Socket.readRedirect(
        redirectUri: String,
        expectedState: () -> String?,
        redirect: CompletableDeferred<RedirectResult>,
    ) {
        soTimeout = READ_TIMEOUT_MILLIS
        val target = try {
            readTarget()
        } catch (e: IOException) {
            logger.d { "Ignoring a connection that sent no request: ${e.message}" }
            return
        }
        val params = target.redirectParams()
        if (!target.isCallback() || params.keys.none { it in REDIRECT_PARAMS }) {
            logger.d { "Ignoring a request that is not the redirect" }
            respondQuietly(NOT_FOUND, "<h2>Not found</h2>")
            return
        }
        val result = RedirectResult(redirectUri, params)
        respondQuietly(OK, page(result, result.failure(expectedState())))
        drainQuietly()
        redirect.complete(result)
    }

    private fun Socket.readTarget(): String =
        BufferedReader(InputStreamReader(getInputStream()))
            .readLine()
            .orEmpty()
            .split(" ")
            .getOrElse(1) { "" }

    private fun String.isCallback(): Boolean {
        val requestPath = substringBefore("?").let {
            if (it.contains("://")) "/" + it.substringAfter("://").substringAfter("/", "") else it
        }
        return requestPath.trimEnd('/') == path.trimEnd('/')
    }

    private fun page(result: RedirectResult, failure: String?): String =
        if (result.code != null && failure == null) {
            "<h2>Sign in successful!</h2>" +
                    "<p>You can close this window and return to Tasks.</p>" +
                    if (closeBrowserTab) "<script>setTimeout(function(){ window.close(); }, 2000)</script>" else ""
        } else {
            "<h2>Sign in failed</h2><p>${(failure ?: "Unknown error").htmlEscape()}</p>"
        }

    private fun Socket.respondQuietly(status: String, body: String) = try {
        val page = "<html><body>$body</body></html>".toByteArray()
        val headers = "HTTP/1.1 $status\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: ${page.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        getOutputStream().apply {
            write(headers.toByteArray())
            write(page)
            flush()
        }
    } catch (e: IOException) {
        logger.d(e) { "Failed to answer the redirect" }
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
        logger.d(e) { "Failed to drain the redirect request" }
    }

    private fun Socket.closeQuietly() = try {
        close()
    } catch (e: Exception) {
        logger.d(e) { "Failed to close the redirect socket" }
    }

    companion object {
        private const val TAG = "LoopbackRedirect"
        const val LOOPBACK_HOST = "127.0.0.1"
        private const val TIMEOUT_MILLIS = 5 * 60 * 1000L
        private const val BACKLOG = 5
        private const val ACCEPT_POLL_MILLIS = 250
        private const val READ_TIMEOUT_MILLIS = 10 * 1000
        private const val DRAIN_TIMEOUT_MILLIS = 250
        private const val DRAIN_BUFFER_BYTES = 8 * 1024
        private const val MAX_DRAIN_BYTES = 64 * 1024
        private const val OK = "200 OK"
        private const val NOT_FOUND = "404 Not Found"
        private val REDIRECT_PARAMS = setOf("code", "error", "state")
    }
}
