package org.tasks.sync.microsoft

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response

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
        fun <T> Response<T>.toMicrosoftError()
            = errorBody()?.string()?.let { Json.decodeFromString<Error>(it) }
    }
}