package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class OAuthDefaultsTest {
    @Test
    fun everyStageOfARequestGetsThirtySeconds() = runTest {
        var request: HttpRequestData? = null
        val client = TasksOAuthClient(
            HttpClient(MockEngine {
                request = it
                respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }) { oauthDefaults() }
        )

        client.fetchDiscovery("https://example.com")

        val timeouts = request!!.getCapabilityOrNull(HttpTimeoutCapability)!!
        assertEquals(30_000L, timeouts.connectTimeoutMillis)
        assertEquals(30_000L, timeouts.requestTimeoutMillis)
        assertEquals(30_000L, timeouts.socketTimeoutMillis)
    }

    @Test
    fun followsAMovedDiscoveryDocumentEvenWhenThePlatformClientDoesNot() = runTest {
        val client = TasksOAuthClient(
            HttpClient(MockEngine { request ->
                if (request.url.encodedPath == "/old") {
                    respond(
                        "",
                        HttpStatusCode.MovedPermanently,
                        headersOf(HttpHeaders.Location, "https://example.com/new"),
                    )
                } else {
                    respond(
                        """{"token_endpoint":"https://example.com/token"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }) {
                followRedirects = false
                oauthDefaults()
            }
        )

        val discovery = client.fetchDiscovery("https://example.com/old")

        assertEquals("https://example.com/token", discovery["token_endpoint"]?.jsonPrimitive?.content)
    }
}
