package org.tasks.caldav

import at.bitfire.dav4jvm.XmlUtils
import at.bitfire.dav4jvm.ktor.MultiStatusParser
import io.ktor.http.Url
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader

class MultiStatusTest {
    private fun members(vararg hrefs: String, location: String = "http://localhost:8080/dav/cal/") = runBlocking {
        val responses = hrefs.joinToString("") {
            "<d:response><d:href>$it</d:href><d:propstat><d:prop/><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>"
        }
        val parser = XmlUtils.newPullParser().apply {
            setInput(StringReader("""<d:multistatus xmlns:d="DAV:">$responses</d:multistatus>"""))
            nextTag()
        }
        flow { MultiStatusParser(Url(location)).parseResponse(parser, this) }.members().map { it.href.toString() }
    }

    @Test
    fun `keeps descendants and drops the collection itself and its siblings`() {
        assertEquals(
            listOf("http://localhost:8080/dav/cal/a.ics", "http://localhost:8080/dav/cal/b/c.ics"),
            members("/dav/cal/", "/dav/cal", "/dav/cal/a.ics", "/dav/cal/b/c.ics", "/dav/calendar/x.ics", "/dav/"),
        )
    }

    @Test
    fun `matches an absolute href whose host differs in case`() {
        assertEquals(
            listOf("http://localhost:8080/dav/cal/a.ics"),
            members("http://LOCALHOST:8080/dav/cal/a.ics", "http://other:8080/dav/cal/b.ics"),
        )
    }

    @Test
    fun `matches hrefs whose escapes differ from the collection url`() {
        assertEquals(
            listOf("http://localhost:8080/dav/foo@bar/a.ics"),
            members("/dav/foo%40bar/a.ics", location = "http://localhost:8080/dav/foo@bar/"),
        )
    }

    @Test
    fun `ignores the query of the collection url`() {
        assertEquals(
            listOf("http://localhost:8080/dav/cal/a.ics?x=1"),
            members("/dav/cal/a.ics", location = "http://localhost:8080/dav/cal/?x=1"),
        )
    }

    @Test
    fun `matches members of the root collection`() {
        assertEquals(
            listOf("http://localhost:8080/a.ics"),
            members("/", "/a.ics", location = "http://localhost:8080/"),
        )
    }

    @Test
    fun `matches a collection url without a trailing slash`() {
        assertEquals(
            listOf("http://localhost:8080/dav/cal/a.ics"),
            members("/dav/cal/a.ics", "/dav/cal/", "/dav/calendar/b.ics", location = "http://localhost:8080/dav/cal"),
        )
    }
}
