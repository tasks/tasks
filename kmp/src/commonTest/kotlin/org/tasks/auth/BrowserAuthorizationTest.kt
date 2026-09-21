package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BrowserAuthorizationTest {
    private val config = OAuthConfig(
        authorizationEndpoint = "https://appleid.apple.com/auth/authorize",
        tokenEndpoint = "https://caldav.tasks.org/apple_token",
        clientId = "org.tasks.web",
        redirectUri = "http://127.0.0.1:49152",
        scope = "name email",
        state = "st",
    )

    @Test
    fun leavesOutPkceWhenThereIsNoChallenge() {
        val url = TasksOAuthClient().buildAuthUrl(
            config.copy(redirectUri = "https://caldav.tasks.org/oauth/apple/callback"),
            codeChallenge = null,
            state = "st",
            extraParams = mapOf("response_mode" to "form_post"),
        )

        assertEquals(
            "https://appleid.apple.com/auth/authorize?client_id=org.tasks.web" +
                "&redirect_uri=https%3A%2F%2Fcaldav.tasks.org%2Foauth%2Fapple%2Fcallback" +
                "&response_type=code&scope=name+email&state=st&response_mode=form_post",
            url,
        )
        assertFalse(url.contains("code_challenge"))
    }

    @Test
    fun exchangesTheCodeWithoutAVerifier() = runTest {
        var request: HttpRequestData? = null
        val client = TasksOAuthClient(
            HttpClient(MockEngine { req ->
                request = req
                respond(
                    """{"access_token":"tasks-token","expires_in":3600}""",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
            })
        )

        val result = client.exchangeCode(config, "c0de", codeVerifier = null)

        val form = (request!!.body as FormDataContent).formData
        assertEquals("https://caldav.tasks.org/apple_token", request!!.url.toString())
        assertEquals("authorization_code", form["grant_type"])
        assertEquals("org.tasks.web", form["client_id"])
        assertEquals("http://127.0.0.1:49152", form["redirect_uri"])
        assertEquals("c0de", form["code"])
        assertNull(form["code_verifier"])
        assertEquals("tasks-token", result.accessToken)
    }
}
