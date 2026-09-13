package org.tasks.caldav

import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaldavAuthTest {
    private val server = failFastServer()

    private val basicChallenge = "Basic realm=\"realm\""
    private val digestChallenge = "Digest realm=\"realm\", nonce=\"nonce\", qop=\"auth\""

    @After
    fun tearDown() = server.shutdown()

    private fun MockWebServer.enqueueUnauthorized(challenge: String) = enqueue(
        MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", challenge)
    )

    private fun MockWebServer.authorization() = takeRequest().getHeader("Authorization")

    private suspend fun <T> withClient(block: suspend (CaldavClient) -> T): T {
        server.start()
        return testClientProvider()
            .forUrl(server.url("/dav/").toString(), "user", "password")
            .use { block(it) }
    }

    private suspend fun CaldavClient.get(path: String) =
        httpClient.get(server.url(path).toString()).status.value

    @Test
    fun `sends basic preemptively`() = runBlocking {
        server.enqueue(MockResponse())

        withClient { client ->
            assertEquals(200, client.get("/dav/"))
        }

        assertEquals("Basic dXNlcjpwYXNzd29yZA==", server.authorization())
    }

    @Test
    fun `retries a rejected basic once`() = runBlocking {
        server.enqueueUnauthorized(basicChallenge)
        server.enqueueUnauthorized(basicChallenge)

        withClient { client ->
            assertEquals(401, client.get("/dav/"))
        }

        assertEquals(2, server.requestCount)
        assertTrue(server.authorization()!!.startsWith("Basic "))
        assertTrue(server.authorization()!!.startsWith("Basic "))
    }

    @Test
    fun `retries an unsupported challenge once`() = runBlocking {
        server.enqueueUnauthorized("Bearer realm=\"realm\"")
        server.enqueueUnauthorized("Bearer realm=\"realm\"")

        withClient { client ->
            assertEquals(401, client.get("/dav/"))
        }

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `retries a rejected digest once`() = runBlocking {
        server.enqueueUnauthorized(digestChallenge)
        server.enqueue(MockResponse())
        server.enqueueUnauthorized(digestChallenge)
        server.enqueueUnauthorized(digestChallenge)

        withClient { client ->
            client.get("/dav/")
            assertEquals(401, client.get("/dav/other/"))
        }

        assertEquals(4, server.requestCount)
    }

    @Test
    fun `retries a stale digest nonce`() = runBlocking {
        server.enqueueUnauthorized(digestChallenge)
        server.enqueue(MockResponse())
        server.enqueueUnauthorized("Digest realm=\"realm\", nonce=\"nonce2\", qop=\"auth\", stale=true")
        server.enqueue(MockResponse())

        withClient { client ->
            client.get("/dav/")
            assertEquals(200, client.get("/dav/other/"))
        }

        repeat(3) { server.takeRequest() }
        assertTrue(server.authorization()!!.contains("nonce=\"nonce2\""))
    }

    @Test
    fun `switches to digest when challenged and remembers it`() = runBlocking {
        server.enqueueUnauthorized(digestChallenge)
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())

        withClient { client ->
            assertEquals(200, client.get("/dav/"))
            assertEquals(200, client.get("/dav/other/"))
        }

        assertTrue(server.authorization()!!.startsWith("Basic "))
        assertTrue(server.authorization()!!.startsWith("Digest "))
        assertTrue(server.authorization()!!.startsWith("Digest "))
    }

    @Test
    fun `falls back to basic when a later request is challenged with basic`() = runBlocking {
        server.enqueueUnauthorized(digestChallenge)
        server.enqueue(MockResponse())
        server.enqueueUnauthorized(basicChallenge)
        server.enqueue(MockResponse())

        withClient { client ->
            client.get("/dav/")
            assertEquals(200, client.get("/dav/other/"))
        }

        assertTrue(server.authorization()!!.startsWith("Basic "))
        assertTrue(server.authorization()!!.startsWith("Digest "))
        assertTrue(server.authorization()!!.startsWith("Digest "))
        assertTrue(server.authorization()!!.startsWith("Basic "))
    }

    @Test
    fun `sends a single authorization header when switching schemes`() = runBlocking {
        server.enqueueUnauthorized(digestChallenge)
        server.enqueue(MockResponse())

        withClient { client ->
            assertEquals(200, client.get("/dav/"))
        }

        server.takeRequest()
        assertEquals(1, server.takeRequest().headers.values("Authorization").size)
    }
}
