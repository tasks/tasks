package org.tasks.caldav

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import org.tasks.data.UUIDHelper

private const val KEY_REV = "rev"

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

suspend fun CaldavClient.supportsDeadProperties(principal: HttpUrl): Boolean {
    val rev = UUIDHelper.newUUID()
    val payload = buildJsonObject { put(KEY_REV, rev) }.toString()
    if (!pushMetadataProbe(principal, payload, rev)) {
        return false
    }
    val (readBack, version) = metadataProbeWithVersion(principal)
    runCatching { removeMetadataProbe(principal) }
    return version == rev && revOf(readBack) == rev
}

private fun revOf(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        (json.parseToJsonElement(raw) as? JsonObject)?.get(KEY_REV)?.jsonPrimitive?.contentOrNull
    } catch (_: Throwable) {
        null
    }
}
