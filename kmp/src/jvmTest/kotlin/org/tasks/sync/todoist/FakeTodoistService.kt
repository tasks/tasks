package org.tasks.sync.todoist

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.utils.EmptyContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.tasks.http.installTodoist
import java.net.URLDecoder

/**
 * A scripted Todoist endpoint.
 *
 * Driven through a real [TodoistService] over a [MockEngine], so tests exercise the
 * actual form encoding and JSON parsing rather than a hand-rolled stand-in. Responses
 * are built per request so that `sync_status` and `temp_id_mapping` can reference the
 * client-generated uuids the synchronizer invents at call time.
 */
class FakeTodoistService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private class Scripted(
        val syncToken: String,
        val fullSync: Boolean,
        val projects: List<TodoistProject>,
        val items: List<TodoistItem>,
        val tempIdMapping: Map<String, String>,
        val okAll: Boolean,
        val errors: Map<String, Pair<Int, String>>,
    )

    private val queue = ArrayDeque<Scripted>()

    /** Sync tokens the client sent, in order. */
    val requestedTokens = mutableListOf<String>()

    /** Commands the client sent, one list per request. */
    val commands = mutableListOf<List<TodoistCommand>>()

    fun enqueue(
        syncToken: String,
        fullSync: Boolean = false,
        projects: List<TodoistProject> = emptyList(),
        items: List<TodoistItem> = emptyList(),
        /**
         * Keys are placeholders: the special key "temp" is rewritten to whichever
         * temp_id the client actually generated for its first `item_add`.
         */
        tempIdMapping: Map<String, String> = emptyMap(),
        okAll: Boolean = true,
        errors: Map<String, Pair<Int, String>> = emptyMap(),
    ) {
        queue.addLast(
            Scripted(syncToken, fullSync, projects, items, tempIdMapping, okAll, errors)
        )
    }

    fun asService(): TodoistService = TodoistService(
        HttpClient(
            MockEngine { request ->
                val body = request.bodyText()
                val params = body.formParams()
                params["sync_token"]?.let { requestedTokens.add(it) }
                val sent = params["commands"]
                    ?.let { json.decodeFromString<List<TodoistCommand>>(it) }
                    ?: emptyList()
                commands.add(sent)

                val scripted = queue.removeFirstOrNull()
                    ?: Scripted("fallback", false, emptyList(), emptyList(), emptyMap(), true, emptyMap())
                respond(
                    content = json.encodeToString(scripted.toResponse(sent)),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }
        ) {
            installTodoist(log = {}, bearerToken = { "test-token" })
        }
    )

    private fun Scripted.toResponse(sent: List<TodoistCommand>): JsonObject {
        val firstTempId = sent.firstNotNullOfOrNull { it.tempId }
        val mapping = tempIdMapping.mapKeys { (key, _) ->
            if (key == TEMP_PLACEHOLDER) firstTempId ?: key else key
        }
        return buildJsonObject {
            put("sync_token", syncToken)
            put("full_sync", fullSync)
            put("projects", json.encodeToJsonElement<List<TodoistProject>>(projects))
            put("items", json.encodeToJsonElement<List<TodoistItem>>(items))
            put("sections", buildJsonArray { })
            put("labels", buildJsonArray { })
            put(
                "temp_id_mapping",
                buildJsonObject { mapping.forEach { (k, v) -> put(k, v) } },
            )
            put(
                "sync_status",
                buildJsonObject {
                    sent.forEach { command ->
                        val error = errors[command.type]
                        when {
                            error != null -> put(
                                command.uuid,
                                buildJsonObject {
                                    put("error_code", error.first)
                                    put("error", error.second)
                                },
                            )
                            okAll -> put(command.uuid, JsonPrimitive("ok"))
                        }
                    }
                },
            )
        }
    }

    private suspend fun io.ktor.client.request.HttpRequestData.bodyText(): String =
        when (val content = body) {
            is TextContent -> content.text
            is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
            EmptyContent -> ""
            else -> ""
        }

    private fun String.formParams(): Map<String, String> =
        split("&")
            .filter { it.isNotEmpty() && "=" in it }
            .associate { pair ->
                val (k, v) = pair.split("=", limit = 2)
                URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
            }

    companion object {
        const val TEMP_PLACEHOLDER = "temp"
    }
}
