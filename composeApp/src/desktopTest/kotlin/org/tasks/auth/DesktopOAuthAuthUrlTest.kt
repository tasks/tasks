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
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences

class DesktopOAuthAuthUrlTest {
    private class BrowserOpened(val url: Url) : RuntimeException()

    private val oauthClient = TasksOAuthClient(
        HttpClient(MockEngine {
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

    @Test
    fun googleTasksAsksForConsentAndTheAccountChooser() = runBlocking {
        val params = authUrl(OAuthProvider.GOOGLE_TASKS).parameters
        assertEquals("offline", params["access_type"])
        assertEquals("consent select_account", params["prompt"])
        assertNull(params["login_hint"])
    }

    @Test
    fun signingGoogleTasksInAgainSendsTheHint() = runBlocking {
        val params = authUrl(OAuthProvider.GOOGLE_TASKS, loginHint = "user@example.com").parameters
        assertEquals("offline", params["access_type"])
        assertEquals("consent", params["prompt"])
        assertEquals("user@example.com", params["login_hint"])
    }

    private suspend fun authUrl(provider: OAuthProvider, loginHint: String? = null): Url {
        val flow = DesktopOAuthFlow(
            oauthClient = oauthClient,
            serverEnvironment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore())),
            openUrl = { url -> throw BrowserOpened(Url(url)) },
        )
        return try {
            flow.signIn(provider, loginHint = loginHint)
            throw AssertionError("The browser was never opened")
        } catch (e: BrowserOpened) {
            e.url
        }
    }
}
