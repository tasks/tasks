package org.tasks.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.data.TagFilters
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterProvider
import org.tasks.preferences.TasksPreferences
import java.time.ZoneId

class AiTaskParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeProvider(private val service: OpenRouterService?) : OpenRouterClientProvider {
        override suspend fun getService(): OpenRouterService? = service
        override suspend fun validateKey(rawKey: String): List<ModelInfo> = emptyList()
    }

    private fun serviceReturning(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): OpenRouterService {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString(),
                ),
            )
        }
        return OpenRouterService(
            HttpClient(engine) { installOpenRouter(apiKey = "sk-test", log = {}) }
        )
    }

    private fun chatBody(content: String): String {
        val escaped = json.encodeToString(JsonPrimitive(content))
        return """{"choices":[{"message":{"role":"assistant","content":$escaped}}]}"""
    }

    private val lists: List<Filter> = listOf(
        CaldavFilter(
            calendar = CaldavCalendar(name = "Personal", uuid = "cal-1"),
            account = CaldavAccount(accountType = CaldavAccount.TYPE_CALDAV),
        )
    )

    private fun parser(service: OpenRouterService?): AiTaskParser {
        val filterProvider = mock<FilterProvider> {
            onBlocking { allLists() } doReturn lists
        }
        val tagDataDao = mock<TagDataDao> {
            onBlocking { getTagFilters(any()) } doReturn listOf(
                TagFilters(tagData = TagData(name = "errand"), count = 0)
            )
        }
        return AiTaskParser(
            clientProvider = FakeProvider(service),
            filterProvider = filterProvider,
            tagDataDao = tagDataDao,
            tasksPreferences = TasksPreferences(InMemoryDataStore()),
            json = json,
            zoneId = { ZoneId.of("America/Chicago") },
        )
    }

    @Test
    fun unconfiguredProviderReturnsNotConfigured() = runBlocking {
        assertEquals(AiParseResult.NotConfigured, parser(null).parse("buy milk"))
    }

    @Test
    fun blankInputIsRejectedWithoutACall() = runBlocking {
        assertEquals(
            AiParseResult.Failure(AiFailure.NO_TASKS),
            parser(null).parse("   "),
        )
    }

    @Test
    fun multiTaskResponseYieldsMultipleParsedTasks() = runBlocking {
        val content = """
            {"tasks":[
              {"title":"Call the dentist","notes":null,"list":"Personal","tags":["errand"],
               "due":"2026-09-01T14:00","start":null,"priority":"high","recurrence":null},
              {"title":"Pick up milk","notes":null,"list":null,"tags":[],
               "due":null,"start":null,"priority":null,"recurrence":null}
            ]}
        """.trimIndent()

        val result = parser(serviceReturning(chatBody(content))).parse("dentist and milk")

        assertTrue(result is AiParseResult.Success)
        val tasks = (result as AiParseResult.Success).tasks
        assertEquals(2, tasks.size)
        assertEquals("Call the dentist", tasks[0].title)
        assertEquals("2026-09-01T14:00", tasks[0].due)
        assertEquals("high", tasks[0].priority)
        assertEquals(listOf("errand"), tasks[0].tags)
        assertEquals("Pick up milk", tasks[1].title)
    }

    @Test
    fun blankTitlesAreDropped() = runBlocking {
        val content = """
            {"tasks":[
              {"title":"   ","notes":null,"list":null,"tags":[],
               "due":null,"start":null,"priority":null,"recurrence":null},
              {"title":"Real task","notes":null,"list":null,"tags":[],
               "due":null,"start":null,"priority":null,"recurrence":null}
            ]}
        """.trimIndent()

        val result = parser(serviceReturning(chatBody(content))).parse("something")

        assertEquals(
            listOf("Real task"),
            (result as AiParseResult.Success).tasks.map { it.title },
        )
    }

    @Test
    fun emptyTaskListIsNoTasks() = runBlocking {
        val result = parser(serviceReturning(chatBody("""{"tasks":[]}"""))).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.NO_TASKS), result)
    }

    @Test
    fun malformedContentIsBadResponse() = runBlocking {
        val result = parser(serviceReturning(chatBody("I am prose, not JSON"))).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.BAD_RESPONSE), result)
    }

    @Test
    fun missingChoiceIsBadResponse() = runBlocking {
        val result = parser(serviceReturning("""{"choices":[]}""")).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.BAD_RESPONSE), result)
    }

    @Test
    fun rateLimitMapsToRateLimited() = runBlocking {
        val result = parser(serviceReturning("{}", HttpStatusCode.TooManyRequests)).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.RATE_LIMITED), result)
    }

    @Test
    fun unauthorizedMapsToUnauthorized() = runBlocking {
        val result = parser(serviceReturning("{}", HttpStatusCode.Unauthorized)).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.UNAUTHORIZED), result)
    }

    @Test
    fun serverErrorMapsToUnavailable() = runBlocking {
        val result = parser(serviceReturning("{}", HttpStatusCode.InternalServerError)).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.UNAVAILABLE), result)
    }

    @Test
    fun otherHttpErrorsMapToNetwork() = runBlocking {
        val result = parser(serviceReturning("{}", HttpStatusCode.BadRequest)).parse("hmm")

        assertEquals(AiParseResult.Failure(AiFailure.NETWORK), result)
    }
}
