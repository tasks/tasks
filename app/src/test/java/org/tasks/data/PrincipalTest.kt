package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Principal

class PrincipalTest {
    @Test
    fun lastSegmentTrailingSlash() {
        val principal = Principal(account = 0, href = "/principals/users/user1")

        assertEquals("user1", principal.name)
    }

    @Test
    fun lastSegmentNoTrailingSlash() {
        val principal = Principal(account = 0, href = "principals/users/user1")

        assertEquals("user1", principal.name)
    }

    @Test
    fun stripMailto() {
        val principal = Principal(account = 0, href = "mailto:user@example.com")

        assertEquals("user@example.com", principal.name)
    }
}