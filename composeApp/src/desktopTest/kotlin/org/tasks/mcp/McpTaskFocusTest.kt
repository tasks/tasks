package org.tasks.mcp

import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TaskEditDestination
import org.tasks.api.TaskWrite
import org.tasks.selection.SelectedTaskStore
import java.net.ServerSocket

class McpTaskFocusTest : McpGraphTestCase() {

    private var server: McpHttpServer? = null

    @org.junit.After
    fun stopServer() {
        server?.takeIf { it.isRunning }?.stop()
        server = null
    }

    @Test
    fun getSelectedTaskSaysSoWhenNothingIsOpen() = withClient { client ->
        koin.get<SelectedTaskStore>().update(null)
        val text = client.call("get_selected_task", "id", "title").text()

        assertTrue("expected the nothing-open message, got: $text",
            text.contains("No task is currently open"))
    }

    @Test
    fun getSelectedTaskReturnsTheOpenTaskWithItsParentChain() = withClient { client ->
        val parentId = api.createTask(title = "Parent task")
        val childId = api.createTasks(
            listOf(TaskWrite(title = "Child task", parentId = parentId)),
        ).ids.single()

        koin.get<SelectedTaskStore>().update(childId)
        val text = client.call("get_selected_task", "id", "title", "parent_id").text()
        val body = Json.parseToJsonElement(text).jsonObject

        val task = body["task"]!!.jsonObject
        assertEquals(childId, task["id"]!!.jsonPrimitive.long)
        assertEquals("Child task", task["title"]!!.jsonPrimitive.content)
        assertEquals(parentId, task["parent_id"]!!.jsonPrimitive.long)

        val chain = body["parent_chain"]!!.jsonArray
        assertEquals(1, chain.size)
        assertEquals(parentId, chain.first().jsonObject["id"]!!.jsonPrimitive.long)
        assertEquals("Parent task", chain.first().jsonObject["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun getSelectedTaskSurvivesTheSelectedTaskBeingDeleted() = withClient { client ->
        val id = api.createTask(title = "Doomed task")
        api.deleteTask(id)
        koin.get<SelectedTaskStore>().update(id)

        val text = client.call("get_selected_task", "id").text()

        assertTrue("expected the gone message, got: $text",
            text.contains("no longer exists"))
    }

    @Test
    fun openTaskIsRefusedWhenTheAppIsNotListening() = withClient { client ->
        val id = api.createTask(title = "Lonely task")

        val text = client.call("open_task", mapOf("task_id" to id)).text()

        assertTrue("expected the refusal message, got: $text",
            text.contains("could not open"))
    }

    @Test
    fun openTaskDeliversTheDestinationToTheApp() = withClient { client ->
        val id = api.createTask(title = "Show me this")
        val requests = koin.get<org.tasks.TaskRequests>()
        val received = CompletableDeferred<TaskEditDestination>()

        koin.get<kotlinx.coroutines.CoroutineScope>().launch {
            val request = requests.openRequests.first()
            received.complete(request.destination)
            request.complete(true)
        }
        delay(50)

        val text = client.call("open_task", mapOf("task_id" to id)).text()

        assertTrue("expected the opened message, got: $text", text.contains("Opened"))
        assertEquals(id, received.await().taskId)
    }

    @Test
    fun openTaskWithAnUnknownIdFails() = withClient { client ->
        val result = runCatching {
            client.call("open_task", mapOf("task_id" to 987_654_321L))
        }

        val failed = result.isFailure || result.getOrNull()?.isError == true
        assertTrue("open_task with an unknown id must fail", failed)
    }

    private suspend fun Client.call(tool: String, vararg fields: String) = callTool(
        CallToolRequest(
            CallToolRequestParams(
                name = tool,
                arguments = buildJsonObject {
                    if (fields.isNotEmpty()) {
                        put("fields", JsonArray(fields.map { JsonPrimitive(it) }))
                    }
                },
            ),
        ),
    )

    private suspend fun Client.call(tool: String, arguments: Map<String, Any?>) = callTool(
        CallToolRequest(
            CallToolRequestParams(
                name = tool,
                arguments = buildJsonObject {
                    arguments.forEach { (key, value) ->
                        put(key, value as? JsonElement ?: JsonPrimitive(value as Number))
                    }
                },
            ),
        ),
    )

    private fun io.modelcontextprotocol.kotlin.sdk.types.CallToolResult.text(): String =
        (content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text

    private fun withClient(mode: AccessMode = AccessMode.ReadOnly, block: suspend (Client) -> Unit) =
        withClient(ToolPermissions.of(mode), block)

    private fun withClient(permissions: ToolPermissions, block: suspend (Client) -> Unit) =
        runBlocking {
            val port = ServerSocket(0).use { it.localPort }
            val instance = McpHttpServer(
                port = port,
                token = { TOKEN },
                permissions = permissions,
                api = api,
                log = ActivityLog(),
                onStateChange = { _, _ -> },
                requests = koin.get<org.tasks.TaskRequests>(),
                selection = koin.get<SelectedTaskStore>(),
            )
            server = instance
            instance.start()

            val http = HttpClient()
            val client = Client(
                io.modelcontextprotocol.kotlin.sdk.types.Implementation(name = "focus-test", version = "1.0"),
            )
            try {
                client.connect(
                    StreamableHttpClientTransport(
                        client = http,
                        url = McpHttpServer.endpoint(port),
                    ) { headers.append(HttpHeaders.Authorization, "Bearer $TOKEN") },
                )
                block(client)
            } finally {
                runCatching { client.close() }
                runCatching { http.close() }
                instance.stop()
                server = null
            }
        }

    private companion object {
        const val TOKEN = "focus-test-token"
    }
}
