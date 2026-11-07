package org.tasks.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.auth.LoopbackRedirect
import org.tasks.auth.OAuthApp
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.RedirectResult
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors

class GitHubSponsorOAuthClientTest {

    @Test(timeout = 30_000)
    fun aSignInThatCannotOpenABrowserFailsAtOnce() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = loopback(),
        )

        assertEquals(
            GitHubSponsorClient.VerifyResult.Failed,
            client.signIn { throw IOException("nothing on this system could open it") },
        )
    }

    @Test(timeout = 30_000)
    fun theCodeGitHubRedirectsBackWithIsHandedToVerification() = runBlocking {
        var verified: Pair<String, String>? = null
        val client = GitHubSponsorOAuthClient(
            verify = { code, redirectUri ->
                verified = code to redirectUri
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl -> redirectBack(authUrl, code = "the-code") }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
        assertEquals("the-code", verified?.first)
    }

    @Test(timeout = 30_000)
    fun theRedirectUriCarriesTheRegisteredCallbackPath() = runBlocking {
        var authUrl: String? = null
        val client = GitHubSponsorOAuthClient(
            verify = { _, redirectUri ->
                assertEquals("http://127.0.0.1:" + port(authUrl!!) + "/callback", redirectUri)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { url ->
            authUrl = url
            redirectBack(url, code = "the-code")
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun gitHubIsAskedToShowTheAccountChooser() = runBlocking {
        var authUrl: String? = null
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = loopback(),
        )

        client.signIn { url ->
            authUrl = url
            redirectBack(url, code = "the-code")
        }

        assertEquals("select_account", params(authUrl!!)["prompt"])
    }

    @Test(timeout = 30_000)
    fun aStrayBrowserRequestDoesNotEndTheFlow() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            Thread {
                get(URI(params(authUrl).getValue("redirect_uri") + "/../favicon.ico"))
                get(callback(authUrl, code = "the-code"))
            }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun aRedirectCarryingTheWrongStateIsRefused() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            redirectBack(authUrl, code = "the-code", state = "forged")
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, result)
    }

    @Test(timeout = 30_000)
    fun anAuthorizationThatComesBackWithAnErrorFails() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl -> redirectBack(authUrl, error = "access_denied") }

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, result)
    }

    @Test(timeout = 30_000)
    fun aConnectionThatSendsNothingDoesNotDelayTheCallback() = runBlocking {
        var idle: Socket? = null
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val started = System.currentTimeMillis()
        val result = client.signIn { authUrl ->
            val redirectUri = URI(params(authUrl).getValue("redirect_uri"))
            idle = Socket(redirectUri.host, redirectUri.port)
            redirectBack(authUrl, code = "the-code")
        }
        val elapsed = System.currentTimeMillis() - started
        idle?.close()

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
        assertTrue("the sign in took ${elapsed}ms", elapsed < 5_000)
    }

    @Test(timeout = 30_000)
    fun theCodeSurvivesABrowserThatHangsUp() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread { send(uri, path(uri), readResponse = false) }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun anAbsoluteRequestTargetIsStillTheCallback() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread { send(uri, uri.toString()) }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun aTrailingSlashIsStillTheCallback() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread { send(uri, uri.rawPath + "/?" + uri.rawQuery) }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun aRedirectCarryingTheWrongStateDoesNotClaimSuccess() = runBlocking {
        val page = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code", state = "forged")
            Thread { page.complete(send(uri, path(uri))) }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, result)
        assertTrue(page.await().contains("Sign in failed"))
    }

    @Test(timeout = 30_000)
    fun theFlowFinishesAfterTheCallerGoesAway() = runBlocking {
        val opened = CompletableDeferred<String>()
        val verified = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                verified.complete(code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = LoopbackRedirect(
                path = GitHubSponsorApp.LOOPBACK_PATH,
                timeoutMillis = 20_000,
            ),
        )

        val caller = launch { client.signIn { opened.complete(it) } }
        val authUrl = opened.await()
        caller.cancelAndJoin()
        redirectBack(authUrl, code = "the-code")

        assertEquals("the-code", verified.await())
    }

    @Test(timeout = 30_000)
    fun thePendingAttemptLetsGoOfACallerThatWentAway() = runBlocking {
        val opened = CompletableDeferred<String>()
        val verified = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                verified.complete(code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = LoopbackRedirect(
                path = GitHubSponsorApp.LOOPBACK_PATH,
                timeoutMillis = 20_000,
            ),
        )

        val (caller, activity) = signInFromAnActivity(client, opened)
        val authUrl = opened.await()
        caller.cancelAndJoin()
        repeat(50) {
            if (activity.get() != null) {
                System.gc()
                Thread.sleep(20)
            }
        }

        assertNull(activity.get())
        redirectBack(authUrl, code = "the-code")
        assertEquals("the-code", verified.await())
    }

    private fun CoroutineScope.signInFromAnActivity(
        client: GitHubSponsorClient,
        opened: CompletableDeferred<String>,
    ): Pair<Job, WeakReference<Any>> {
        val activity = Any()
        val caller = launch {
            client.signIn { url ->
                activity.hashCode()
                opened.complete(url)
            }
        }
        return caller to WeakReference(activity)
    }

    @Test(timeout = 30_000)
    fun aCancelledSignInIsNotReportedAsAFailure() = runBlocking {
        val opened = CompletableDeferred<String>()
        var result: GitHubSponsorClient.VerifyResult? = null
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = LoopbackRedirect(
                path = GitHubSponsorApp.LOOPBACK_PATH,
                timeoutMillis = 2_000,
            ),
        )

        val caller = launch { result = client.signIn { opened.complete(it) } }
        opened.await()
        caller.cancelAndJoin()

        assertNull(result)
    }

    @Test(timeout = 30_000)
    fun returningToTheFlowReopensTheBrowser() = runBlocking {
        val opened = mutableListOf<String>()
        val firstOpen = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = LoopbackRedirect(
                path = GitHubSponsorApp.LOOPBACK_PATH,
                timeoutMillis = 20_000,
            ),
        )
        val openUrl: (String) -> Unit = { url ->
            synchronized(opened) { opened.add(url) }
            firstOpen.complete(url)
        }

        val caller = launch { client.signIn(openUrl) }
        val authUrl = firstOpen.await()
        caller.cancelAndJoin()
        val resumed = launch { client.signIn(openUrl) }
        redirectBack(authUrl, code = "the-code")
        resumed.join()

        assertEquals(listOf(authUrl, authUrl), synchronized(opened) { opened.toList() })
    }

    @Test(timeout = 30_000)
    fun aMalformedEscapeDoesNotEndTheFlow() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread {
                send(uri, uri.rawPath + "?x=%zz")
                get(uri)
            }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
    }

    @Test(timeout = 30_000)
    fun aRequestOffTheCallbackPathIsNotTheRedirect() = runBlocking {
        val stray = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { code, _ ->
                assertEquals("the-code", code)
                GitHubSponsorClient.VerifyResult.Success
            },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread {
                stray.complete(send(uri, "/favicon.ico?" + uri.rawQuery))
                get(uri)
            }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Success, result)
        assertTrue(stray.await().startsWith("HTTP/1.1 404"))
    }

    @Test(timeout = 30_000)
    fun anErrorWithoutStateShowsTheProvidersError() = runBlocking {
        val page = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = loopback(),
        )

        val result = client.signIn { authUrl ->
            val uri = URI(params(authUrl).getValue("redirect_uri"))
            Thread {
                page.complete(send(uri, uri.rawPath + "?error=access_denied&error_description=Nope"))
            }.start()
        }

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, result)
        assertTrue(page.await().contains("Nope"))
    }

    @Test(timeout = 30_000)
    fun theBrowserReopensWhenTheRedirectUriIsReadyBeforeTheAttemptIsRecorded() = runBlocking {
        val opened = mutableListOf<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = redirect { authUrl ->
                authUrl("http://127.0.0.1:1/callback")
                awaitCancellation()
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        val first = launch { client.signIn { } }
        yield()
        val second = launch { client.signIn { synchronized(opened) { opened.add(it) } } }
        yield()

        assertEquals(1, synchronized(opened) { opened.size })
        first.cancelAndJoin()
        second.cancelAndJoin()
    }

    @Test(timeout = 30_000)
    fun theSuccessPageClosesItself() = runBlocking {
        val page = CompletableDeferred<String>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = loopback(),
        )
        client.signIn { authUrl ->
            val uri = callback(authUrl, code = "the-code")
            Thread { page.complete(send(uri, path(uri))) }.start()
        }

        assertTrue(page.await().contains("window.close()"))
    }

    @Test(timeout = 30_000)
    fun theListenerDoesNotHoldOntoTheCallersThread() = runBlocking {
        val caller = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            val client = GitHubSponsorOAuthClient(
                verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
                redirect = LoopbackRedirect(
                    timeoutMillis = 2_000,
                ),
            )
            val waiting = launch(caller) { client.signIn { } }
            val ranOnTheSameThread = CompletableDeferred<Unit>()
            launch(caller) { ranOnTheSameThread.complete(Unit) }

            withTimeout(10_000) { ranOnTheSameThread.await() }

            waiting.cancelAndJoin()
        } finally {
            caller.close()
        }
    }

    @Test(timeout = 30_000)
    fun theAuthorizationNamesTheSponsorApp() = runBlocking {
        var authUrl: String? = null
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = loopback(),
        )

        client.signIn { url ->
            authUrl = url
            redirectBack(url, code = "the-code")
        }

        assertEquals("Ov23limM74bOgBgCBrJ5", params(authUrl!!)["client_id"])
    }

    @Test(timeout = 30_000)
    fun anAttemptThatEndsWithoutARedirectIsCancelledNotFailed() = runBlocking {
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = redirect { throw CancellationException("Sign in cancelled") },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Cancelled, client.signIn { })
    }

    @Test(timeout = 30_000)
    fun anAttemptThatTimesOutFailsAndTheNextSignInStartsOver() = runBlocking {
        var attempts = 0
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = redirect { authUrl ->
                attempts++
                authUrl("http://127.0.0.1:1/callback")
                throw IOException("Timed out waiting for the sign-in to finish")
            },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn { })
        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn { })
        assertEquals(2, attempts)
    }

    @Test(timeout = 30_000)
    fun returningToTheFlowLetsTheRedirectBringTheBrowserBack() = runBlocking {
        val reopened = mutableListOf<String>()
        val callerOpened = mutableListOf<String>()
        val firstOpen = CompletableDeferred<Unit>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> GitHubSponsorClient.VerifyResult.Success },
            redirect = object : OAuthRedirect {
                override suspend fun await(
                    app: OAuthApp,
                    openUrl: (String) -> Unit,
                    authUrl: (redirectUri: String) -> String,
                    expectedState: () -> String?,
                ): RedirectResult {
                    authUrl("org.tasks.github.sponsor://oauth2redirect")
                    firstOpen.complete(Unit)
                    awaitCancellation()
                }

                override fun reopen(openUrl: (String) -> Unit, authUrl: String) {
                    synchronized(reopened) { reopened.add(authUrl) }
                }
            },
        )

        val first = launch { client.signIn { synchronized(callerOpened) { callerOpened.add(it) } } }
        firstOpen.await()
        first.cancelAndJoin()
        val second = launch { client.signIn { synchronized(callerOpened) { callerOpened.add(it) } } }
        withTimeout(10_000) {
            while (synchronized(reopened) { reopened.isEmpty() }) yield()
        }
        second.cancelAndJoin()

        assertEquals(1, synchronized(reopened) { reopened.size })
        assertTrue(synchronized(callerOpened) { callerOpened.isEmpty() })
    }

    @Test(timeout = 30_000)
    fun signingInAgainWhileTheCodeIsBeingVerifiedDoesNotReopenTheBrowser() = runBlocking {
        val reopened = mutableListOf<String>()
        val verifying = CompletableDeferred<Unit>()
        val verified = CompletableDeferred<GitHubSponsorClient.VerifyResult>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ ->
                verifying.complete(Unit)
                verified.await()
            },
            redirect = object : OAuthRedirect {
                override suspend fun await(
                    app: OAuthApp,
                    openUrl: (String) -> Unit,
                    authUrl: (redirectUri: String) -> String,
                    expectedState: () -> String?,
                ): RedirectResult {
                    authUrl("org.tasks.github.sponsor://oauth2redirect")
                    return RedirectResult(
                        "org.tasks.github.sponsor://oauth2redirect",
                        mapOf("code" to "the-code", "state" to expectedState()!!),
                    )
                }

                override fun reopen(openUrl: (String) -> Unit, authUrl: String) {
                    synchronized(reopened) { reopened.add(authUrl) }
                }
            },
        )

        val first = async { client.signIn { } }
        verifying.await()
        val second = async { client.signIn { } }
        yield()
        verified.complete(GitHubSponsorClient.VerifyResult.Success)

        assertEquals(GitHubSponsorClient.VerifyResult.Success, first.await())
        assertEquals(GitHubSponsorClient.VerifyResult.Success, second.await())
        assertTrue(synchronized(reopened) { reopened.isEmpty() })
    }

    @Test(timeout = 30_000)
    fun aRedirectThatOpensItsOwnBrowserStillNoticesTheCallerWentAway() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val abandoned = CompletableDeferred<String?>()
        val client = GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = redirect { authUrl ->
                started.complete(Unit)
                gate.await()
                try {
                    authUrl("org.tasks.github.sponsor://oauth2redirect")
                } catch (e: IOException) {
                    abandoned.complete(e.message)
                    throw e
                }
                abandoned.complete(null)
                awaitCancellation()
            },
        )

        val caller = launch { client.signIn { } }
        started.await()
        caller.cancelAndJoin()
        gate.complete(Unit)

        assertEquals("The sign-in was abandoned", abandoned.await())
    }

    private fun loopback() = LoopbackRedirect(path = GitHubSponsorApp.LOOPBACK_PATH)

    private fun redirect(
        await: suspend (authUrl: (redirectUri: String) -> String) -> RedirectResult,
    ) = object : OAuthRedirect {
        override suspend fun await(
            app: OAuthApp,
            openUrl: (String) -> Unit,
            authUrl: (redirectUri: String) -> String,
            expectedState: () -> String?,
        ): RedirectResult = await(authUrl)
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

    private fun path(uri: URI) = uri.rawPath.ifEmpty { "/" } + "?" + uri.rawQuery

    private fun port(authUrl: String) = URI(params(authUrl).getValue("redirect_uri")).port

    private fun params(authUrl: String): Map<String, String> = authUrl
        .substringAfter("?")
        .split("&")
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key to java.net.URLDecoder.decode(value, "UTF-8")
        }
}
