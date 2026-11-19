package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences

class DesktopOAuthAuthUrlTest {
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
    fun theBrowserIsAskedToShowTheAccountChooser() = runBlocking {
        assertEquals("select_account", authUrl(OAuthProvider.GOOGLE).parameters["prompt"])
    }

    @Test
    fun googleTasksKeepsAskingForConsent() = runBlocking {
        val params = authUrl(OAuthProvider.GOOGLE_TASKS).parameters
        assertEquals("consent select_account", params["prompt"])
        assertEquals("offline", params["access_type"])
    }

    private suspend fun authUrl(provider: OAuthProvider): Url {
        var authUrl: Url? = null
        val flow = DesktopOAuthFlow(
            oauthClient = oauthClient,
            serverEnvironment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore())),
            callbackTimeoutMillis = 100,
            openUrl = { url -> authUrl = Url(url) },
        )
        runCatching { flow.signIn(provider) }
        return authUrl!!
    }
}
