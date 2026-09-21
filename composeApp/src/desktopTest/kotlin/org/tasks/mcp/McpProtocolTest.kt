package org.tasks.mcp

import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.tasks.api.ReminderWrite
import org.tasks.api.TagWrite
import org.tasks.api.TaskWrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.AgentNotes
import java.net.ServerSocket

class McpProtocolTest : McpGraphTestCase() {

    private var server: McpHttpServer? = null

    @org.junit.After
    fun stopServer() {
        server?.takeIf { it.isRunning }?.stop()
        server = null
    }

    @Test
    fun readWriteExposesEveryToolIncludingTheDestructiveOnes() = withClient(AccessMode.ReadWrite) { client ->
        val names = client.listTools().tools.map { it.name }.toSet()

        assertEquals(READ_TOOLS + WRITE_TOOLS, names)
        assertTrue(names.containsAll(WRITE_TOOLS))
    }

    @Test
    fun readOnlyRegistersNoWriteToolAtAll() = withClient(AccessMode.ReadOnly) { client ->
        val names = client.listTools().tools.map { it.name }.toSet()

        assertEquals(READ_TOOLS, names)
        WRITE_TOOLS.forEach {
            assertFalse("read-only must not expose $it", it in names)
        }
    }

    @Test
    fun readOnlyToolsAreAnnotatedAsSuch() = withClient(AccessMode.ReadOnly) { client ->
        client.listTools().tools.forEach {
            assertEquals("${it.name} should be annotated read-only", true, it.annotations?.readOnlyHint)
        }
    }

    @Test
    fun callingAWriteToolInReadOnlyModeFails() = withClient(AccessMode.ReadOnly) { client ->
        val result = runCatching {
            client.callTool("delete_task_list", mapOf("list_id" to 1))
        }

        val refused = result.isFailure || result.getOrNull()?.isError == true
        assertTrue("delete_task_list must not run in read-only mode", refused)
    }

    @Test
    fun aReadToolReturnsTheJsonAModelWouldSee() = withClient(AccessMode.ReadWrite) { client ->
        val id = api.createTask(title = "Visible over the wire")

        val text = (client.callTool("list_tasks", mapOf("task_ids" to listOf(id)))
            ?.content
            ?.filterIsInstance<TextContent>()
            ?.firstOrNull()
            ?.text)

        assertNotNull(text)
        assertTrue(text!!, text.contains("Visible over the wire"))
    }

    @Test
    fun fieldsTrimsEveryRowToWhatWasAskedFor() = withClient(AccessMode.ReadWrite) { client ->
        api.createTask(title = "Trimmed", notes = "this should not travel")

        val rows = client.rows("list_tasks", "tasks", "title")

        assertEquals(setOf("id", "child_count", "title"), rows.single().keys)
    }

    @Test
    fun omittingFieldsStillReturnsTheWholeRow() = withClient(AccessMode.ReadWrite) { client ->
        api.createTask(title = "Whole", notes = "every field")

        val rows = client.rows("list_tasks", "tasks")

        assertTrue(rows.single().keys.containsAll(setOf("id", "title", "notes", "priority")))
    }

    @Test
    fun aTaskRowNeverCarriesItsReminders() = withClient(AccessMode.ReadWrite) { client ->
        val id = api.createTask(title = "Reminded", dueDate = DUE)
        api.setTaskReminders(
            id,
            add = listOf(ReminderWrite(type = "relative_due", offsetMs = -3_600_000L)),
            removeReminderIds = emptyList(),
        )

        val row = client.rows("list_tasks", "tasks").single()

        assertFalse(row.keys.toString(), "reminders" in row.keys)
        assertEquals(1, client.rows("list_reminders", "reminders").size)
    }

    @Test
    fun aTaskNamesItsRelatedRowsByIdOnly() = withClient(AccessMode.ReadWrite) { client ->
        api.createTask(title = "On a list")

        val row = client.rows("list_tasks", "tasks").single()

        assertTrue(row.keys.toString(), "list_id" in row.keys)
        assertTrue(row.keys.toString(), "list_name" !in row.keys)
        assertTrue(row.keys.toString(), "place_name" !in row.keys)
    }

