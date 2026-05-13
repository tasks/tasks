package org.tasks.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

class IdToken(idToken: String) {
    private val json: JsonObject

    init {
        val parts = idToken.split(".")
        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        json = Json.parseToJsonElement(payload) as JsonObject
    }

    val email: String?
        get() = json["email"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

    val sub: String
        get() = json["sub"]!!.jsonPrimitive.content

    val login: String?
        get() = json["login"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
}
