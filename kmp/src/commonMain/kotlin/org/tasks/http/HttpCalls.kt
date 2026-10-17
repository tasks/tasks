package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

suspend fun <T> KtorClientFactory.withClient(
    callTimeoutMillis: Long,
    block: suspend (HttpClient) -> T,
): T {
    val client = newClient {
        install(HttpTimeout) { requestTimeoutMillis = callTimeoutMillis }
    }
    return try {
        block(client)
    } finally {
        client.close()
    }
}

fun <T> HttpRequestBuilder.jsonBody(json: Json, serializer: SerializationStrategy<T>, body: T) {
    contentType(ContentType.Application.Json)
    setBody(json.encodeToString(serializer, body))
}
