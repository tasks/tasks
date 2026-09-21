package org.tasks.caldav.metadata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.tasks.data.dao.RemoteTagEntry
import org.tasks.data.entity.normalizeColor
import org.tasks.data.entity.normalizeIcon

private val JsonElement.primitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

const val TAG_METADATA_CATEGORY = "tag-metadata"

const val TAG_METADATA_VERSION = 1

const val TAG_METADATA_MAX_BYTES = 16_777_216

private const val KEY_VERSION = "version"
private const val KEY_TAGS = "tags"
private const val KEY_ORDER = "order"
private const val KEY_REV = "rev"

private const val ENTRY_NAME = "name"
private const val ENTRY_COLOR = "color"
private const val ENTRY_ICON = "icon"
private const val ENTRY_DELETED = "deleted"
private const val ENTRY_TS = "ts"

private val KNOWN_ENTRY_KEYS = setOf(ENTRY_NAME, ENTRY_COLOR, ENTRY_ICON, ENTRY_DELETED, ENTRY_TS)

sealed interface Entry {
    data class Live(val name: String?, val color: Int?, val icon: String?) : Entry

    data class Tomb(val ts: Long) : Entry
}

fun stampRev(json: JsonObject, rev: String): JsonObject = buildJsonObject {
    json.forEach { (k, v) -> if (k != KEY_REV) put(k, v) }
    put(KEY_REV, rev)
}

class TagMetadataBlob private constructor(private val root: JsonObject) {

    val version: Int
        get() = root[KEY_VERSION]?.primitiveOrNull?.intOrNull ?: TAG_METADATA_VERSION

    val rev: String?
        get() = root[KEY_REV]?.primitiveOrNull?.contentOrNull

    private val rawTags: Map<String, JsonObject> by lazy {
        (root[KEY_TAGS] as? JsonObject)
            ?.mapNotNull { (k, v) -> (v as? JsonObject)?.let { k to it } }
            ?.toMap()
            ?: emptyMap()
    }

    val order: List<String>? by lazy {
        (root[KEY_ORDER] as? JsonArray)?.mapNotNull { it.primitiveOrNull?.contentOrNull }
    }

    val keys: Set<String>
        get() = rawTags.keys

    fun entryOf(key: String): Entry? = rawTags[key]?.let { entryOf(it) }

    private val unknownTopLevel: Map<String, JsonElement>
        get() = root.filterKeys { it != KEY_VERSION && it != KEY_TAGS && it != KEY_ORDER && it != KEY_REV }

    fun overlaid(overlay: Map<String, JsonObject?>, order: List<String>? = this.order): JsonObject = rebuild(
        tags = buildJsonObject {
            rawTags.forEach { (k, v) -> if (k !in overlay) put(k, v) }
            overlay.forEach { (k, v) -> v?.let { put(k, it) } }
        },
        order = order,
    )

    fun liveEntry(key: String, name: String?, color: Int?, icon: String?): JsonObject = buildJsonObject {
        rawTags[key]?.forEach { (k, v) -> if (k !in KNOWN_ENTRY_KEYS) put(k, v) }
        putLiveFields(name, color, icon)
    }

