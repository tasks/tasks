package org.tasks.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.decodeBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RedirectStateTest {
    @Test
    fun carriesTheNonceAndTheRedirectAsUnpaddedBase64Url() {
        val state = RedirectState.encode("n0nce", "http://127.0.0.1:49152")

        assertFalse(state.contains('='))
        assertFalse(state.contains('+'))
        assertFalse(state.contains('/'))
        val json = Json.parseToJsonElement(state.decodeBase64()!!.utf8()).jsonObject
        assertEquals("n0nce", json["n"]?.jsonPrimitive?.content)
        assertEquals("http://127.0.0.1:49152", json["r"]?.jsonPrimitive?.content)
    }

    @Test
    fun differsPerNonce() {
        val redirect = "org.tasks.apple://oauth2redirect"
        assertFalse(RedirectState.encode("a", redirect) == RedirectState.encode("b", redirect))
    }
}
