package org.tasks.sync.todoist

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.utils.EmptyContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.tasks.http.NotFoundException
import org.tasks.http.installTodoist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoistServiceTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"sync_token": "next-token"}""",
    ): TodoistService {
        val engine = MockEngine { request ->
            requests.add(request)
            if (status.isSuccess()) {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respondError(status)
            }
        }
        return TodoistService(
            HttpClient(engine) {
                installTodoist(log = {}, bearerToken = { "test-token" })
            }
        )
    }

    private suspend fun HttpRequestData.bodyText(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        EmptyContent -> ""
        else -> error("unexpected body type ${content::class.simpleName}")
    }

    private fun String.formParams(): Map<String, String> =
        split("&")
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val (k, v) = pair.split("=", limit = 2)
                k.decodeForm() to v.decodeForm()
            }

    private fun String.decodeForm(): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < length) {
            when (val c = this[i]) {
                '+' -> { bytes.add(' '.code.toByte()); i++ }
                '%' -> {
                    bytes.add(substring(i + 1, i + 3).toInt(16).toByte())
                    i += 3
                }
                else -> { bytes.addAll(c.toString().toByteArray().toList()); i++ }
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    @Test
    fun sendsBearerToken() = runTest {
        service().sync()
        assertEquals("Bearer test-token", requests.single().headers["Authorization"])
    }

    @Test
    fun fullSyncUsesStarToken() = runTest {
        service().sync()
        val params = requests.single().bodyText().formParams()
        assertEquals("*", params["sync_token"])
        assertEquals("""["projects","items","sections","labels"]""", params["resource_types"])
    }

    @Test
    fun deltaSyncReplaysToken() = runTest {
        service().sync(syncToken = "prev-token")
        assertEquals("prev-token", requests.single().bodyText().formParams()["sync_token"])
    }

    @Test
    fun omitsCommandsWhenThereAreNone() = runTest {
        service().sync()
        assertTrue("commands" !in requests.single().bodyText().formParams())
    }

    @Test
    fun encodesCommandsAsJsonArray() = runTest {
        service().sync(
            syncToken = "t",
            commands = listOf(
                TodoistCommand(
                    type = TodoistCommand.ITEM_ADD,
                    uuid = "cmd-uuid",
                    tempId = "temp-1",
                    args = buildJsonObject {
                        put("content", "Buy milk")
                        put("project_id", "p1")
                    },
                )
            ),
        )
        val raw = assertNotNull(requests.single().bodyText().formParams()["commands"])
        val command = Json.parseToJsonElement(raw).jsonArray.single().jsonObject
        assertEquals("item_add", command["type"]?.jsonPrimitive?.content)
        assertEquals("cmd-uuid", command["uuid"]?.jsonPrimitive?.content)
        assertEquals("temp-1", command["temp_id"]?.jsonPrimitive?.content)
        assertEquals("Buy milk", command["args"]?.jsonObject?.get("content")?.jsonPrimitive?.content)
    }

    @Test
    fun omitsTempIdWhenAbsent() = runTest {
        service().sync(
            commands = listOf(
                TodoistCommand(
                    type = TodoistCommand.ITEM_DELETE,
                    uuid = "u",
                    args = buildJsonObject { put("id", "123") },
                )
            ),
        )
        val raw = requests.single().bodyText().formParams()["commands"]!!
        val command = Json.parseToJsonElement(raw).jsonArray.single().jsonObject
        // An explicit "temp_id": null is a malformed command, so the key must be gone
        // entirely rather than present-and-null.
        assertTrue("temp_id" !in command, "temp_id must be omitted, got: $raw")
    }

    @Test
    fun keepsExplicitNullsInsideCommandArgs() = runTest {
        // Omitting temp_id must not also drop a JsonNull inside args: that is how a
        // cleared due date is expressed, and dropping it would silently leave the
        // remote date in place.
        service().sync(
            commands = listOf(
                TodoistCommand(
                    type = TodoistCommand.ITEM_UPDATE,
                    uuid = "u",
                    args = buildJsonObject {
                        put("id", "123")
                        put("due", kotlinx.serialization.json.JsonNull)
                    },
                )
            ),
        )
        val raw = requests.single().bodyText().formParams()["commands"]!!
        val args = Json.parseToJsonElement(raw).jsonArray.single().jsonObject["args"]!!.jsonObject
        assertTrue("due" in args, "due must survive as explicit null, got: $raw")
        assertEquals(kotlinx.serialization.json.JsonNull, args["due"])
    }

    @Test
    fun generatedCommandIdsAreUuids() {
        val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        repeat(20) {
            val id = todoistCommandId()
            assertTrue(uuid.matches(id), "'$id' is not a UUID")
        }
    }

    @Test
    fun parsesSyncResponse() = runTest {
        val response = service(
            body = """{"sync_token": "next-token", "full_sync": false, "items": []}"""
        ).sync(syncToken = "t")
        assertEquals("next-token", response.syncToken)
    }

    @Test
    fun postsToSyncEndpoint() = runTest {
        service().sync()
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/sync", request.url.encodedPath)
        assertEquals("api.todoist.com", request.url.host)
    }

    @Test
    fun createsProjectOverRest() = runTest {
        val project = service(
            body = """{"id": "p9", "name": "New", "color": "blue"}"""
        ).createProject(name = "New", color = "blue")
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/projects", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.bodyText()).jsonObject
        assertEquals("New", body["name"]?.jsonPrimitive?.content)
        assertEquals("blue", body["color"]?.jsonPrimitive?.content)
        assertEquals("p9", project.id)
    }

    @Test
    fun deletesProjectOverRest() = runTest {
        service(body = "").deleteProject("p9")
        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/projects/p9", request.url.encodedPath)
    }

    @Test
    fun paginatesCompletedTasksByCompletionDate() = runTest {
        service(body = """{"results": [], "next_cursor": null}""")
            .getCompletedByCompletionDate(
                since = "2026-09-01T00:00:00",
                until = "2026-10-01T00:00:00",
                cursor = "abc",
            )
        val url = requests.single().url
        assertEquals("/api/v1/tasks/completed/by_completion_date", url.encodedPath)
        assertEquals("2026-09-01T00:00:00", url.parameters["since"])
        assertEquals("2026-10-01T00:00:00", url.parameters["until"])
        assertEquals("abc", url.parameters["cursor"])
        assertEquals("200", url.parameters["limit"])
    }

    @Test
    fun translates404ToNotFound() = runTest {
        assertFailsWith<NotFoundException> {
            service(status = HttpStatusCode.NotFound).getProject("missing")
        }
    }
}
