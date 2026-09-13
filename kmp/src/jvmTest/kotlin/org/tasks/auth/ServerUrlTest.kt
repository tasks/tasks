package org.tasks.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.url_host_name_required
import tasks.kmp.generated.resources.url_invalid_scheme

class ServerUrlTest {
    @Test
    fun acceptsHttpAndHttps() {
        assertNull(serverUrlError("https://example.com"))
        assertNull(serverUrlError("http://example.com/remote.php/dav"))
        assertNull(serverUrlError("https://user@example.com:8443/"))
        assertNull(serverUrlError("HTTPS://EXAMPLE.COM"))
        assertNull(serverUrlError("https://例え.jp/dav"))
        assertNull(serverUrlError("http://[::1]:8080/dav"))
    }

    @Test
    fun rejectsMissingOrUnsupportedScheme() {
        assertEquals(Res.string.url_invalid_scheme, serverUrlError("example.com"))
        assertEquals(Res.string.url_invalid_scheme, serverUrlError("ftp://example.com"))
        assertEquals(Res.string.url_invalid_scheme, serverUrlError("not a url"))
        assertEquals(Res.string.url_invalid_scheme, serverUrlError("https:/example.com"))
        assertEquals(Res.string.url_invalid_scheme, serverUrlError("https://exa mple.com"))
    }

    @Test
    fun rejectsMissingHost() {
        assertEquals(Res.string.url_host_name_required, serverUrlError("https://"))
        assertEquals(Res.string.url_host_name_required, serverUrlError("https:///dav"))
        assertEquals(Res.string.url_host_name_required, serverUrlError("https://user@:8443/dav"))
    }
}
