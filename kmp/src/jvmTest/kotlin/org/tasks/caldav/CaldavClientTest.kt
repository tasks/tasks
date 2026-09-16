package org.tasks.caldav

import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.caldav.property.CalendarIcon

class CaldavClientTest {
    private val server = failFastServer()
    private lateinit var client: CaldavClient

    @Before
    fun setUp() = runBlocking {
        server.start()
        client = testClientProvider().forUrl(server.url("/dav/").toString(), "user", "password")
    }

    @After
    fun tearDown() {
        client.close()
        server.shutdown()
    }

    private suspend fun updateIconFailures(body: String): Int {
        server.enqueue(multiStatus(body))
        var failures = 0
        client.updateIcon(Url(server.url("/dav/list/").toString()), "icon") { failures++ }
        return failures
    }

    @Test
    fun `proppatch accepted`() = runBlocking {
        assertEquals(0, updateIconFailures(proppatch(propstatStatus = "HTTP/1.1 200 OK")))
    }

    @Test
    fun `proppatch refused in propstat is not a failure`() = runBlocking {
        assertEquals(0, updateIconFailures(proppatch(propstatStatus = "HTTP/1.1 403 Forbidden")))
    }

    @Test
    fun `proppatch refused for the whole response`() = runBlocking {
        assertEquals(1, updateIconFailures(proppatch(responseStatus = "HTTP/1.1 403 Forbidden")))
    }

    @Test
    fun `calendars reports the response headers`() = runBlocking {
        server.enqueue(multiStatus(EMPTY_HOME_SET).setHeader("DAV", "1, 3, oc-resource-sharing"))
        server.enqueue(MockResponse().setHeader("DAV", "1"))
        val seen = mutableListOf<Headers>()

        client.calendars { seen += it }
        client.httpClient.get(server.url("/dav/").toString())

        assertEquals(listOf("1, 3, oc-resource-sharing"), seen.map { it["DAV"] })
    }

    @Test
    fun `calendars reports the headers of every response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Digest realm=\"realm\", nonce=\"nonce\", qop=\"auth\"")
                .setHeader("X-Sabre-Version", "4.7.0")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(301)
                .setHeader("Location", "/dav/redirected/")
                .setHeader("DAV", "1, 3, oc-resource-sharing")
        )
        server.enqueue(multiStatus(EMPTY_HOME_SET).setHeader("DAV", "1, 3"))
        val seen = mutableListOf<Headers>()

        client.calendars { seen += it }

        assertEquals(listOf("4.7.0", null, null), seen.map { it["X-Sabre-Version"] })
        assertEquals(listOf(null, "1, 3, oc-resource-sharing", "1, 3"), seen.map { it["DAV"] })
    }

    @Test
    fun `makeCollection returns the canonical location after a redirect`() = runBlocking {
        val redirected = server.url("/dav/Created/").newBuilder().host(server.hostName.uppercase()).build()
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", redirected.toString()))
        server.enqueue(MockResponse().setResponseCode(201))

        val created = client.makeCollection("Created", 0, null)

        assertEquals(server.url("/dav/Created/").toString(), created)
        assertEquals(created, created.canonicalUrl())
    }

    @Test
    fun `home set falls back to the entered url when the principal href is unparseable`() = runBlocking {
        server.enqueue(multiStatus(principal("http://localhost:99999/principals/user/")))
        server.enqueue(multiStatus(homeSet("/dav/calendars/user/")))

        val homeSet = client.homeSet()

        assertEquals("/dav/", server.takeRequest().path)
        assertEquals("/dav/", server.takeRequest().path)
        assertEquals(server.url("/dav/calendars/user/").toString(), homeSet)
    }

    companion object {
        private val EMPTY_HOME_SET = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/dav/</d:href>
                    <d:propstat>
                        <d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private fun proppatch(responseStatus: String? = null, propstatStatus: String? = null) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:t="${CalendarIcon.NAME.namespace}">
                <d:response>
                    <d:href>/dav/list/</d:href>
                    ${propstatStatus?.let { "<d:propstat><d:prop><t:${CalendarIcon.NAME.name}/></d:prop><d:status>$it</d:status></d:propstat>" } ?: ""}
                    ${responseStatus?.let { "<d:status>$it</d:status>" } ?: ""}
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private fun principal(href: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/dav/</d:href>
                    <d:propstat>
                        <d:prop><d:current-user-principal><d:href>$href</d:href></d:current-user-principal></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private fun homeSet(href: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                <d:response>
                    <d:href>/dav/</d:href>
                    <d:propstat>
                        <d:prop><cal:calendar-home-set><d:href>$href</d:href></cal:calendar-home-set></d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        init {
            CaldavClient.registerFactories()
        }
    }
}
