package org.tasks.compose.accounts

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrubEmailsTest {
    @Test
    fun anAddressIsReplaced() {
        assertEquals("No account for <email>", scrubEmails("No account for first.last+tag@mail.example.co.uk"))
    }

    @Test
    fun anEncodedAddressInAUrlIsReplaced() {
        assertEquals(
            "https://login.example.com/authorize?login_hint=<email>&prompt=login",
            scrubEmails("https://login.example.com/authorize?login_hint=user%40example.com&prompt=login"),
        )
    }

    @Test
    fun everyAddressIsReplaced() {
        assertEquals("<email> is not <email>", scrubEmails("a@b.com is not c@d.org"))
    }

    @Test
    fun textWithoutAnAddressIsUnchanged() {
        assertEquals("HTTP 403 - Forbidden @ step 2", scrubEmails("HTTP 403 - Forbidden @ step 2"))
    }
}
