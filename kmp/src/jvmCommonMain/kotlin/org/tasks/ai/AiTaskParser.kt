package org.tasks.ai

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.tasks.data.dao.TagDataDao
import org.tasks.filters.CaldavFilter
import org.tasks.filters.FilterProvider
import org.tasks.http.NetworkException
import org.tasks.http.RateLimitedException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException
import org.tasks.preferences.TasksPreferences
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface AiParseResult {
    data class Success(val tasks: List<ParsedTask>) : AiParseResult
    data object NotConfigured : AiParseResult
    data class Failure(val reason: AiFailure) : AiParseResult
}

enum class AiFailure { UNAUTHORIZED, RATE_LIMITED, UNAVAILABLE, BAD_RESPONSE, NO_TASKS, NETWORK }

const val DEFAULT_AI_MODEL = "google/gemini-2.0-flash-exp:free"

class AiTaskParser(
    private val clientProvider: OpenRouterClientProvider,
    private val filterProvider: FilterProvider,
    private val tagDataDao: TagDataDao,
    private val tasksPreferences: TasksPreferences,
    private val json: Json,
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
) {
    /** Never throws: every failure mode is returned as an [AiParseResult]. */
    suspend fun parse(input: String): AiParseResult {
        if (input.isBlank()) return AiParseResult.Failure(AiFailure.NO_TASKS)

        val service = clientProvider.getService() ?: return AiParseResult.NotConfigured
        val model = tasksPreferences
            .get(TasksPreferences.aiModel, "")
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_AI_MODEL

        return try {
            val zone = zoneId()
            val response = service.chat(
                ChatRequest(
                    model = model,
                    messages = buildMessages(
                        input = input,
                        listNames = writableLists().map { it.title },
                        tagNames = tagDataDao.getTagFilters().mapNotNull { it.tagData.name },
                        nowIso = LocalDateTime.now(zone)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
                        timeZoneId = zone.id,
                    ),
                    responseFormat = ResponseFormat(
                        jsonSchema = JsonSchemaSpec(name = "tasks", schema = TASK_SCHEMA),
                    ),
                )
            )

            val content = response.choices.firstOrNull()?.message?.content
                ?: return AiParseResult.Failure(AiFailure.BAD_RESPONSE)

            val tasks = json
                .decodeFromString<ParsedTaskList>(content)
                .tasks
                .filter { it.title.isNotBlank() }

            if (tasks.isEmpty()) {
                AiParseResult.Failure(AiFailure.NO_TASKS)
            } else {
                AiParseResult.Success(tasks)
            }
        } catch (e: UnauthorizedException) {
            AiParseResult.Failure(AiFailure.UNAUTHORIZED)
        } catch (e: RateLimitedException) {
            AiParseResult.Failure(AiFailure.RATE_LIMITED)
        } catch (e: ServiceUnavailableException) {
            AiParseResult.Failure(AiFailure.UNAVAILABLE)
        } catch (e: SerializationException) {
            AiParseResult.Failure(AiFailure.BAD_RESPONSE)
        } catch (e: NetworkException) {
            AiParseResult.Failure(AiFailure.NETWORK)
        } catch (e: IOException) {
            AiParseResult.Failure(AiFailure.NETWORK)
        }
    }

    suspend fun writableLists(): List<CaldavFilter> =
        filterProvider
            .allLists()
            .filterIsInstance<CaldavFilter>()
            .filter { !it.isReadOnly }

    suspend fun knownTags() = tagDataDao.getTagFilters().map { it.tagData }
}
