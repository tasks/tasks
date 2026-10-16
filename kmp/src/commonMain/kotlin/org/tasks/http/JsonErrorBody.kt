package org.tasks.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun String.jsonField(name: String): String? = try {
    Json.parseToJsonElement(this).jsonObject[name]?.jsonPrimitive?.content
} catch (_: Exception) {
    null
}

fun String.errorCode(): String? = jsonField("error")
