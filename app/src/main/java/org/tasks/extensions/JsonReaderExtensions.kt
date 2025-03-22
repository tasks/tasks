package org.tasks.extensions

import android.util.JsonReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.StringWriter

inline fun <reified T>JsonReader.forEach(callback: (@Serializable T) -> Unit) where T : Any {
    beginArray()
    while (hasNext()) callback(Json.decodeFromString(jsonString()))
    endArray()
}

fun JsonReader.jsonString(): String {
    val stringWriter = StringWriter()
    val jsonWriter = android.util.JsonWriter(stringWriter)
    jsonWriter.isLenient = true

    copyJsonToken(this, jsonWriter)
    jsonWriter.close()

    return stringWriter.toString()
}

// Helper function to copy JSON tokens
private fun copyJsonToken(reader: JsonReader, writer: android.util.JsonWriter) {
    when (reader.peek()) {
        android.util.JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            writer.beginArray()
            while (reader.hasNext()) {
                copyJsonToken(reader, writer)
            }
            reader.endArray()
            writer.endArray()
        }
        android.util.JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            writer.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                writer.name(name)
                copyJsonToken(reader, writer)
            }
            reader.endObject()
            writer.endObject()
        }
        android.util.JsonToken.BOOLEAN -> {
            val value = reader.nextBoolean()
            writer.value(value)
        }
        android.util.JsonToken.NULL -> {
            reader.nextNull()
            writer.nullValue()
        }
        android.util.JsonToken.NUMBER -> {
            val value = reader.nextString()
            writer.value(value)
        }
        android.util.JsonToken.STRING -> {
            val value = reader.nextString()
            writer.value(value)
        }
        else -> throw IllegalStateException("Unexpected token: ${reader.peek()}")
    }
}
