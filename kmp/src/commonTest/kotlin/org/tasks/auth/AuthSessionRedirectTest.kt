package org.tasks.auth

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.tasks.billing.GitHubSponsorApp
import org.tasks.billing.GitHubSponsorClient.VerifyResult
import org.tasks.billing.GitHubSponsorOAuthClient
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthSessionRedirectTest {

    private val sponsorRedirect = "org.tasks.github.sponsor://oauth2redirect"
    private val verified = mutableListOf<Pair<String, String>>()
    private var authUrl: String? = null
    private var callbackScheme: String? = null
    private val opened = mutableListOf<String>()

    private fun redirect(callback: suspend (authUrl: String) -> String) = AuthSessionRedirect { url, scheme ->
        authUrl = url
        callbackScheme = scheme
        callback(url)
    }

    private fun echoState(params: String = "code=abc"): suspend (String) -> String = { url ->
        "$sponsorRedirect?$params&state=${Url(url).parameters["state"]}"
    }

    private fun TestScope.sponsorClient(
        callback: suspend (authUrl: String) -> String = echoState(),
        result: VerifyResult = VerifyResult.Success,
    ) = GitHubSponsorOAuthClient(
        verify = { code, uri ->
            verified += code to uri
            result
        },
        redirect = redirect(callback),
        scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext.job)),
    )

    @Test
    fun usesTheIosClient() {
        assertEquals(GitHubSponsorApp.ios, redirect { it }.client(GitHubSponsorApp))
        assertEquals(OAuthProvider.GITHUB.ios, redirect { it }.client(OAuthProvider.GITHUB))
    }

    @Test
    fun capturesTheCallbackOnTheRegisteredScheme() = runTest {
        val result = redirect { "$sponsorRedirect?code=a%2Bb&state=s+1" }.await(
            app = GitHubSponsorApp,
            openUrl = { opened += it },
            authUrl = { "https://example.org/authorize?redirect_uri=$it" },
            expectedState = { "s 1" },
        )

        assertEquals("org.tasks.github.sponsor", callbackScheme)
        assertEquals("https://example.org/authorize?redirect_uri=$sponsorRedirect", authUrl)
        assertEquals(sponsorRedirect, result.redirectUri)
        assertEquals("a+b", result.authorizedCode("s 1"))
        assertTrue(opened.isEmpty())
    }

    @Test
    fun googleRedirectsUseTheReversedIosClientId() {
        listOf(OAuthProvider.GOOGLE, OAuthProvider.GOOGLE_TASKS).forEach {
            assertEquals(GoogleOAuth.redirectUri(GoogleOAuth.scheme(it.ios.clientId)), it.ios.redirectUri)
        }
    }

    @Test
    fun capturesGoogleCallbacksOnTheReversedClientIdScheme() = runTest {
        val result = redirect { "${OAuthProvider.GOOGLE.ios.redirectUri}?code=c" }
            .await(OAuthProvider.GOOGLE, {}, { it }, { null })

        assertEquals("com.googleusercontent.apps.363426363175-nar1o932el9fi4gogs0mgsq8160epu4t", callbackScheme)
        assertEquals(OAuthProvider.GOOGLE.ios.redirectUri, result.redirectUri)
    }

    @Test
    fun anAppWithoutAnIosRedirectIsNotConfigured() = runTest {
        assertFailsWith<IOException> {
            redirect { it }.await(OAuthProvider.APPLE, {}, { it }, { null })
        }

        assertNull(authUrl)
    }

    @Test
    fun ignoresTheFragmentAndDecodesKeys() = runTest {
        val result = redirect { "$sponsorRedirect?c%6Fde=abc&state=s#_" }
            .await(GitHubSponsorApp, {}, { it }, { "s" })

        assertEquals("abc", result.authorizedCode("s"))
    }

    @Test
    fun dropsOnlyTheParameterThatCannotBeDecoded() = runTest {
        val result = redirect { "$sponsorRedirect?code=abc&state=s&junk=%E" }
            .await(GitHubSponsorApp, {}, { it }, { "s" })

        assertEquals(mapOf("code" to "abc", "state" to "s"), result.params)
    }

    @Test
    fun aNewSignInDismissesASessionThatNeverFinished() = runTest {
        var dismissed = false
        var sessions = 0
        val redirect = AuthSessionRedirect { url, _ ->
            if (++sessions == 1) {
                try {
                    awaitCancellation()
                } finally {
                    dismissed = true
                }
            }
            "$sponsorRedirect?code=c&state=${Url(url).parameters["state"]}"
        }
        val first = async {
            redirect.await(GitHubSponsorApp, {}, { "https://example.org/a?state=1" }, { "1" })
        }
        runCurrent()

        val second = redirect.await(GitHubSponsorApp, {}, { "https://example.org/b?state=2" }, { "2" })
        runCurrent()

        assertEquals("c", second.authorizedCode("2"))
        assertTrue(dismissed)
        assertTrue(first.isCancelled)
    }

    @Test
    fun aReplacedSessionLeavesItsReplacementReopenable() = runTest {
        val redirect = redirect { awaitCancellation() }
        val first = launch { redirect.await(GitHubSponsorApp, {}, { "https://example.org/a" }, { null }) }
        runCurrent()
        val second = launch { redirect.await(GitHubSponsorApp, {}, { "https://example.org/b" }, { null }) }
        runCurrent()

        assertTrue(first.isCancelled)
        assertTrue(second.isActive)
        redirect.reopen({}, "https://example.org/b")
        runCurrent()
        assertTrue(second.isCancelled)
    }

    @Test
    fun reopeningCancelsOnlyTheSessionForThatUrl() = runTest {
        val redirect = redirect { awaitCancellation() }
        val pending = launch {
            redirect.await(GitHubSponsorApp, {}, { "https://example.org/a" }, { null })
        }
        runCurrent()

        redirect.reopen({}, "https://example.org/b")
        runCurrent()
        assertTrue(pending.isActive)

        redirect.reopen({}, "https://example.org/a")
        runCurrent()
        assertTrue(pending.isCancelled)
    }

    @Test
    fun signingInAgainReplacesASessionThatNeverFinished() = runTest {
        val sessions = mutableListOf<String>()
        val client = sponsorClient(callback = { url ->
            sessions += url
            if (sessions.size == 1) awaitCancellation()
            echoState()(url)
        })
        val first = async { client.signIn {} }
        runCurrent()

        assertEquals(VerifyResult.Success, client.signIn {})
        assertEquals(VerifyResult.Cancelled, first.await())
        assertEquals(2, sessions.size)
        assertEquals(1, verified.size)
    }

    @Test
    fun aReplacedSessionCanItselfBeReplaced() = runTest {
        val sessions = mutableListOf<String>()
        val client = sponsorClient(callback = { url ->
            sessions += url
            if (sessions.size < 3) awaitCancellation()
            echoState()(url)
        })
        val first = async { client.signIn {} }
        runCurrent()
        val second = async { client.signIn {} }
        runCurrent()

        assertEquals(VerifyResult.Success, client.signIn {})
        assertEquals(VerifyResult.Cancelled, first.await())
        assertEquals(VerifyResult.Cancelled, second.await())
        assertEquals(3, sessions.size)
    }

    @Test
    fun neverReopensTheBrowserItself() {
        redirect { it }.reopen({ opened += it }, "https://example.org")

        assertTrue(opened.isEmpty())
    }

    @Test
    fun sponsorSignInOpensGitHubWithTheIosRedirect() = runTest {
        sponsorClient().signIn { opened += it }

        val url = Url(authUrl!!)
        assertEquals("https://github.com/login/oauth/authorize", url.toString().substringBefore('?'))
        assertEquals(GitHubSponsorApp.ios.clientId, url.parameters["client_id"])
        assertEquals(sponsorRedirect, url.parameters["redirect_uri"])
        assertTrue(opened.isEmpty())
    }

    @Test
    fun sponsorSignInVerifiesTheCodeAgainstTheRedirectItWasIssuedFor() = runTest {
        assertEquals(VerifyResult.NotSponsor, sponsorClient(result = VerifyResult.NotSponsor).signIn {})

        assertEquals(listOf("abc" to sponsorRedirect), verified)
    }

    @Test
    fun aMismatchedStateIsRejectedWithoutVerifying() = runTest {
        val result = sponsorClient(callback = { "$sponsorRedirect?code=abc&state=other" }).signIn {}

        assertEquals(VerifyResult.Failed, result)
        assertTrue(verified.isEmpty())
    }

    @Test
    fun anAuthorizationErrorFails() = runTest {
        val result = sponsorClient(callback = echoState("error=access_denied")).signIn {}

        assertEquals(VerifyResult.Failed, result)
        assertTrue(verified.isEmpty())
    }

    @Test
    fun closingTheBrowserIsACancellation() = runTest {
        val result = sponsorClient(callback = { throw CancellationException("Sign in cancelled") }).signIn {}

        assertEquals(VerifyResult.Cancelled, result)
    }

    @Test
    fun aSessionFailureFails() = runTest {
        val result = sponsorClient(callback = { throw Exception("Could not start the sign-in session") }).signIn {}

        assertEquals(VerifyResult.Failed, result)
    }
}
