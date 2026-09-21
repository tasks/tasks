package org.tasks.mcp

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class SchemaBuilder {
    private val properties = mutableMapOf<String, JsonObject>()
    private val required = mutableListOf<String>()

    fun string(
        name: String,
        description: String,
        required: Boolean = false,
        enum: List<String>? = null,
    ) = apply {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
            enum?.let { put("enum", JsonArray(it.map(::JsonPrimitive))) }
        }
        if (required) this.required += name
    }

    fun integer(
        name: String,
        description: String,
        required: Boolean = false,
        minimum: Int? = null,
        maximum: Int? = null,
        default: Int? = null,
    ) = apply {
        properties[name] = buildJsonObject {
            put("type", "integer")
            put("description", description)
            minimum?.let { put("minimum", it) }
            maximum?.let { put("maximum", it) }
            default?.let { put("default", it) }
        }
        if (required) this.required += name
    }

    fun number(name: String, description: String, required: Boolean = false) = apply {
        properties[name] = buildJsonObject {
            put("type", "number")
            put("description", description)
        }
        if (required) this.required += name
    }

    fun boolean(name: String, description: String, default: Boolean? = null) = apply {
        properties[name] = buildJsonObject {
            put("type", "boolean")
            put("description", description)
            default?.let { put("default", it) }
        }
    }

    fun date(name: String, description: String) = apply {
        properties[name] = buildJsonObject {
            put("type", JsonArray(listOf(JsonPrimitive("integer"), JsonPrimitive("string"))))
            put(
                "description",
                "$description Epoch milliseconds, or the local form reads return: 2026-09-12 " +
                    "for an all-day date, 2026-09-12T19:00:00 for a date and time.",
            )
        }
    }

    fun timestamp(name: String, description: String, required: Boolean = false) = apply {
        properties[name] = buildJsonObject {
            put("type", JsonArray(listOf(JsonPrimitive("integer"), JsonPrimitive("string"))))
            put(
                "description",
                "$description Epoch milliseconds, or a local date and time like " +
                    "2026-09-12T19:00:00; a date alone, 2026-09-12, is its midnight.",
            )
        }
        if (required) this.required += name
    }

    fun stringArray(name: String, description: String, enum: List<String>? = null) = apply {
        properties[name] = buildJsonObject {
            put("type", "array")
            put("description", description)
            putJsonObject("items") {
                put("type", "string")
                enum?.let { put("enum", JsonArray(it.map(::JsonPrimitive))) }
            }
        }
    }

    fun integerArray(name: String, description: String) = apply {
        properties[name] = buildJsonObject {
            put("type", "array")
            put("description", description)
            putJsonObject("items") { put("type", "integer") }
        }
    }

    fun objectArray(name: String, description: String, item: SchemaBuilder.() -> Unit) = apply {
        val nested = SchemaBuilder().apply(item)
        properties[name] = buildJsonObject {
            put("type", "array")
            put("description", description)
            put("items", nested.buildObjectSchema())
        }
    }

    fun buildObjectSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(properties))
        if (required.isNotEmpty()) {
            put("required", JsonArray(required.map(::JsonPrimitive)))
        }
    }

    fun build(): ToolSchema = ToolSchema(
        properties = JsonObject(properties),
        required = required.takeIf { it.isNotEmpty() },
    )
}

fun schema(block: SchemaBuilder.() -> Unit): ToolSchema = SchemaBuilder().apply(block).build()
