package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.decodeBase64
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DesktopAppleSignInTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val oauthClient = TasksOAuthClient(
        HttpClient(MockEngine { request ->
            requests += request
            val body = when (request.url.encodedPath) {
                "/oauth/apple-configuration" -> """{
                    "issuer": "https://appleid.apple.com",
                    "authorization_endpoint": "https://appleid.apple.com/auth/authorize",
                    "token_endpoint": "https://caldav.tasks.org/apple_token",
                    "client_id": "org.tasks.web"
                }"""
                "/apple_token" -> """{"access_token":"tasks-token","id_token":"$identityToken","expires_in":3600}"""
                else -> error("unexpected ${request.url}")
            }
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        })
    )

    @Test
    fun bouncesThroughTheServerCallbackWithoutPkce() = runBlocking {
        var authUrl: Url? = null
        val flow = DesktopOAuthFlow(oauthClient, TasksServerEnvironment(TasksPreferences(InMemoryDataStore()))) { url ->
            authUrl = Url(url)
            launch(Dispatchers.IO) { apple(Url(url)) }
            Unit
        }

        val result = flow.signIn(OAuthProvider.APPLE)

        val params = authUrl!!.parameters
        assertEquals("org.tasks.web", params["client_id"])
        assertEquals("https://caldav.tasks.org/oauth/apple/callback", params["redirect_uri"])
        assertEquals("form_post", params["response_mode"])
        assertEquals("name email", params["scope"])
        assertNull(params["code_challenge"])
        assertNull(params["prompt"])

        val exchange = requests.last()
        val form = (exchange.body as FormDataContent).formData
        assertEquals("https://caldav.tasks.org/apple_token", exchange.url.toString())
        assertEquals("org.tasks.web", form["client_id"])
        assertEquals("c0de", form["code"])
        assertEquals(loopback(params["state"]!!), form["redirect_uri"])
        assertNull(form["code_verifier"])
        assertEquals("tasks-token", result.accessToken)
        assertEquals("001234.abcd", result.idToken?.sub)
    }

    private fun apple(authUrl: Url) {
        val state = authUrl.parameters["state"]!!
        val connection = URL("${loopback(state)}?code=c0de&state=$state").openConnection() as HttpURLConnection
        connection.responseCode
        connection.disconnect()
    }

    private fun loopback(state: String): String =
        Json.parseToJsonElement(state.decodeBase64()!!.utf8()).jsonObject["r"]!!.jsonPrimitive.content

    private val identityToken = "eyJhbGciOiJSUzI1NiJ9." +
        "eyJzdWIiOiIwMDEyMzQuYWJjZCIsImVtYWlsIjoicmVsYXlAcHJpdmF0ZXJlbGF5LmFwcGxlaWQuY29tIn0." +
        "sig"
}
