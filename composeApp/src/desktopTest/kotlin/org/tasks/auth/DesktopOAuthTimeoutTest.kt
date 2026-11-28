package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

class DesktopOAuthTimeoutTest {
    private val oauthClient = TasksOAuthClient(
        HttpClient(MockEngine {
            respond(
                """{
                    "authorization_endpoint": "https://login.microsoftonline.com/authorize",
                    "token_endpoint": "https://login.microsoftonline.com/token"
                }""",
                HttpStatusCode.OK,
                headersOf("Content-Type", "application/json"),
            )
        })
    )

    @Test
    fun givesUpWhenTheBrowserNeverComesBack() = runBlocking {
        var authUrl: Url? = null
        val authorizer = OAuthAuthorizer(
            oauthClient = oauthClient,
            serverEnvironment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore())),
            redirect = LoopbackRedirect(timeoutMillis = 100),
        )

        val failure = runCatching {
            authorizer.authorize(OAuthProvider.MICROSOFT, openUrl = { url -> authUrl = Url(url) })
        }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertEquals("Timed out waiting for the sign-in to finish", failure?.message)
        val redirect = Url(authUrl!!.parameters["redirect_uri"]!!)
        ServerSocket().use {
            it.bind(InetSocketAddress(InetAddress.getByName(redirect.host), redirect.port))
        }
    }
}