    @Test
    fun everyReadToolTakesFields() = withClient(AccessMode.ReadOnly) { client ->
        client.listTools().tools.forEach {
            assertTrue(
                "${it.name} should take a fields argument",
                it.inputSchema.properties?.containsKey("fields") == true,
            )
        }
    }

    @Test
    fun anUnknownFieldIsRefusedRatherThanIgnored() = withClient(AccessMode.ReadOnly) { client ->
        val result = client.call("list_tasks", "titel")

        assertEquals(true, result.isError)
        val text = result.content.filterIsInstance<TextContent>().firstOrNull()?.text.orEmpty()
        assertTrue(text, text.contains("titel"))
    }

    @Test
    fun anUnknownArgumentIsRefusedRatherThanIgnored() = withClient(AccessMode.ReadWrite) { client ->
        val singular = client.callRaw("list_tasks", buildJsonObject { put("list_id", 1) })
        val nested = client.callRaw(
            "create_tasks",
            buildJsonObject {
                put("tasks", JsonArray(listOf(buildJsonObject { put("title", "x"); put("due", "2026-09-12") })))
            },
        )

        assertEquals(true, singular.isError)
        assertTrue(singular.text(), "'list_id'" in singular.text() && "list_ids" in singular.text())
        assertEquals(true, nested.isError)
        assertTrue(nested.text(), "'tasks[0].due'" in nested.text() && "due_date" in nested.text())
        assertEquals(0, api.countTasks())
    }

    @Test
    fun anArrayOfTheWrongShapeIsRefusedRatherThanEmptied() = withClient(AccessMode.ReadOnly) { client ->
        api.createTask("Only")

        val text = client.callRaw("list_tasks", buildJsonObject { put("task_ids", "1,2") })
        val fraction = client.callRaw("list_tasks", buildJsonObject { put("task_ids", JsonArray(listOf(JsonPrimitive(1.5)))) })

        assertEquals(true, text.isError)
        assertTrue(text.text(), "array of integers" in text.text())
        assertEquals(true, fraction.isError)
    }

    private fun CallToolResult.text(): String =
        content.filterIsInstance<TextContent>().firstOrNull()?.text.orEmpty()

    @Test
    fun theServerExplainsFieldsToTheModel() = withClient(AccessMode.ReadOnly) { client ->
        assertTrue(client.serverInstructions!!.contains("`fields`"))
    }

    @Test
    fun theServerIntroducesItselfAndExplainsItsAccessLevel() = withClient(AccessMode.ReadOnly) { client ->
        assertEquals("tasks-org", client.serverVersion?.name)
        assertTrue(client.serverInstructions!!.contains("READ ONLY"))
    }

    @Test
    fun theCatalogueListsExactlyTheToolsThatExist() = withClient(AccessMode.ReadWrite) { client ->
        val names = client.listTools().tools.map { it.name }.toSet()

        assertEquals(McpToolCatalog.names, names)
        assertEquals(READ_TOOLS + WRITE_TOOLS, McpToolCatalog.names)
        assertEquals(READ_TOOLS, McpToolCatalog.readTools)
    }

    @Test
    fun advancedRegistersOnlyWhatTheUserTicked() {
        val ticked = setOf("list_tasks", "list_tags", "create_tasks")

        withClient(ToolPermissions.of(AccessMode.Advanced, ticked)) { client ->
            assertEquals(ticked, client.listTools().tools.map { it.name }.toSet())
        }
    }

    @Test
    fun aSwitchedOffToolIsAbsentRatherThanRefusing() {
        val permissions = ToolPermissions.of(AccessMode.Advanced, setOf("list_tasks"))

        withClient(permissions) { client ->
            assertFalse("list_places" in client.listTools().tools.map { it.name })

            val result = client.callTool("list_places", emptyMap())

            assertEquals(true, result.isError)
            val text = result.content.filterIsInstance<TextContent>().single().text
            assertTrue(text, text.contains("not found"))
            assertFalse(text, text.contains("permission"))
        }
    }

