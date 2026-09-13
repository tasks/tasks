package org.tasks.caldav

import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.ktor.ResponseParser
import at.bitfire.dav4jvm.ktor.resolve
import io.ktor.http.Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

class CaldavUrlTest {
    @Test
    fun `decodes escapes that okhttp preserved`() {
        assertEquals(
            "https://caldav.example.com/dav/calendars/user/foo@example.com/abc/",
            "https://caldav.example.com/dav/calendars/user/foo%40example.com/abc/".canonicalUrl(),
        )
    }

    @Test
    fun `lowercases the host`() {
        assertEquals(
            "https://caldav.example.com/dav/",
            "https://CalDAV.Example.com/dav/".canonicalUrl(),
        )
    }

    @Test
    fun `punycodes an idn host`() {
        assertEquals(
            "https://xn--bcher-kva.example/dav/",
            "https://Bücher.example/dav/".canonicalUrl(),
        )
        assertEquals(
            "https://xn--bcher-kva.example/dav/",
            "https://xn--bcher-kva.example/dav/".canonicalUrl(),
        )
    }

    @Test
    fun `uppercases percent escapes`() {
        assertEquals(
            "https://example.com/a%20b/",
            "https://example.com/a%20b/".canonicalUrl(),
        )
        assertEquals(
            "https://example.com/a%2Fb/",
            "https://example.com/a%2fb/".canonicalUrl(),
        )
    }

    @Test
    fun `drops the default port`() {
        assertEquals("https://example.com/dav/", "https://example.com:443/dav/".canonicalUrl())
        assertEquals("https://example.com:8443/dav/", "https://example.com:8443/dav/".canonicalUrl())
    }

    @Test
    fun `is idempotent`() {
        val once = "https://Example.com/user/foo%40bar/a%2fb/".canonicalUrl()

        assertEquals(once, once.canonicalUrl())
    }

    @Test
    fun `leaves an unparseable string alone`() {
        assertEquals("https://example.com:port/", "https://example.com:port/".canonicalUrl())
        assertNull("https://example.com:port/".canonicalUrlOrNull())
        assertEquals("https://example.com/100%/", "https://example.com/100%/".canonicalUrl())
        assertNull("https://example.com/100%/".canonicalUrlOrNull())
    }

    @Test
    fun `leaves a string without a scheme alone`() {
        assertEquals("", "".canonicalUrl())
        assertEquals("example.com/dav", "example.com/dav".canonicalUrl())
        assertEquals("AbC%40deF", "AbC%40deF".canonicalUrl())
    }

    @Test
    fun `matches what dav4jvm produces for a multistatus href`() {
        val location = Url("https://Example.com/dav/calendars/user/foo%40example.com/")
        val parser = XmlUtils.newPullParser().apply {
            setInput(
                StringReader(
                    """
                    <d:response xmlns:d="DAV:">
                        <d:href>/dav/calendars/user/foo%40example.com/abc/</d:href>
                        <d:propstat><d:prop/><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
                    </d:response>
                    """.trimIndent()
                )
            )
            nextTag()
        }
        val href = ResponseParser(location.canonical()).parseResponse(parser)!!.response.href

        assertEquals(href.toString(), location.resolve("abc/")!!.canonical().toString())
        assertEquals(href.toString(), href.canonical().toString())
    }
}
