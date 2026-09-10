package org.tasks.mcp

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull

internal class Projection private constructor(private val fields: Set<String>?) {

    val isEverything: Boolean get() = fields == null

    fun includes(field: String): Boolean = fields == null || field in fields

    fun apply(element: JsonElement): JsonElement = when {
        fields == null -> element
        element is JsonArray -> JsonArray(element.map { apply(it) })
        element is JsonObject -> JsonObject(
            buildMap {
                element.forEach { (name, value) -> if (name in fields) put(name, value) }
                fields.forEach { if (it !in this) put(it, JsonNull) }
            }
        )
        else -> element
    }

    fun encoder(): Json = if (fields == null) LEAN else COMPLETE

    fun describe(): String =
        fields?.let { " (${it.joinToString(", ")})" }.orEmpty()

    companion object {
        val EVERYTHING = Projection(null)

        private val LEAN = Json {
            prettyPrint = true
            encodeDefaults = false
            explicitNulls = false
        }

        private val COMPLETE = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = true
        }

        fun of(
            requested: List<String>,
            available: List<String>,
            always: Set<String> = setOf("id", "child_count"),
        ): Projection {
            if (requested.isEmpty()) return EVERYTHING
            val unknown = requested.filterNot { it in available }
            require(unknown.isEmpty()) {
                "Unknown field(s) in 'fields': ${unknown.joinToString(", ")}." +
                    " Available: ${available.joinToString(", ")}"
            }
            return Projection(requested.toSet() + always.filter { it in available })
        }
    }
}

internal fun SerialDescriptor.fieldNames(): List<String> =
    (0 until elementsCount).map { getElementName(it) }
