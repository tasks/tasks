package org.tasks.extensions

import com.google.gson.JsonObject

object JsonObject {
    fun JsonObject.getStringOrNull(key: String): String? = getOrNull(key)?.asString

    fun JsonObject.getOrNull(key: String) = if (has(key)) get(key) else null
}