    @Test
    fun advancedWithNoWriteToolReadsAsReadOnly() {
        val permissions = ToolPermissions.of(AccessMode.Advanced, McpToolCatalog.readTools)

        withClient(permissions) { client ->
            assertTrue(client.serverInstructions!!.contains("READ ONLY"))
        }
    }

    @Test
    fun theModelIsToldWhenToolsHaveBeenSwitchedOff() {
        val permissions = ToolPermissions.of(AccessMode.Advanced, setOf("list_tasks"))

        withClient(permissions) { client ->
            assertTrue(client.serverInstructions!!.contains("switched individual tools off"))
        }
    }

    @Test
    fun adviceAboutAToolIsOnlyGivenWhenTheToolIsThere() {
        withClient(ToolPermissions.of(AccessMode.Advanced, setOf("list_tasks"))) { client ->
            assertFalse(client.serverInstructions!!.contains(AgentNotes.DELETE_CASCADE))
        }
        withClient(AccessMode.ReadWrite) { client ->
            assertTrue(client.serverInstructions!!.contains(AgentNotes.DELETE_CASCADE))
        }
    }

    @Test
    fun anInvalidRegexIsRefusedWithSomethingFixable() = withClient(AccessMode.ReadWrite) { client ->
        val result = client.callRaw("list_tasks", buildJsonObject { put("matches", "[unclosed") })

        assertEquals(true, result.isError)
        val text = result.content.filterIsInstance<TextContent>().firstOrNull()?.text.orEmpty()
        assertTrue(text, text.contains("matches"))
    }

    @Test
    fun anOversizedPageIsCutToFitAndSaysSo() = withClient(AccessMode.ReadWrite) { client ->
        val padding = "x".repeat(2_000)
        repeat(60) { api.createTask(title = "Task $it $padding") }

        val body = client.body("list_tasks", buildJsonObject { put("limit", 1000) })
        val rows = body.getValue("tasks").jsonArray

        assertEquals(60, body.getValue("total").jsonPrimitive.int)
        assertTrue("expected fewer rows than matched", rows.size < 60)
        assertTrue(body.getValue("has_more").jsonPrimitive.boolean)
        assertEquals(rows.size, body.getValue("next_offset").jsonPrimitive.int)
        assertTrue(
            "response was ${body.toString().length} chars",
            body.toString().toByteArray().size < 64 * 1024,
        )
    }

    @Test
    fun aProjectedFieldIsPresentEvenWhenItIsEmpty() = withClient(AccessMode.ReadWrite) { client ->
        api.createTask(title = "No notes on this one")

        val row = client.rows("list_tasks", "tasks", "title", "notes", "is_read_only").single()

        assertEquals(setOf("id", "child_count", "title", "notes", "is_read_only"), row.keys)
        assertEquals(JsonNull, row.getValue("notes"))
        assertFalse(row.getValue("is_read_only").jsonPrimitive.boolean)
    }

    @Test
    fun everyListToolPagesAndCounts() = withClient(AccessMode.ReadWrite) { client ->
        defaultListId()
        repeat(7) { api.createTag(TagWrite(name = "tag $it")) }

        val counted = client.body("list_tags", buildJsonObject { put("limit", 0) })
        assertEquals(7, counted.getValue("total").jsonPrimitive.int)
        assertFalse("tags" in counted.keys)

        val first = client.body("list_tags", buildJsonObject { put("limit", 3) })
        assertEquals(3, first.getValue("tags").jsonArray.size)
        assertEquals(7, first.getValue("total").jsonPrimitive.int)
        assertTrue(first.getValue("has_more").jsonPrimitive.boolean)
        assertEquals(3, first.getValue("next_offset").jsonPrimitive.int)

        val last = client.body(
            "list_tags",
            buildJsonObject {
                put("limit", 3)
                put("offset", 6)
            },
        )
        assertEquals(1, last.getValue("tags").jsonArray.size)
        assertFalse(last.getValue("has_more").jsonPrimitive.boolean)
    }

