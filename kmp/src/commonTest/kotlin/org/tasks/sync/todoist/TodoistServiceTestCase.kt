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

abstract class TodoistServiceTestCase {
    protected val requests = mutableListOf<HttpRequestData>()

    protected fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"sync_token": "next-token"}""",
    ): TodoistService {
        val engine = MockEngine { request ->
            requests.add(request)
            if (status.isSuccess()) {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respondError(status)
            }
        }
        return TodoistService(
            HttpClient(engine) {
                installTodoist(log = {}, bearerToken = { "test-token" })
            }
        )
    }

    protected suspend fun HttpRequestData.bodyText(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        EmptyContent -> ""
        else -> error("unexpected body type ${content::class.simpleName}")
    }

    protected fun String.formParams(): Map<String, String> =
        split("&")
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val (k, v) = pair.split("=", limit = 2)
                k.decodeForm() to v.decodeForm()
            }

    protected fun String.decodeForm(): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < length) {
            when (val c = this[i]) {
                '+' -> { bytes.add(' '.code.toByte()); i++ }
                '%' -> {
                    bytes.add(substring(i + 1, i + 3).toInt(16).toByte())
                    i += 3
                }
                else -> { bytes.addAll(c.toString().toByteArray().toList()); i++ }
            }
        }
        return bytes.toByteArray().decodeToString()
    }

}
