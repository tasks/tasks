package org.tasks.sync.microsoft

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val error: ErrorBody,
) {
    @Serializable
    data class ErrorBody(
        val code: String,
        val message: String,
    )

    companion object {
        suspend fun HttpResponse.toMicrosoftError() = body<Error>()
    }
}