    @Test
    fun everyListToolTakesLimitAndOffset() = withClient(AccessMode.ReadOnly) { client ->
        client.listTools().tools.filter { it.name.startsWith("list_") }.forEach {
            val properties = it.inputSchema.properties.orEmpty()
            assertTrue("${it.name} should take a limit", properties.containsKey("limit"))
            assertTrue("${it.name} should take an offset", properties.containsKey("offset"))
        }
    }

    @Test
    fun theDefaultLimitIsAHundred() = withClient(AccessMode.ReadOnly) { client ->
        client.listTools().tools.filter { it.name.startsWith("list_") }.forEach {
            val limit = it.inputSchema.properties!!.getValue("limit").jsonObject
            assertEquals(it.name, 100, limit.getValue("default").jsonPrimitive.int)
        }
    }

    @Test
    fun aDateIsWrittenInTheFormItIsRead() = withClient(AccessMode.ReadWrite) { client ->
        val created = client.body(
            "create_tasks",
            buildJsonObject {
                put("tasks", JsonArray(listOf(buildJsonObject {
                    put("title", "Trash")
                    put("due_date", "2026-09-12T19:00:00")
                })))
            },
        )
        val row = created.getValue("tasks").jsonArray.single().jsonObject
        val id = row.getValue("id").jsonPrimitive.long
        assertEquals("2026-09-12T19:00:00", row.getValue("due").jsonObject.getValue("iso").jsonPrimitive.content)
        assertTrue(row["due_all_day"]?.jsonPrimitive?.boolean != true)

        val updated = client.body(
            "update_tasks",
            buildJsonObject {
                put("updates", JsonArray(listOf(buildJsonObject {
                    put("task_id", id)
                    put("due_date", "2026-09-13")
                })))
            },
        )
        val after = updated.getValue("tasks").jsonArray.single().jsonObject
        assertTrue(after.getValue("due_all_day").jsonPrimitive.boolean)
        assertTrue(after.getValue("due").jsonObject.getValue("iso").jsonPrimitive.content.startsWith("2026-09-13T"))
    }

    @Test
    fun aBoundIsReadInTheFormADateIsWritten() = withClient(AccessMode.ReadWrite) { client ->
        listOf("2026-09-11T09:00:00", "2026-09-12T09:00:00", "2026-09-13T09:00:00").forEach {
            client.body(
                "create_tasks",
                buildJsonObject {
                    put("tasks", JsonArray(listOf(buildJsonObject { put("title", it); put("due_date", it) })))
                },
            )
        }

        val page = client.body(
            "list_tasks",
            buildJsonObject { put("due_after", "2026-09-12"); put("due_before", "2026-09-13") },
        )

        assertEquals(
            listOf("2026-09-12T09:00:00"),
            page.getValue("tasks").jsonArray.map { it.jsonObject.getValue("title").jsonPrimitive.content },
        )
    }

    @Test
    fun aCountIsJustATotal() = withClient(AccessMode.ReadWrite) { client ->
        repeat(3) { api.createTask(title = "Task $it") }

        val counted = client.body("list_tasks", buildJsonObject { put("limit", 0) })
        val paged = client.body("list_tasks", buildJsonObject { put("limit", 1) })

        assertEquals(3, counted.getValue("total").jsonPrimitive.int)
        assertEquals("a count has nothing to page", setOf("total"), counted.keys)
        assertTrue("a real short page still explains itself", "hint" in paged.keys)
    }

