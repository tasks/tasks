package org.tasks.googleapis

import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.TaskLists
import com.google.api.services.tasks.model.Tasks
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Test

class GtasksModelParityTest {
    private val gson = GsonFactory.getDefaultInstance()
    private val json = KtorGoogleTasksApi.json

    @Test
    fun aNewTaskSerializesLikeTheJavaClient() = assertSameJson(
        GtasksTask(title = "Title", notes = "Notes", status = "needsAction"),
    )

    @Test
    fun aTaskWithADueDateSerializesLikeTheJavaClient() = assertSameJson(
        GtasksTask(
            id = "abc",
            title = "Title",
            due = "2026-11-24T00:00:00.000Z",
            status = "needsAction",
        ),
    )

    @Test
    fun aCompletedTaskSerializesLikeTheJavaClient() = assertSameJson(
        GtasksTask(
            id = "abc",
            title = "Title",
            completed = "2026-11-24T10:15:30.123-06:00",
            status = "completed",
        ),
    )

    @Test
    fun aDeletedTaskSerializesLikeTheJavaClient() = assertSameJson(
        GtasksTask(id = "abc", deleted = true, title = "", status = "needsAction"),
    )

    @Test
    fun anEmptyTaskSerializesLikeTheJavaClient() = assertSameJson(GtasksTask())

    @Test
    fun everyFieldSurvivesTheJavaModel() {
        assertEquals(FULL_TASK, FULL_TASK.toJava().toGtasks())
    }

    @Test
    fun aServerTaskReadsTheSameThroughBothClients() {
        val response = """
            {
              "kind": "tasks#task",
              "id": "abc",
              "etag": "\"etag\"",
              "title": "Title",
              "updated": "2026-11-24T17:45:12.345Z",
              "selfLink": "https://www.googleapis.com/tasks/v1/lists/list/tasks/abc",
              "parent": "parent",
              "position": "00000000000000000001",
              "notes": "Notes",
              "status": "completed",
              "due": "2026-11-24T00:00:00.000Z",
              "completed": "2026-11-24T17:45:12.345Z",
              "deleted": false,
              "hidden": true,
              "links": [{"type": "email", "description": "d", "link": "https://example.com"}],
              "webViewLink": "https://tasks.google.com/task/abc",
              "assignmentInfo": {"surfaceType": "DOCUMENT"}
            }
        """.trimIndent()

        assertEquals(
            gson.fromString(response, Task::class.java).toGtasks(),
            json.decodeFromString<GtasksTask>(response),
        )
    }

    @Test
    fun aTaskPageReadsTheSameThroughBothClients() {
        val response = """
            {
              "kind": "tasks#tasks",
              "etag": "\"etag\"",
              "nextPageToken": "next",
              "items": [
                {"id": "a", "parent": "p", "position": "1"},
                {"id": "b", "position": "2"}
              ]
            }
        """.trimIndent()

        assertEquals(
            gson.fromString(response, Tasks::class.java).toGtasks(),
            json.decodeFromString<GtasksTasks>(response),
        )
    }

    @Test
    fun anEmptyTaskPageReadsTheSameThroughBothClients() {
        val response = """{"kind": "tasks#tasks", "etag": "\"etag\""}"""

        assertEquals(
            gson.fromString(response, Tasks::class.java).toGtasks(),
            json.decodeFromString<GtasksTasks>(response),
        )
    }

    @Test
    fun aListPageReadsTheSameThroughBothClients() {
        val response = """
            {
              "kind": "tasks#taskLists",
              "etag": "\"etag\"",
              "nextPageToken": "next",
              "items": [
                {
                  "kind": "tasks#taskList",
                  "id": "list",
                  "etag": "\"list-etag\"",
                  "title": "My Tasks",
                  "updated": "2026-11-24T17:45:12.345Z",
                  "selfLink": "https://www.googleapis.com/tasks/v1/users/@me/lists/list"
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            gson.fromString(response, TaskLists::class.java).toGtasks(),
            json.decodeFromString<GtasksTaskLists>(response),
        )
    }

    @Test
    fun aListReadsTheSameThroughBothClients() {
        val response = """{"id": "list", "title": "My Tasks", "etag": "\"e\""}"""

        assertEquals(
            gson.fromString(response, TaskList::class.java).toGtasks(),
            json.decodeFromString<GtasksTaskList>(response),
        )
    }

    private fun assertSameJson(task: GtasksTask) {
        assertEquals(
            parse(gson.toString(task.toJava())),
            parse(json.encodeToString(task)),
        )
    }

    private fun parse(text: String): JsonElement = Json.parseToJsonElement(text)

    companion object {
        private val FULL_TASK = GtasksTask(
            id = "abc",
            etag = "\"etag\"",
            title = "Title",
            notes = "Notes",
            status = "completed",
            due = "2026-11-24T00:00:00.000Z",
            completed = "2026-11-24T17:45:12.345Z",
            deleted = false,
            hidden = true,
            parent = "parent",
            position = "00000000000000000001",
            updated = "2026-11-24T17:45:12.345Z",
        )
    }
}
