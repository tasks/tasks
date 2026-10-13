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

class TodoistServiceRestTest : TodoistServiceTestCase() {    @Test
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
    }    @Test
    fun deletesProjectOverRest() = runTest {
        service(body = "").deleteProject("p9")
        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/projects/p9", request.url.encodedPath)
    }    @Test
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
    }    @Test
    fun translates404ToNotFound() = runTest {
        assertFailsWith<NotFoundException> {
            service(status = HttpStatusCode.NotFound).getProject("missing")
        }
    }
}
