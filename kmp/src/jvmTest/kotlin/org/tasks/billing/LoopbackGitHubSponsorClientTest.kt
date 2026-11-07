package org.tasks.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI

class LoopbackGitHubSponsorClientTest {

    @Test(timeout = 30_000)
    fun aSignInThatCannotOpenABrowserFailsAtOnce() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { _, _ -> error("should not reach verification") },
            openUrl = { throw IOException("nothing on this system could open it") },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn())
    }

    @Test(timeout = 30_000)
    fun theCodeGitHubRedirectsBackWithIsHandedToVerification() = runBlocking {
        var verified: Pair<String, String>? = null
        val client = LoopbackGitHubSponsorClient(
            verify = { code, redirectUri ->
                verified = code to redirectUri
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl -> redirectBack(authUrl, code = "the-code") },
        )

        val result = client.signIn()

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
        assertEquals("the-code", verified?.first)
    }

    @Test(timeout = 30_000)
    fun theRedirectUriCarriesTheRegisteredCallbackPath() = runBlocking {
        var authUrl: String? = null
        val client = LoopbackGitHubSponsorClient(
            verify = { _, redirectUri ->
                assertEquals("http://127.0.0.1:" + port(authUrl!!) + "/callback", redirectUri)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { url ->
                authUrl = url
                redirectBack(url, code = "the-code")
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Success, client.signIn())
    }

    @Test(timeout = 30_000)
    fun aStrayBrowserRequestDoesNotEndTheFlow() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl ->
                Thread {
                    get(URI(params(authUrl).getValue("redirect_uri") + "/../favicon.ico"))
                    get(callback(authUrl, code = "the-code"))
                }.start()
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Success, client.signIn())
    }

    @Test(timeout = 30_000)
    fun aRedirectCarryingTheWrongStateIsRefused() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { _, _ -> error("should not reach verification") },
            openUrl = { authUrl -> redirectBack(authUrl, code = "the-code", state = "forged") },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn())
    }

    @Test(timeout = 30_000)
    fun anAuthorizationThatComesBackWithAnErrorFails() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { _, _ -> error("should not reach verification") },
            openUrl = { authUrl -> redirectBack(authUrl, error = "access_denied") },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn())
    }

    @Test(timeout = 30_000)
    fun aConnectionThatSendsNothingDoesNotDelayTheCallback() = runBlocking {
        var idle: Socket? = null
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl ->
                val redirectUri = URI(params(authUrl).getValue("redirect_uri"))
                idle = Socket(redirectUri.host, redirectUri.port)
                redirectBack(authUrl, code = "the-code")
            },
        )

        val started = System.currentTimeMillis()
        val result = client.signIn()
        val elapsed = System.currentTimeMillis() - started
        idle?.close()

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
        assertTrue("the sign in took ${elapsed}ms", elapsed < 5_000)
    }

    @Test(timeout = 30_000)
    fun theCodeSurvivesABrowserThatHangsUp() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl ->
                val uri = callback(authUrl, code = "the-code")
                Thread { send(uri, path(uri), readResponse = false) }.start()
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Success, client.signIn())
    }

    @Test(timeout = 30_000)
    fun anAbsoluteRequestTargetIsStillTheCallback() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl ->
                val uri = callback(authUrl, code = "the-code")
                Thread { send(uri, uri.toString()) }.start()
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Success, client.signIn())
    }

    @Test(timeout = 30_000)
    fun aTrailingSlashIsStillTheCallback() = runBlocking {
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { authUrl ->
                val uri = callback(authUrl, code = "the-code")
                Thread { send(uri, "/callback/?" + uri.rawQuery) }.start()
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Success, client.signIn())
    }

    @Test(timeout = 30_000)
    fun aRedirectCarryingTheWrongStateDoesNotClaimSuccess() = runBlocking {
        val page = CompletableDeferred<String>()
        val client = LoopbackGitHubSponsorClient(
            verify = { _, _ -> error("should not reach verification") },
            openUrl = { authUrl ->
                val uri = callback(authUrl, code = "the-code", state = "forged")
                Thread { page.complete(send(uri, path(uri))) }.start()
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn())
        assertTrue(page.await().contains("Sign in failed"))
    }

    @Test(timeout = 30_000)
    fun theFlowFinishesAfterTheCallerGoesAway() = runBlocking {
        val opened = CompletableDeferred<String>()
        val verified = CompletableDeferred<String>()
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                verified.complete(code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { opened.complete(it) },
            signInTimeoutMillis = 20_000,
        )

        val caller = launch { client.signIn() }
        val authUrl = opened.await()
        caller.cancelAndJoin()
        redirectBack(authUrl, code = "the-code")

        assertEquals("the-code", verified.await())
    }

    @Test(timeout = 30_000)
    fun aCancelledSignInIsNotReportedAsAFailure() = runBlocking {
        val opened = CompletableDeferred<String>()
        var result: GitHubSponsorClient.VerifyResult? = null
        val client = LoopbackGitHubSponsorClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            openUrl = { opened.complete(it) },
            signInTimeoutMillis = 2_000,
        )

        val caller = launch { result = client.signIn() }
        opened.await()
        caller.cancelAndJoin()

        assertNull(result)
    }

    @Test(timeout = 30_000)
    fun returningToTheFlowReopensTheBrowser() = runBlocking {
        val opened = mutableListOf<String>()
        val firstOpen = CompletableDeferred<String>()
        val client = LoopbackGitHubSponsorClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            openUrl = { url ->
                synchronized(opened) { opened.add(url) }
                firstOpen.complete(url)
            },
            signInTimeoutMillis = 20_000,
        )

        val caller = launch { client.signIn() }
        val authUrl = firstOpen.await()
        caller.cancelAndJoin()
        val resumed = launch { client.signIn() }
        redirectBack(authUrl, code = "the-code")
        resumed.join()

        assertEquals(listOf(authUrl, authUrl), synchronized(opened) { opened.toList() })
    }

    private fun redirectBack(
        authUrl: String,
        code: String? = null,
        state: String? = null,
        error: String? = null,
    ) {
        val uri = callback(authUrl, code, state, error)
        Thread { get(uri) }.start()
    }

    private fun callback(
        authUrl: String,
        code: String? = null,
        state: String? = null,
        error: String? = null,
    ): URI {
        val params = params(authUrl)
        val query = buildList {
            code?.let { add("code=$it") }
            add("state=${state ?: params.getValue("state")}")
            error?.let { add("error=$it") }
        }.joinToString("&")
        return URI(params.getValue("redirect_uri") + "?" + query)
    }

    private fun get(uri: URI) {
        try {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.inputStream.use { it.readBytes() }
            connection.disconnect()
        } catch (ignored: Exception) {
        }
    }

    private fun send(uri: URI, target: String, readResponse: Boolean = true): String = try {
        Socket(uri.host, uri.port).use { socket ->
            val request = "GET $target HTTP/1.1\r\n" +
                    "Host: ${uri.host}:${uri.port}\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            socket.getOutputStream().apply {
                write(request.toByteArray())
                flush()
            }
            if (readResponse) socket.getInputStream().readBytes().decodeToString() else ""
        }
    } catch (ignored: Exception) {
        ""
    }

    private fun path(uri: URI) = uri.rawPath + "?" + uri.rawQuery

    private fun port(authUrl: String) = URI(params(authUrl).getValue("redirect_uri")).port

    private fun params(authUrl: String): Map<String, String> = authUrl
        .substringAfter("?")
        .split("&")
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key to java.net.URLDecoder.decode(value, "UTF-8")
        }
}
