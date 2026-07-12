package org.tasks.caldav

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class CapabilityProbeTest {
    private val principal = "https://example.com/principal/".toHttpUrl()

    @Test
    fun `rejects a server that refuses the write`() = runBlocking {
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doReturn false
        }

        assertFalse(client.supportsDeadProperties(principal))

        verify(client, never()).metadataProbeWithVersion(any())
        Unit
    }

    @Test
    fun `rejects a server that dropped a property in the multi-prop write`() = runBlocking {
        var payload: String? = null
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doAnswer { payload = it.getArgument(1); true }
            onBlocking { metadataProbeWithVersion(any()) } doAnswer { payload to null }
        }

        assertFalse(client.supportsDeadProperties(principal))
    }

    @Test
    fun `rejects a server that did not persist the write`() = runBlocking {
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doReturn true
            onBlocking { metadataProbeWithVersion(any()) } doReturn (null to null)
        }

        assertFalse(client.supportsDeadProperties(principal))
    }

    @Test
    fun `rejects a server that echoed a mismatched rev`() = runBlocking {
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doReturn true
            onBlocking { metadataProbeWithVersion(any()) } doReturn ("""{"rev":"stale"}""" to "stale")
        }

        assertFalse(client.supportsDeadProperties(principal))
    }

    @Test
    fun `accepts a server that persisted both properties consistently, and cleans up after itself`() = runBlocking {
        var payload: String? = null
        var version: String? = null
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doAnswer { inv ->
                payload = inv.getArgument(1)
                version = inv.getArgument(2)
                true
            }
            onBlocking { metadataProbeWithVersion(any()) } doAnswer { payload to version }
        }

        assertTrue(client.supportsDeadProperties(principal))

        verify(client).removeMetadataProbe(principal)
        Unit
    }

    @Test
    fun `the probe carries no user data`() = runBlocking {
        var payload: String? = null
        val client = mock<CaldavClient> {
            onBlocking { pushMetadataProbe(any(), any(), any()) } doAnswer { payload = it.getArgument(1); true }
            onBlocking { metadataProbeWithVersion(any()) } doReturn (null to null)
        }

        client.supportsDeadProperties(principal)

        assertEquals(setOf("rev"), Json.parseToJsonElement(payload!!).jsonObject.keys)
    }
}
