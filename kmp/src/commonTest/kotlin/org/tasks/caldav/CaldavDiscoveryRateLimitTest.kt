package org.tasks.caldav

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.tasks.http.HttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CaldavDiscoveryRateLimitTest {
    private val root = "https://caldav.example.com/dav/"

    private suspend fun withResponses(
        vararg responses: Pair<HttpStatusCode, String>,
        check: suspend (CaldavClient, List<String>) -> Unit,
    ) {
        val paths = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            val (status, body) = responses.getOrElse(paths.lastIndex) { HttpStatusCode.NotFound to "" }
            respond(body, status, headersOf("Content-Type", "application/xml; charset=utf-8"))
        })
        try {
            CaldavClient.registerFactories()
            check(CaldavClient(http, Url(root)), paths)
        } finally {
            http.close()
        }
    }

    @Test
    fun rateLimitedInitialEndpointStopsDiscovery() = runTest {
        withResponses(HttpStatusCode.TooManyRequests to "") { client, paths ->
            val error = assertFailsWith<HttpException> { client.homeSet() }
            assertEquals(429, error.code)
            assertEquals(listOf("/dav/"), paths)
        }
    }

    @Test
    fun rateLimitedWellKnownEndpointStopsDiscovery() = runTest {
        withResponses(HttpStatusCode.NotFound to "", HttpStatusCode.TooManyRequests to "") { client, paths ->
            val error = assertFailsWith<HttpException> { client.homeSet() }
            assertEquals(429, error.code)
            assertEquals(listOf("/dav/", "/.well-known/caldav"), paths)
        }
    }

    @Test
    fun homeSetRateLimitIsNotMaskedByEarlierUnauthorized() = runTest {
        withResponses(
            HttpStatusCode.Unauthorized to "",
            HttpStatusCode.NotFound to "",
            HttpStatusCode.TooManyRequests to "",
        ) { client, paths ->
            val error = assertFailsWith<HttpException> { client.homeSet() }
            assertEquals(429, error.code)
            assertEquals(listOf("/dav/", "/.well-known/caldav", "/dav/"), paths)
        }
    }

    @Test
    fun unauthorizedInitialEndpointStillAllowsWellKnownDiscovery() = runTest {
        withResponses(
            HttpStatusCode.Unauthorized to "",
            HttpStatusCode.MultiStatus to multistatus("<d:current-user-principal><d:href>/principals/user/</d:href></d:current-user-principal>"),
            HttpStatusCode.MultiStatus to multistatus("<c:calendar-home-set><d:href>/calendars/user/</d:href></c:calendar-home-set>"),
        ) { client, paths ->
            assertEquals("https://caldav.example.com/calendars/user/", client.homeSet())
            assertEquals(listOf("/dav/", "/.well-known/caldav", "/principals/user/"), paths)
        }
    }

    private fun multistatus(property: String) = """<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
          <d:response><d:href>/</d:href><d:propstat><d:prop>$property</d:prop>
          <d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>
        </d:multistatus>""".trimIndent()
}
