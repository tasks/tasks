package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences

class TasksOAuthSignInAuthUrlTest {
    private val requested = mutableListOf<String>()

    private val oauthClient = TasksOAuthClient(
        HttpClient(MockEngine { request ->
            requested.add(request.url.toString())
            respond(
                """{
                    "authorization_endpoint": "https://accounts.example.com/authorize",
                    "token_endpoint": "https://accounts.example.com/token",
                    "client_id": "client"
                }""",
                HttpStatusCode.OK,
                headersOf("Content-Type", "application/json"),
            )
        })
    )

    private class BrowserOpened(val url: Url) : RuntimeException()

    private var registeredClient: (OAuthProvider) -> RegisteredClient = { it.desktop }

    private val redirect = object : OAuthRedirect {
        override fun client(provider: OAuthProvider) = registeredClient(provider)

        override suspend fun await(
            provider: OAuthProvider,
            openUrl: (String) -> Unit,
            authUrl: (redirectUri: String) -> String,
            expectedState: () -> String?,
        ): RedirectResult = throw BrowserOpened(Url(authUrl("http://127.0.0.1:1")))
    }

    private val signIn = TasksOAuthSignIn(
        caldavDao = mock(),
        encryption = mock(),
        serverEnvironment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore())),
        clientFactory = mock(),
        oauthClient = oauthClient,
        redirect = redirect,
    )

    @Test
    fun aNewSignInShowsTheAccountChooser() = runBlocking {
        val params = authorize(OAuthProvider.GOOGLE).parameters
        assertEquals("select_account", params["prompt"])
        assertNull(params["login_hint"])
    }

    @Test
    fun signingInAgainHintsTheAccount() = runBlocking {
        val params = authorize(OAuthProvider.GOOGLE, loginHint = "user@example.com").parameters
        assertEquals("user@example.com", params["login_hint"])
        assertNull(params["prompt"])
    }

    @Test
    fun signingMicrosoftInAgainAsksForThatAccount() = runBlocking {
        val params = authorize(OAuthProvider.MICROSOFT, loginHint = "user@example.com").parameters
        assertEquals("user@example.com", params["login_hint"])
        assertEquals("login", params["prompt"])
    }

    @Test
    fun androidSignsInToMicrosoftThroughTheSameEndpointAsDesktop() = runBlocking {
        registeredClient = { it.android }

        authorize(OAuthProvider.MICROSOFT)

        assertEquals(
            "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration",
            requested.single(),
        )
    }

    private suspend fun authorize(provider: OAuthProvider, loginHint: String? = null): Url =
        try {
            signIn.authorize(provider, openUrl = {}, loginHint = loginHint)
            throw AssertionError("The browser was never opened")
        } catch (e: BrowserOpened) {
            e.url
        }
}
