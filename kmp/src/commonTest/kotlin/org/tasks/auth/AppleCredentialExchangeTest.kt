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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AppleCredentialExchangeTest {
    private var request: HttpRequestData? = null

    private val identityToken = "eyJhbGciOiJSUzI1NiJ9." +
        "eyJzdWIiOiIwMDEyMzQuYWJjZCIsImVtYWlsIjoicmVsYXlAcHJpdmF0ZXJlbGF5LmFwcGxlaWQuY29tIn0." +
        "sig"

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String) =
        TasksOAuthClient(
            HttpClient(MockEngine { req ->
                request = req
                respond(body, status, headersOf("Content-Type", "application/json"))
            })
        )

    @Test
    fun postsTheCredentialAsAForm() = runTest {
        val result = client(
            body = """{"access_token":"tasks-token","id_token":"$identityToken","expires_in":3600}""",
        ).exchangeAppleCredential(
            tokenEndpoint = "https://caldav.tasks.org/apple_token",
            clientId = "org.tasks",
            authorizationCode = "c0de",
            identityToken = identityToken,
            nonce = "n0nce",
            email = "alice@example.com",
            fullName = "Alice Example",
        )

        val form = (request!!.body as FormDataContent).formData
        assertEquals("https://caldav.tasks.org/apple_token", request!!.url.toString())
        assertEquals("authorization_code", form["grant_type"])
        assertEquals("org.tasks", form["client_id"])
        assertEquals("c0de", form["code"])
        assertEquals(identityToken, form["id_token"])
        assertEquals("n0nce", form["nonce"])
        assertEquals("alice@example.com", form["email"])
        assertEquals("Alice Example", form["name"])
        assertNull(form["redirect_uri"])

        assertEquals("tasks-token", result.accessToken)
        assertEquals("001234.abcd", result.idToken?.sub)
        assertEquals("relay@privaterelay.appleid.com", result.idToken?.email)
        assertEquals("org.tasks", result.clientId)
        assertEquals("https://caldav.tasks.org/apple_token", result.tokenEndpoint)
    }

    @Test
    fun leavesOutWhatAppleDidNotSend() = runTest {
        client(body = """{"access_token":"tasks-token"}""").exchangeAppleCredential(
            tokenEndpoint = "https://caldav.tasks.org/apple_token",
            clientId = "org.tasks",
            authorizationCode = "c0de",
            identityToken = identityToken,
            nonce = "n0nce",
        )

        val form = (request!!.body as FormDataContent).formData
        assertNull(form["email"])
        assertNull(form["name"])
    }

    @Test
    fun aRejectedCredentialIsAnExchangeFailure() = runTest {
        val e = assertFailsWith<Exception> {
            client(
                status = HttpStatusCode.Unauthorized,
                body = """{"error":"invalid_grant","error_description":"nonce mismatch"}""",
            ).exchangeAppleCredential(
                tokenEndpoint = "https://caldav.tasks.org/apple_token",
                clientId = "org.tasks",
                authorizationCode = "c0de",
                identityToken = identityToken,
                nonce = "n0nce",
            )
        }
        assertEquals("${TokenError.EXCHANGE_FAILED}: nonce mismatch", e.message)
    }
}
