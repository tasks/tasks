package org.tasks.googleapis

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.TasksBuildConfig

interface GoogleTasksAuth {
    suspend fun accessToken(): String

    suspend fun invalidate(rejected: String): Boolean
}

class KtorGoogleTasksApi(
    private val httpClient: HttpClient,
    private val auth: GoogleTasksAuth,
    private val baseUrl: String = BASE_URL,
) : GoogleTasksApi {

    override suspend fun allGtaskLists(pageToken: String?): GtasksTaskLists? =
        call(HttpMethod.Get, listOf("users", "@me", "lists")) {
            parameter("maxResults", "100")
            parameter("pageToken", pageToken)
        }?.let { json.decodeFromString<GtasksTaskLists>(it) }

    override suspend fun getAllGtasksFromListId(
        listId: String?,
        lastSyncDate: Long,
        pageToken: String?,
    ): GtasksTasks? =
        call(HttpMethod.Get, listOf("lists", requireNotNull(listId), "tasks")) {
            parameter("maxResults", "100")
            parameter("showDeleted", "true")
            parameter("showHidden", "true")
            parameter("pageToken", pageToken)
            parameter("updatedMin", GtasksDates.unixTimeToGtasksCompletionTime(lastSyncDate))
        }?.let { json.decodeFromString<GtasksTasks>(it) }

    override suspend fun getAllPositions(listId: String?, pageToken: String?): GtasksTasks? =
        call(HttpMethod.Get, listOf("lists", requireNotNull(listId), "tasks")) {
            parameter("maxResults", "100")
            parameter("showDeleted", "false")
            parameter("showHidden", "false")
            parameter("pageToken", pageToken)
            parameter("fields", "items(id,parent,position),nextPageToken")
        }?.let { json.decodeFromString<GtasksTasks>(it) }

    override suspend fun createGtask(
        listId: String?,
        task: GtasksTask,
        parent: String?,
        previous: String?,
    ): GtasksTask? {
        Logger.d(TAG) { "createGtask(listId=$listId, task=<redacted>, parent=$parent, previous=$previous)" }
        return call(HttpMethod.Post, listOf("lists", requireNotNull(listId), "tasks"), json.encodeToString(task)) {
            parameter("parent", parent)
            parameter("previous", previous)
        }?.let { json.decodeFromString<GtasksTask>(it) }
    }

    override suspend fun updateGtask(listId: String?, task: GtasksTask): GtasksTask? =
        call(
            HttpMethod.Put,
            listOf("lists", requireNotNull(listId), "tasks", requireNotNull(task.id)),
            json.encodeToString(task),
        )?.let { json.decodeFromString<GtasksTask>(it) }

    override suspend fun moveGtask(
        listId: String?,
        taskId: String?,
        parentId: String?,
        previousId: String?,
    ): GtasksTask? {
        Logger.d(TAG) { "moveGtask(listId=$listId, taskId=$taskId, parentId=$parentId, previousId=$previousId)" }
        return call(
            HttpMethod.Post,
            listOf("lists", requireNotNull(listId), "tasks", requireNotNull(taskId), "move"),
        ) {
            parameter("parent", parentId)
            parameter("previous", previousId)
        }?.let { json.decodeFromString<GtasksTask>(it) }
    }

    override suspend fun deleteGtask(listId: String?, taskId: String?) {
        Logger.d(TAG) { "deleteGtask(listId=$listId, taskId=$taskId)" }
        try {
            call(HttpMethod.Delete, listOf("lists", requireNotNull(listId), "tasks", requireNotNull(taskId)))
        } catch (_: HttpNotFoundException) {
        }
    }

    override suspend fun createGtaskList(title: String): GoogleTaskList =
        call(HttpMethod.Post, listOf("users", "@me", "lists"), json.encodeToString(ListTitle(title)))
            ?.let { json.decodeFromString<GtasksTaskList>(it) }
            ?.let { GoogleTaskList(id = requireNotNull(it.id), title = it.title.orEmpty()) }
            ?: throw IllegalStateException("Google Tasks did not return a list")

    override suspend fun renameGtaskList(listId: String, title: String) {
        call(HttpMethod.Patch, listOf("users", "@me", "lists", listId), json.encodeToString(ListTitle(title)))
    }

    override suspend fun deleteGtaskList(listId: String) {
        try {
            call(HttpMethod.Delete, listOf("users", "@me", "lists", listId))
        } catch (_: HttpNotFoundException) {
        }
    }

    private class QueryParameters {
        val values = mutableListOf<Pair<String, String>>()

        fun parameter(name: String, value: String?) {
            value?.let { values += name to it }
        }
    }

    private suspend fun call(
        method: HttpMethod,
        path: List<String>,
        body: String? = null,
        parameters: QueryParameters.() -> Unit = {},
    ): String? {
        val url = URLBuilder()
            .takeFrom(baseUrl)
            .apply {
                appendPathSegments(path, encodeSlash = true)
                QueryParameters().apply(parameters).values.forEach { (name, value) ->
                    this.parameters.append(name, value)
                }
            }
            .build()
        Logger.d(TAG) { "$method $url" }
        var token = auth.accessToken()
        var response = send(method, url.toString(), token, body)
        if (response.status.value == 401 && auth.invalidate(token)) {
            token = auth.accessToken()
            response = send(method, url.toString(), token, body)
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            Logger.d(TAG) { "$method $url -> ${if (TasksBuildConfig.DEBUG) text else "<redacted>"}" }
            return text.takeIf { it.isNotBlank() }
        }
        val status = "${response.status.value} ${response.status.description}"
        if (response.status.value == 404) {
            throw HttpNotFoundException(status)
        }
        val errorMessage = try {
            json.decodeFromString<ErrorResponse>(text).error?.message
        } catch (_: Exception) {
            null
        }
        throw GoogleTasksException(
            statusCode = response.status.value,
            message = listOfNotNull(status, errorMessage).joinToString("\n"),
            errorMessage = errorMessage,
        )
    }

    private suspend fun send(
        method: HttpMethod,
        url: String,
        token: String,
        body: String?,
    ): HttpResponse = httpClient.request {
        this.method = method
        url(url)
        header(HttpHeaders.Authorization, "Bearer $token")
        body?.let { setBody(TextContent(it, ContentType.Application.Json)) }
    }

    @Serializable
    private data class ListTitle(val title: String)

    @Serializable
    private data class ErrorResponse(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(val message: String? = null)

    companion object {
        private const val TAG = "KtorGoogleTasksApi"
        const val BASE_URL = "https://tasks.googleapis.com/tasks/v1"

        internal val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
