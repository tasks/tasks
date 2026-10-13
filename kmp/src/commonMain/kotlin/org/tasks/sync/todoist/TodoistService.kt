package org.tasks.sync.todoist

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TodoistService(
    private val client: HttpClient,
) {
    private val baseUrl = BASE_URL
    private val projectsUrl = "$baseUrl/projects"

    suspend fun sync(
        syncToken: String = FULL_SYNC,
        resourceTypes: List<String> = TodoistResourceTypes.DEFAULT,
        commands: List<TodoistCommand> = emptyList(),
    ): TodoistSyncResponse =
        client
            .submitForm(
                url = "$baseUrl/sync",
                formParameters = parameters {
                    append("sync_token", syncToken)
                    append("resource_types", json.encodeToString(resourceTypes))
                    if (commands.isNotEmpty()) {
                        append("commands", json.encodeToString(commands))
                    }
                },
            )
            .body()

    suspend fun getProject(id: String): TodoistProject = client.get("$projectsUrl/$id").body()

    suspend fun createProject(name: String, color: String? = null): TodoistProject =
        client
            .post(projectsUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("name", name)
                        color?.let { put("color", it) }
                    }
                )
            }
            .body()

    suspend fun updateProject(id: String, name: String? = null, color: String? = null): TodoistProject =
        client
            .post("$projectsUrl/$id") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        name?.let { put("name", it) }
                        color?.let { put("color", it) }
                    }
                )
            }
            .body()

    suspend fun deleteProject(id: String) = client.delete("$projectsUrl/$id")

    suspend fun getCompletedByCompletionDate(
        since: String,
        until: String,
        cursor: String? = null,
        limit: Int = PAGE_SIZE,
    ): TodoistPage<TodoistItem> =
        client
            .get("$baseUrl/tasks/completed/by_completion_date") {
                parameter("since", since)
                parameter("until", until)
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
            }
            .body()

    companion object {
        const val BASE_URL = "https://api.todoist.com/api/v1"
        const val FULL_SYNC = "*"

        const val PAGE_SIZE = 200

        private val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }
}

@kotlinx.serialization.Serializable
data class TodoistPage<T>(
    val results: List<T> = emptyList(),
    @kotlinx.serialization.SerialName("next_cursor") val nextCursor: String? = null,
)
