package org.tasks.caldav

import at.bitfire.dav4jvm.Property
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProppatchBodyTest {
    private val ns = "http://org.tasks/ns/"
    private val propA = Property.Name(ns, "prop-a")
    private val propB = Property.Name(ns, "prop-b")

    @Test
    fun `both set properties go in a single prop`() {
        val body = proppatchBody(
            set = listOf(
                propA to """{"version":1,"tags":{},"order":[]}""",
                propB to "abc-123",
            ),
            remove = emptyList(),
        )

        assertEquals("exactly one <prop> open tag", 1, Regex("<[^/][^>]*:prop>").findAll(body).count())
        assertTrue("prop-a present", body.contains("prop-a>"))
        assertTrue("prop-b present", body.contains("prop-b>"))
    }

    @Test
    fun `special characters in the payload are xml-escaped`() {
        val body = proppatchBody(
            set = listOf(propA to """{"name":"Home & <Garden"}"""),
            remove = emptyList(),
        )

        assertTrue("ampersand escaped", body.contains("&amp;"))
        assertTrue("lt escaped", body.contains("&lt;"))
    }

    private val multistatus = { status: String ->
        """<?xml version="1.0" encoding="utf-8"?>
           <d:multistatus xmlns:d="DAV:"><d:response>
             <d:href>/principals/user/</d:href>
             <d:propstat>
               <d:prop><t:tag-metadata xmlns:t="http://org.tasks/ns/"/></d:prop>
               <d:status>$status</d:status>
             </d:propstat>
           </d:response></d:multistatus>"""
    }

    @Test
    fun `207 with an all-ok propstat has no failure code`() {
        assertNull(propstatFailureCode(multistatus("HTTP/1.1 200 OK")))
    }

    @Test
    fun `207 hiding a failed propstat returns its code`() {
        assertEquals(403, propstatFailureCode(multistatus("HTTP/1.1 403 Forbidden")))
        assertEquals(507, propstatFailureCode(multistatus("HTTP/1.1 507 Insufficient Storage")))
    }

    @Test
    fun `an empty or unparseable body has no failure code`() {
        assertNull(propstatFailureCode(null))
        assertNull(propstatFailureCode(""))
        assertNull(propstatFailureCode("not xml at all"))
    }
}

