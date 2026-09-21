package org.tasks.caldav

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.tasks.http.HttpException
import javax.inject.Inject

@HiltAndroidTest
class HomeSetDiscoveryTest : CaldavTest() {

    @Inject lateinit var clientProvider: CaldavClientProvider

    @Test
    fun probesEnteredUrlBeforeWellKnown() = runBlocking {
        server.enqueue(propfind(PRINCIPAL))
        server.enqueue(propfind(HOME_SET))

        val homeSet = clientProvider
            .forUrl(server.url(USER_PATH).toString(), "username", "password")
            .use { it.homeSet() }

        // The user-entered URL is probed first, so .well-known/caldav is never hit
        assertEquals(USER_PATH, server.takeRequest().path)
        assertEquals(server.url(USER_PATH).toString(), homeSet)
    }

    @Test
    fun wellKnownUnauthorizedIsNotFatalWhenUrlResolves() = runBlocking {
        server.enqueue(propfind(NO_PROPS))       // entered URL: no principal
        enqueueUnauthorized()                    // .well-known: refused
        server.enqueue(propfind(HOME_SET))       // findHomeset on entered URL

        val homeSet = clientProvider
            .forUrl(server.url(USER_PATH).toString(), "username", "password")
            .use { it.homeSet() }

        // A 401 from the .well-known probe doesn't abort the flow
        assertEquals(server.url(USER_PATH).toString(), homeSet)
    }

    @Test
    fun unauthorizedInDiscoverySurfacesAsAuthError() = runBlocking {
        enqueueUnauthorized()                    // entered URL: refused
        enqueueUnauthorized()                    // .well-known: refused
        server.enqueue(propfind(NO_PROPS))       // findHomeset: no home-set property

        try {
            clientProvider
                .forUrl(server.url(USER_PATH).toString(), "username", "password")
                .use { it.homeSet() }
            fail("Expected HTTP 401")
        } catch (e: HttpException) {
            // Home-set lookup failed without a 401 of its own, but a 401 was seen
            // during discovery, so an auth error is surfaced rather than not-found
            assertEquals(401, e.code)
        }
    }

    private fun enqueueUnauthorized() = repeat(2) {
        server.enqueue(MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Basic realm=\"realm\""))
    }

    private fun propfind(body: String) = MockResponse()
        .setResponseCode(207)
        .setHeader("Content-Type", "text/xml; charset=\"utf-8\"")
        .setBody(body)

    companion object {
        private const val USER_PATH = "/remote.php/dav/calendars/user1/"
        private const val PRINCIPAL_PATH = "/remote.php/dav/principals/users/user1/"

        private const val NO_PROPS = """<?xml version="1.0"?><d:multistatus xmlns:d="DAV:"/>"""

        private val PRINCIPAL = """<?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>$USER_PATH</d:href>
                <d:propstat>
                  <d:prop>
                    <d:current-user-principal><d:href>$PRINCIPAL_PATH</d:href></d:current-user-principal>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>"""

        private val HOME_SET = """<?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:response>
                <d:href>$PRINCIPAL_PATH</d:href>
                <d:propstat>
                  <d:prop>
                    <cal:calendar-home-set><d:href>$USER_PATH</d:href></cal:calendar-home-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>"""
    }
}
