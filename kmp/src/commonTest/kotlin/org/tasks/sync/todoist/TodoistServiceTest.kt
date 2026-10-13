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

class TodoistServiceTest : TodoistServiceTestCase() {
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
        assertTrue("temp_id" !in command, "temp_id must be omitted, got: $raw")
    }

    @Test
    fun keepsExplicitNullsInsideCommandArgs() = runTest {
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

}
