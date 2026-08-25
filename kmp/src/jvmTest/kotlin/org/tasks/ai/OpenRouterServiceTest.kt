package org.tasks.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.http.HttpException
import org.tasks.http.NotFoundException
import org.tasks.http.RateLimitedException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException

class OpenRouterServiceTest {

    private fun service(
        status: HttpStatusCode,
        body: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): OpenRouterService {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(
                    *(
                        listOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())) +
                            extraHeaders.map { it.key to listOf(it.value) }
                        ).toTypedArray()
                ),
            )
        }
        return OpenRouterService(
            HttpClient(engine) {
                installOpenRouter(apiKey = "sk-test", log = {})
            }
        )
    }

    private val chatRequest = ChatRequest(
        model = "some/model:free",
        messages = listOf(ChatMessage(role = "user", content = "hi")),
    )

    @Test
    fun parsesSuccessfulChatResponse() = runBlocking {
        val response = service(
            HttpStatusCode.OK,
            """
            {
              "choices": [
                {
                  "message": {"role": "assistant", "content": "{\"tasks\":[]}"},
                  "finish_reason": "stop"
                }
              ]
            }
            """.trimIndent(),
        ).chat(chatRequest)

        assertEquals(1, response.choices.size)
        assertEquals("{\"tasks\":[]}", response.choices[0].message?.content)
        assertEquals("stop", response.choices[0].finishReason)
    }

    @Test
    fun unauthorizedIncludesOpenRouterMessage() = runBlocking {
        val e = runCatching {
            service(
                HttpStatusCode.Unauthorized,
                """{"error": {"message": "No auth credentials found"}}""",
            ).chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is UnauthorizedException)
        assertTrue(e!!.message!!.contains("No auth credentials found"))
    }

    @Test
    fun forbiddenIsAlsoUnauthorized() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.Forbidden, """{}""").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is UnauthorizedException)
    }

    @Test
    fun notFoundMapsToNotFound() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.NotFound, """{}""").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is NotFoundException)
    }

    @Test
    fun rateLimitedCarriesRetryAfter() = runBlocking {
        val e = runCatching {
            service(
                HttpStatusCode.TooManyRequests,
                """{"error": {"message": "Rate limit exceeded"}}""",
                extraHeaders = mapOf("Retry-After" to "42"),
            ).chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is RateLimitedException)
        assertEquals(42L, (e as RateLimitedException).retryAfterSeconds)
    }

    @Test
    fun rateLimitedWithoutRetryAfterHasNullDelay() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.TooManyRequests, """{}""").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is RateLimitedException)
        assertNull((e as RateLimitedException).retryAfterSeconds)
    }

    @Test
    fun serverErrorMapsToServiceUnavailable() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.InternalServerError, """{}""").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is ServiceUnavailableException)
    }

    @Test
    fun otherErrorsMapToHttpException() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.BadRequest, """{}""").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is HttpException)
        assertEquals(400, (e as HttpException).code)
    }

    @Test
    fun unparseableErrorBodyStillMapsByStatus() = runBlocking {
        val e = runCatching {
            service(HttpStatusCode.Unauthorized, "not json at all").chat(chatRequest)
        }.exceptionOrNull()

        assertTrue(e is UnauthorizedException)
        assertEquals("HTTP 401", e!!.message)
    }

    @Test
    fun deserializesModelsIncludingSupportedParameters() = runBlocking {
        val models = service(
            HttpStatusCode.OK,
            """
            {
              "data": [
                {
                  "id": "vendor/model:free",
                  "name": "Vendor: Model (free)",
                  "supported_parameters": ["tools", "structured_outputs"],
                  "unknown_field": 1
                },
                {
                  "id": "vendor/paid",
                  "name": "Vendor: Paid"
                }
              ]
            }
            """.trimIndent(),
        ).listModels()

        assertEquals(2, models.size)
        assertTrue(models[0].isFree)
        assertTrue(models[0].supportsStructuredOutputs)
        assertEquals(false, models[1].isFree)
        assertEquals(false, models[1].supportsStructuredOutputs)
    }
}