    @Test
    fun everyListToolFiltersById() = withClient(AccessMode.ReadOnly) { client ->
        val idFilter = mapOf(
            "list_tasks" to "task_ids",
            "list_task_lists" to "list_ids",
            "list_tags" to "tag_ids",
            "list_places" to "place_ids",
            "list_accounts" to "account_ids",
            "list_reminders" to "task_ids",
        )
        client.listTools().tools.filter { it.name.startsWith("list_") }.forEach {
            val expected = idFilter.getValue(it.name)
            assertTrue(
                "${it.name} should filter on $expected",
                it.inputSchema.properties!!.containsKey(expected),
            )
        }
    }

    @Test
    fun aBatchUpdateAppliesEveryEntryInOneCall() = withClient(AccessMode.ReadWrite) { client ->
        val one = api.createTask(title = "One")
        val two = api.createTask(title = "Two")

        val body = client.body(
            "update_tasks",
            buildJsonObject {
                put(
                    "updates",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("task_id", one)
                                put("title", "One enriched")
                            },
                            buildJsonObject {
                                put("task_id", two)
                                put("notes", "context")
                            },
                        )
                    ),
                )
            },
        )

        assertEquals(2, body.getValue("rows_changed").jsonPrimitive.int)
        assertEquals(2, body.getValue("results").jsonArray.size)
        assertEquals("the tasks come back as they are afterwards", 2, body.getValue("tasks").jsonArray.size)
        assertEquals("One enriched", api.getTask(one)!!.title)
        assertEquals("context", api.getTask(two)!!.notes)
    }

    @Test
    fun aSingleRowWriteHandsBackTheRowAndAMissingRowIsAnError() = withClient(AccessMode.ReadWrite) { client ->
        val created = client.body(
            "create_task_list",
            buildJsonObject {
                put("account_id", accountId())
                put("title", "Garage")
            },
        ).getValue("list").jsonObject
        val id = created.getValue("id").jsonPrimitive.long

        val renamed = client.body(
            "update_task_list",
            buildJsonObject {
                put("list_id", id)
                put("title", "Workshop")
            },
        ).getValue("list").jsonObject
        val missing = client.callRaw(
            "update_task_list",
            buildJsonObject {
                put("list_id", 9_999L)
                put("title", "Nowhere")
            },
        )

        assertEquals("Garage", created.getValue("title").jsonPrimitive.content)
        assertEquals("Workshop", renamed.getValue("title").jsonPrimitive.content)
        assertEquals(true, missing.isError)
    }

    @Test
    fun createTasksMakesSeveralInOneCall() = withClient(AccessMode.ReadWrite) { client ->
        val body = client.body(
            "create_tasks",
            buildJsonObject {
                put(
                    "tasks",
                    JsonArray(
                        listOf(
                            buildJsonObject { put("title", "Milk") },
                            buildJsonObject { put("title", "Eggs") },
                        )
                    ),
                )
            },
        )

        assertEquals(2, body.getValue("created_task_ids").jsonArray.size)
        assertEquals(2, body.getValue("tasks").jsonArray.size)
        assertEquals(2, api.countTasks())
    }

    @Test
    fun completeTasksMarksEveryIdAndReportsPerTask() = withClient(AccessMode.ReadWrite) { client ->
        val ids = api.createTasks(listOf(TaskWrite(title = "One"), TaskWrite(title = "Two"))).ids

        val body = client.body(
            "complete_tasks",
            buildJsonObject { put("task_ids", JsonArray(ids.map { JsonPrimitive(it) })) },
        )

        assertEquals(2, body.getValue("rows_changed").jsonPrimitive.int)
        assertEquals(2, body.getValue("results").jsonArray.size)
        ids.forEach { assertNotNull(api.getTask(it)!!.completed) }
    }

    @Test
    fun setTaskTagsTagsEveryTaskYouName() = withClient(AccessMode.ReadWrite) { client ->
        val tag = api.createTag(TagWrite(name = "read-later")).row.id
        val ids = api.createTasks(listOf(TaskWrite(title = "One"), TaskWrite(title = "Two"))).ids

        val body = client.body(
            "set_task_tags",
            buildJsonObject {
                put("task_ids", JsonArray(ids.map { JsonPrimitive(it) }))
                put("add_tag_ids", JsonArray(listOf(JsonPrimitive(tag))))
            },
        )

        assertEquals(2, body.getValue("added").jsonPrimitive.int)
        ids.forEach { assertEquals(listOf(tag), api.getTask(it)!!.tagIds) }
    }

    @Test
    fun settingRemindersHandsBackTheOnesTheTaskEndsUpWith() = withClient(AccessMode.ReadWrite) { client ->
        val id = api.createTask(title = "Dentist", dueDate = DUE, dueAllDay = true)

        val body = client.body(
            "set_task_reminders",
            buildJsonObject {
                put("task_id", id)
                put(
                    "add",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("type", "relative_due")
                                put("offset_ms", -3_600_000L)
                            }
                        )
                    ),
                )
            },
        )

        assertEquals(1, body.getValue("added_reminder_ids").jsonArray.size)
        assertEquals(0, body.getValue("removed").jsonPrimitive.int)
        val reminders = body.getValue("reminders").jsonArray
        assertEquals(1, reminders.size)
        assertEquals(
            "relative_due",
            reminders.single().jsonObject.getValue("type").jsonPrimitive.content,
        )
    }

    @Test
    fun theTaskWriteToolsArePluralOnly() = withClient(AccessMode.ReadWrite) { client ->
        val names = client.listTools().tools.map { it.name }.toSet()

        listOf("create_tasks", "update_tasks", "complete_tasks").forEach {
            assertTrue("$it should be registered", it in names)
        }
        listOf("create_task", "update_task", "complete_task", "get_task").forEach {
            assertFalse("$it should be gone", it in names)
        }
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
            )
        )
    )

    private suspend fun Client.callRaw(tool: String, arguments: JsonObject) =
        callTool(CallToolRequest(CallToolRequestParams(name = tool, arguments = arguments)))

    private suspend fun Client.body(tool: String, arguments: JsonObject): JsonObject {
        val text = callRaw(tool, arguments)
            .content
            .filterIsInstance<TextContent>()
            .firstOrNull()
            ?.text
        assertNotNull("$tool returned no content", text)
        return Json.parseToJsonElement(text!!).jsonObject
    }

    private suspend fun Client.rows(
        tool: String,
        key: String,
        vararg fields: String,
    ): List<JsonObject> {
        val text = call(tool, *fields)
            .content
            .filterIsInstance<TextContent>()
            .firstOrNull()
            ?.text
        assertNotNull("$tool returned no content", text)
        return Json.parseToJsonElement(text!!).jsonObject.getValue(key).jsonArray.map { it.jsonObject }
    }

    private fun withClient(mode: AccessMode, block: suspend (Client) -> Unit) =
        withClient(ToolPermissions.of(mode), block)

    private fun withClient(permissions: ToolPermissions, block: suspend (Client) -> Unit) = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val instance = McpHttpServer(
            port = port,
            token = { TOKEN },
            permissions = permissions,
            api = api,
            log = ActivityLog(),
            onStateChange = { _, _ -> },
        )
        server = instance
        instance.start()

        val http = HttpClient()
        val client = Client(Implementation(name = "test-client", version = "1.0"))
        try {
            client.connect(
                StreamableHttpClientTransport(
                    client = http,
                    url = McpHttpServer.endpoint(port),
                ) { headers.append(HttpHeaders.Authorization, "Bearer $TOKEN") }
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
        const val TOKEN = "test-token"

        const val DUE = 1_757_000_000_000L

        val READ_TOOLS = setOf(
            "list_tasks", "list_task_lists",
            "list_tags", "list_places", "list_accounts", "list_reminders",
        )

        val WRITE_TOOLS = setOf(
            "create_tasks", "update_tasks", "complete_tasks", "delete_task",
            "create_task_list", "update_task_list", "delete_task_list",
            "create_tag", "update_tag", "delete_tag", "set_task_tags",
            "create_place", "update_place", "delete_place", "set_task_reminders",
        )
    }
}
