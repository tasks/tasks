package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class GraphTokenRefreshTest {

    private val sentTokens = mutableListOf<String?>()
    private val refreshCalls = mutableListOf<String?>()
    private val tokenRef = AtomicReference("old-token")

    private fun client(
        unauthorizedCode: String,
        unauthorizedBody: String = """{"error":{"code":"$unauthorizedCode","message":"no"}}""",
        refresh: (suspend (rejected: String?) -> String?)? = { rejected ->
            refreshCalls += rejected
            "new-token".also { tokenRef.set(it) }
        },
    ) = HttpClient(MockEngine { request ->
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
        sentTokens += token
        if (token == "new-token") {
            respond("ok")
        } else {
            respond(
                unauthorizedBody,
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }) {
        installMicrosoftGraph(
            cookiesStorage = AcceptAllCookiesStorage(),
            log = {},
            bearerToken = { tokenRef.get() },
            refreshToken = refresh,
        )
    }

    private suspend fun HttpClient.fetch() = get("https://graph.microsoft.com/v1.0/me/todo/lists").bodyAsText()

    @Test
    fun aRejectedTokenIsRefreshedAndTheRequestRetried() = runBlocking {
        assertEquals("ok", client("InvalidAuthenticationToken").fetch())
        assertEquals(listOf<String?>("old-token"), refreshCalls)
        assertEquals(listOf<String?>("old-token", "new-token"), sentTokens)
    }

    @Test
    fun aRejectedTokenIsRefreshedWhenTheErrorBodyOmitsOptionalFields() = runBlocking {
        val body = """{"error":{"code":"InvalidAuthenticationToken","message":null,"innerError":{"request-id":"r"}}}"""

        assertEquals("ok", client("InvalidAuthenticationToken", unauthorizedBody = body).fetch())
        assertEquals(listOf<String?>("old-token"), refreshCalls)
    }

    @Test
    fun anUnauthorizedErrorThatIsNotATokenErrorOnlyRefreshesAnExpiredToken() = runBlocking {
        assertEquals("ok", client("AccessDenied").fetch())
        assertEquals(listOf<String?>(null), refreshCalls)
    }

    @Test
    fun aForbiddenResponseIsNotRefreshed() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                """{"error":{"code":"AccessDenied","message":"no"}}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            installMicrosoftGraph(
                cookiesStorage = AcceptAllCookiesStorage(),
                log = {},
                bearerToken = { tokenRef.get() },
                refreshToken = { rejected -> refreshCalls += rejected; "new-token" },
            )
        }

        val thrown = runCatching { client.fetch() }.exceptionOrNull()

        assertEquals(403, (thrown as HttpException).code)
        assertEquals(emptyList<String?>(), refreshCalls)
    }

    @Test
    fun everyGraphTokenErrorRefreshesTheRejectedToken() = runBlocking {
        listOf("InvalidAuthenticationToken", "AuthenticationError", "UnknownError").forEach { code ->
            refreshCalls.clear()
            tokenRef.set("old-token")

            assertEquals("ok", client(code).fetch())
            assertEquals(code, listOf<String?>("old-token"), refreshCalls)
        }
    }

    @Test
    fun anUnchangedTokenIsNotRetriedAndTheGraphErrorIsKept() = runBlocking {
        val client = client("UnknownError", refresh = { rejected ->
            refreshCalls += rejected
            tokenRef.get()
        })

        val thrown = runCatching { client.fetch() }.exceptionOrNull()

        assertTrue(thrown is UnauthorizedException)
        assertEquals("UnknownError", (thrown as UnauthorizedException).graphCode)
        assertEquals(listOf<String?>("old-token"), sentTokens)
    }

    @Test
    fun aTokenSourceThatCannotRefreshKeepsTheGraphError() = runBlocking {
        val client = client("InvalidAuthenticationToken", refresh = { rejected ->
            refreshCalls += rejected
            null
        })

        val thrown = runCatching { client.fetch() }.exceptionOrNull()

        assertTrue(thrown is UnauthorizedException)
        assertEquals(listOf<String?>("old-token"), sentTokens)
    }

    private fun failing(status: HttpStatusCode, body: String) = HttpClient(MockEngine { request ->
        sentTokens += request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
        respond(body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        installMicrosoftGraph(
            cookiesStorage = AcceptAllCookiesStorage(),
            log = {},
            bearerToken = { tokenRef.get() },
            refreshToken = { rejected -> refreshCalls += rejected; "new-token" },
        )
    }

    @Test
    fun aServerErrorWithATokenErrorCodeIsNotRefreshedOrRetried() = runBlocking {
        val thrown = runCatching {
            failing(HttpStatusCode.InternalServerError, """{"error":{"code":"UnknownError","message":"no"}}""").fetch()
        }.exceptionOrNull()

        assertTrue(thrown is ServiceUnavailableException)
        assertEquals(emptyList<String?>(), refreshCalls)
        assertEquals(listOf<String?>("old-token"), sentTokens)
    }

    @Test
    fun aNotFoundWithATokenErrorCodeAndNoMessageIsNotFound() = runBlocking {
        val thrown = runCatching {
            failing(HttpStatusCode.NotFound, """{"error":{"code":"UnknownError","innerError":{"date":"d"}}}""").fetch()
        }.exceptionOrNull()

        assertTrue(thrown is NotFoundException)
        assertEquals(emptyList<String?>(), refreshCalls)
    }

    @Test
    fun aTokenErrorCodeOnAForbiddenResponseIsNotRefreshed() = runBlocking {
        val thrown = runCatching {
            failing(HttpStatusCode.Forbidden, """{"error":{"code":"InvalidAuthenticationToken","message":"no"}}""").fetch()
        }.exceptionOrNull()

        assertEquals(403, (thrown as UnauthorizedException).status)
        assertEquals(emptyList<String?>(), refreshCalls)
        assertEquals(listOf<String?>("old-token"), sentTokens)
    }

    @Test
    fun withoutARefreshAnUnauthorizedResponseIsNotRetried() = runBlocking {
        val thrown = runCatching { client("InvalidAuthenticationToken", refresh = null).fetch() }.exceptionOrNull()

        assertTrue(thrown is UnauthorizedException)
        assertEquals(listOf<String?>("old-token"), sentTokens)
    }
}