    fun overlayLocal(localLive: List<RemoteTagEntry>, dirtyKeys: Set<String>): JsonObject? {
        var changed = false
        val localByKey = localLive.associateBy { it.key }
        val mergedTags = buildJsonObject {
            rawTags.forEach { (k, v) ->
                if (isTombstone(v) && k in dirtyKeys) {
                    val local = localByKey[k]
                    put(k, buildJsonObject { putLiveFields(local?.name, local?.color, local?.icon) })
                    changed = true
                } else {
                    put(k, v)
                }
            }
            for (t in localLive) {
                val existing = rawTags[t.key]
                when {
                    existing == null -> {
                        put(t.key, buildJsonObject { putLiveFields(t.name, t.color, t.icon) })
                        changed = true
                    }
                    isTombstone(existing) -> {
                    }
                    else -> {
                        val curName = existing[ENTRY_NAME]?.primitiveOrNull?.contentOrNull
                        val curColor = normalizeColor(existing[ENTRY_COLOR]?.primitiveOrNull?.intOrNull)
                        val curIcon = normalizeIcon(existing[ENTRY_ICON]?.primitiveOrNull?.contentOrNull)
                        val dirty = t.key in dirtyKeys
                        val newName = if (dirty) (t.name ?: curName) else (curName ?: t.name)
                        val newColor =
                            if (dirty) (normalizeColor(t.color) ?: curColor) else (curColor ?: normalizeColor(t.color))
                        val newIcon =
                            if (dirty) (normalizeIcon(t.icon) ?: curIcon) else (curIcon ?: normalizeIcon(t.icon))
                        if (newName != curName || newColor != curColor || newIcon != curIcon) {
                            put(t.key, buildJsonObject {
                                existing.forEach { (k, v) -> if (k !in KNOWN_ENTRY_KEYS) put(k, v) }
                                putLiveFields(newName, newColor, newIcon)
                            })
                            changed = true
                        }
                    }
                }
            }
        }
        return if (changed) rebuild(mergedTags, order) else null
    }

    private fun isTombstone(entry: JsonObject): Boolean =
        entry[ENTRY_DELETED]?.primitiveOrNull?.booleanOrNull == true

    private fun rebuild(tags: JsonObject, order: List<String>?): JsonObject = buildJsonObject {
        put(KEY_VERSION, version)
        unknownTopLevel.forEach { (k, v) -> put(k, v) }
        put(KEY_TAGS, tags)
        if (order != null) put(KEY_ORDER, JsonArray(order.map { JsonPrimitive(it) }))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun parse(raw: String?): TagMetadataBlob? {
            if (raw.isNullOrBlank()) return null
            return try {
                if (raw.length.toLong() * 3 > TAG_METADATA_MAX_BYTES &&
                    raw.encodeToByteArray().size > TAG_METADATA_MAX_BYTES
                ) {
                    null
                } else {
                    (json.parseToJsonElement(raw) as? JsonObject)?.let { TagMetadataBlob(it) }
                }
            } catch (_: Throwable) {
                null
            }
        }

        internal fun of(root: JsonObject): TagMetadataBlob = TagMetadataBlob(root)

        fun build(
            tags: List<RemoteTagEntry>,
            order: List<String>,
            tombstones: List<String> = emptyList(),
            ts: Long = 0L,
        ): JsonObject = buildJsonObject {
            put(KEY_VERSION, TAG_METADATA_VERSION)
            put(KEY_TAGS, buildJsonObject {
                tags.forEach { t -> put(t.key, buildJsonObject { putLiveFields(t.name, t.color, t.icon) }) }
                tombstones.forEach { key -> if (tags.none { it.key == key }) put(key, tombstone(ts)) }
            })
            put(KEY_ORDER, JsonArray(order.map { JsonPrimitive(it) }))
        }

        fun tombstone(ts: Long): JsonObject = buildJsonObject {
            put(ENTRY_DELETED, true)
            put(ENTRY_TS, ts)
        }

        private fun entryOf(entry: JsonObject): Entry {
            if (entry[ENTRY_DELETED]?.primitiveOrNull?.booleanOrNull == true) {
                return Entry.Tomb(ts = entry[ENTRY_TS]?.primitiveOrNull?.longOrNull ?: 0L)
            }
            return Entry.Live(
                name = entry[ENTRY_NAME]?.primitiveOrNull?.contentOrNull,
                color = normalizeColor(entry[ENTRY_COLOR]?.primitiveOrNull?.intOrNull),
                icon = normalizeIcon(entry[ENTRY_ICON]?.primitiveOrNull?.contentOrNull),
            )
        }
    }
}

private fun JsonObjectBuilder.putLiveFields(name: String?, color: Int?, icon: String?) {
    name?.let { put(ENTRY_NAME, it) }
    normalizeColor(color)?.let { put(ENTRY_COLOR, it) }
    normalizeIcon(icon)?.let { put(ENTRY_ICON, it) }
}
