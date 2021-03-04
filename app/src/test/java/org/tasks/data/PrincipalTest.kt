package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.Principal.Companion.name

class PrincipalTest {
    @Test
    fun lastSegmentTrailingSlash() {
        val principal = Principal().apply {
            principal = "principals/users/user1/"
        }

        assertEquals("user1", principal.name)
    }

    @Test
    fun lastSegmentNoTrailingSlash() {
        val principal = Principal().apply {
            principal = "principals/users/user1"
        }

        assertEquals("user1", principal.name)
    }

    @Test
    fun stripMailto() {
        val principal = Principal().apply {
            principal = "mailto:user@example.com"
        }

        assertEquals("user@example.com", principal.name)
    }
}