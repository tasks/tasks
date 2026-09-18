package org.tasks.auth

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.ByteString.Companion.encodeUtf8

object RedirectState {
    fun encode(nonce: String, redirectUri: String): String =
        JsonObject(mapOf("n" to JsonPrimitive(nonce), "r" to JsonPrimitive(redirectUri)))
            .toString()
            .encodeUtf8()
            .base64Url()
            .trimEnd('=')
}
