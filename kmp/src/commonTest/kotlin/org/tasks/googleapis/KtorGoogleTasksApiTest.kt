package org.tasks.googleapis

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorGoogleTasksApiTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var responses: MutableList<MockRequestHandleScope.(HttpRequestData) -> HttpResponseData> =
        mutableListOf()
    private val auth = FakeAuth()

    @Test
    fun listsTaskLists() = runTest {
        respondWith("""{"items":[{"id":"list","title":"My Tasks","etag":"e"}],"nextPageToken":"next"}""")

        val lists = api().allGtaskLists("page")

        assertEquals(
            GtasksTaskLists(
                items = listOf(GtasksTaskList(id = "list", title = "My Tasks", etag = "e")),
                nextPageToken = "next",
            ),
            lists,
        )
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals(
            "https://tasks.googleapis.com/tasks/v1/users/@me/lists?maxResults=100&pageToken=page",
            requests.single().url.toString(),
        )
    }

    @Test
    fun leavesOutAMissingPageToken() = runTest {
        respondWith("{}")

        api().allGtaskLists(null)

        assertEquals(
            "https://tasks.googleapis.com/tasks/v1/users/@me/lists?maxResults=100",
            requests.single().url.toString(),
        )
    }

    @Test
    fun fetchesChangedTasksIncludingDeletedAndHidden() = runTest {
        respondWith("""{"items":[{"id":"a","updated":"2026-11-24T17:45:12.345Z"}]}""")

        val tasks = api().getAllGtasksFromListId("list", 0, null)

        assertEquals(listOf(GtasksTask(id = "a", updated = "2026-11-24T17:45:12.345Z")), tasks?.items)
        val url = requests.single().url
        assertEquals("/tasks/v1/lists/list/tasks", url.encodedPath)
        assertEquals("100", url.parameters["maxResults"])
        assertEquals("true", url.parameters["showDeleted"])
        assertEquals("true", url.parameters["showHidden"])
        assertEquals(GtasksDates.unixTimeToGtasksCompletionTime(0), url.parameters["updatedMin"])
    }

    @Test
    fun fetchesOnlyPositions() = runTest {
        respondWith("""{"items":[{"id":"a","parent":"p","position":"1"}]}""")

        api().getAllPositions("list", "page")

        val url = requests.single().url
        assertEquals("/tasks/v1/lists/list/tasks", url.encodedPath)
        assertEquals("false", url.parameters["showDeleted"])
        assertEquals("false", url.parameters["showHidden"])
        assertEquals("page", url.parameters["pageToken"])
        assertEquals("items(id,parent,position),nextPageToken", url.parameters["fields"])
    }

    @Test
    fun createsATaskWithoutNullFields() = runTest {
        respondWith("""{"id":"new","title":"Title"}""")

        val created = api().createGtask(
            "list",
            GtasksTask(title = "Title", status = "needsAction"),
            parent = "parent",
            previous = null,
        )

        assertEquals(GtasksTask(id = "new", title = "Title"), created)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/tasks/v1/lists/list/tasks", request.url.encodedPath)
        assertEquals("parent", request.url.parameters["parent"])
        assertNull(request.url.parameters["previous"])
        assertEquals("""{"title":"Title","status":"needsAction"}""", request.bodyText())
    }

    @Test
    fun replacesATask() = runTest {
        respondWith("""{"id":"abc"}""")

        api().updateGtask("list", GtasksTask(id = "abc", title = "Title", status = "completed"))

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/tasks/v1/lists/list/tasks/abc", request.url.encodedPath)
        assertEquals("""{"id":"abc","title":"Title","status":"completed"}""", request.bodyText())
    }

    @Test
    fun movesATask() = runTest {
        respondWith("""{"id":"abc","parent":"p","position":"2"}""")

        val moved = api().moveGtask("list", "abc", parentId = "p", previousId = "prev")

        assertEquals(GtasksTask(id = "abc", parent = "p", position = "2"), moved)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/tasks/v1/lists/list/tasks/abc/move", request.url.encodedPath)
        assertEquals("p", request.url.parameters["parent"])
        assertEquals("prev", request.url.parameters["previous"])
    }

    @Test
    fun deletesATask() = runTest {
        responses += { respond("", HttpStatusCode.NoContent) }

        api().deleteGtask("list", "abc")

        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("/tasks/v1/lists/list/tasks/abc", requests.single().url.encodedPath)
    }

    @Test
    fun deletingATaskThatIsAlreadyGoneSucceeds() = runTest {
        responses += { respond("", HttpStatusCode.NotFound) }

        api().deleteGtask("list", "abc")
    }

    @Test
    fun encodesIdsInThePath() = runTest {
        respondWith("{}")

        api().getAllPositions("a/b c", null)

        assertEquals("/tasks/v1/lists/a%2Fb%20c/tasks", requests.single().url.encodedPath)
    }

    @Test
    fun createsRenamesAndDeletesLists() = runTest {
        respondWith("""{"id":"list","title":"Groceries"}""")
        respondWith("""{"id":"list","title":"Food"}""")
        responses += { respond("", HttpStatusCode.NoContent) }

        val created = api().createGtaskList("Groceries")
        api().renameGtaskList("list", "Food")
        api().deleteGtaskList("list")

        assertEquals(GoogleTaskList(id = "list", title = "Groceries"), created)
        assertEquals(
            listOf(HttpMethod.Post, HttpMethod.Patch, HttpMethod.Delete),
            requests.map { it.method },
        )
        assertEquals(
            listOf("/tasks/v1/users/@me/lists", "/tasks/v1/users/@me/lists/list", "/tasks/v1/users/@me/lists/list"),
            requests.map { it.url.encodedPath },
        )
        assertEquals("""{"title":"Groceries"}""", requests[0].bodyText())
        assertEquals("""{"title":"Food"}""", requests[1].bodyText())
    }

    @Test
    fun sendsTheAccessToken() = runTest {
        respondWith("{}")

        api().allGtaskLists(null)

        assertEquals("Bearer token-1", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun refreshesTheTokenOnceAfterAnUnauthorizedResponse() = runTest {
        responses += { respond("", HttpStatusCode.Unauthorized) }
        respondWith("{}")

        api().allGtaskLists(null)

        assertEquals(listOf("token-1"), auth.invalidated)
        assertEquals(
            listOf("Bearer token-1", "Bearer token-2"),
            requests.map { it.headers[HttpHeaders.Authorization] },
        )
    }

    @Test
    fun anUnauthorizedResponseFailsWhenTheTokenCannotBeRefreshed() = runTest {
        auth.canRefresh = false
        responses += { respond("", HttpStatusCode.Unauthorized) }

        val e = assertFailsWith<GoogleTasksException> { api().allGtaskLists(null) }

        assertEquals(401, e.statusCode)
        assertEquals(1, requests.size)
    }

    @Test
    fun aSecondUnauthorizedResponseFails() = runTest {
        responses += { respond("", HttpStatusCode.Unauthorized) }
        responses += { respond("", HttpStatusCode.Unauthorized) }

        val e = assertFailsWith<GoogleTasksException> { api().allGtaskLists(null) }

        assertEquals(401, e.statusCode)
        assertEquals(2, requests.size)
    }

    @Test
    fun notFoundIsHttpNotFound() = runTest {
        responses += { respond("", HttpStatusCode.NotFound) }

        assertFailsWith<HttpNotFoundException> { api().getAllGtasksFromListId("list", 0, null) }
    }

    @Test
    fun aRejectionCarriesTheStatusAndErrorMessage() = runTest {
        responses += {
            respond(
                """{"error":{"code":400,"message":"Invalid task ID","errors":[]}}""",
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val e = assertFailsWith<GoogleTasksException> {
            api().updateGtask("list", GtasksTask(id = "abc"))
        }

        assertEquals(400, e.statusCode)
        assertEquals("Invalid task ID", e.errorMessage)
        assertTrue(e.message!!.startsWith("400 Bad Request"))
    }

    @Test
    fun aRejectionWithoutAJsonBodyHasNoErrorMessage() = runTest {
        responses += { respond("<html>busy</html>", HttpStatusCode.ServiceUnavailable) }

        val e = assertFailsWith<GoogleTasksException> { api().allGtaskLists(null) }

        assertEquals(503, e.statusCode)
        assertNull(e.errorMessage)
    }

    private fun respondWith(body: String) {
        responses += {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    }

    private fun api() = KtorGoogleTasksApi(
        httpClient = HttpClient(MockEngine { request ->
            requests += request
            responses.removeAt(0)(this, request)
        }),
        auth = auth,
    )

    private fun HttpRequestData.bodyText(): String = (body as TextContent).text

    private class FakeAuth : GoogleTasksAuth {
        var canRefresh = true
        var current = 1
        val invalidated = mutableListOf<String>()

        override suspend fun accessToken() = "token-$current"

        override suspend fun invalidate(rejected: String): Boolean {
            invalidated += rejected
            if (!canRefresh) return false
            current++
            return true
        }
    }
}
