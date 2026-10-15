package org.tasks.sync.todoist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Response envelope from `POST /api/v1/sync`.
 *
 * [syncToken] must be persisted and replayed on the next call to get a delta. A delta
 * only reports resources that changed; absence from [items] therefore means
 * "unchanged", never "deleted". Deletions arrive as entries with `is_deleted = true`,
 * and completions as `checked = true`.
 */
@Serializable
data class TodoistSyncResponse(
    @SerialName("sync_token") val syncToken: String,
    @SerialName("full_sync") val fullSync: Boolean = false,
    val projects: List<TodoistProject> = emptyList(),
    val items: List<TodoistItem> = emptyList(),
    val sections: List<TodoistSection> = emptyList(),
    val labels: List<TodoistLabel> = emptyList(),
    @SerialName("temp_id_mapping") val tempIdMapping: Map<String, String> = emptyMap(),
    /**
     * Keyed by command uuid. The value is the bare string `"ok"` on success, or an
     * error object -- hence [JsonElement] rather than a typed result.
     */
    @SerialName("sync_status") val syncStatus: Map<String, JsonElement> = emptyMap(),
) {
    fun statusOf(uuid: String): CommandResult {
        val status = syncStatus[uuid] ?: return CommandResult.Missing
        (status as? JsonPrimitive)?.let {
            return if (it.content == "ok") CommandResult.Ok else CommandResult.Error(null, it.content)
        }
        val obj = status as? JsonObject ?: return CommandResult.Error(null, status.toString())
        val code = obj["error_code"]?.jsonPrimitive?.content?.toIntOrNull()
        val message = obj["error"]?.jsonPrimitive?.content ?: status.toString()
        return CommandResult.Error(code, message)
    }

    sealed interface CommandResult {
        data object Ok : CommandResult
        /** The server returned no entry for this uuid, which should not happen. */
        data object Missing : CommandResult
        data class Error(val code: Int?, val message: String) : CommandResult {
            /**
             * Todoist error codes for "the thing you referenced is gone". Treated the
             * same as an HTTP 404 on the REST endpoints.
             */
            val isNotFound: Boolean
                get() = code in NOT_FOUND_CODES
        }

        companion object {
            private val NOT_FOUND_CODES = setOf(42, 43, 44, 405, 410)
        }
    }
}

/**
 * A single write in a `commands` batch.
 *
 * [uuid] is the client-generated idempotency key: replaying a batch with the same
 * uuids will not duplicate work. [tempId] names a resource created by this command so
 * that later commands in the *same* batch can reference it; the server returns the
 * real id in [TodoistSyncResponse.tempIdMapping].
 */
@Serializable
data class TodoistCommand(
    val type: String,
    val uuid: String,
    @SerialName("temp_id") val tempId: String? = null,
    val args: JsonObject,
) {
    companion object {
        const val ITEM_ADD = "item_add"
        const val ITEM_UPDATE = "item_update"
        const val ITEM_MOVE = "item_move"
        const val ITEM_DELETE = "item_delete"
        const val ITEM_COMPLETE = "item_complete"
        const val ITEM_UNCOMPLETE = "item_uncomplete"
        const val PROJECT_ADD = "project_add"
        const val PROJECT_UPDATE = "project_update"
        const val PROJECT_DELETE = "project_delete"
    }
}

/**
 * A fresh identifier for a sync command or temp id.
 *
 * Todoist requires these to be RFC 4122 UUIDs and silently rejects the command
 * otherwise. Deliberately *not* [org.tasks.data.UUIDHelper.newUUID], which returns an
 * Astrid-legacy decimal id -- using it here makes every write fail while reads keep
 * working, which is painful to diagnose.
 */
@OptIn(ExperimentalUuidApi::class)
fun todoistCommandId(): String = Uuid.random().toString()

/** Resource types requested from the sync endpoint. */
object TodoistResourceTypes {
    const val PROJECTS = "projects"
    const val ITEMS = "items"
    const val SECTIONS = "sections"
    const val LABELS = "labels"

    val DEFAULT = listOf(PROJECTS, ITEMS, SECTIONS, LABELS)
}
