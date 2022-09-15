package org.tasks.sync.microsoft

import com.google.gson.Gson
import retrofit2.Response

data class Error(
    val error: ErrorBody,
) {
    data class ErrorBody(
        val code: String,
        val message: String,
    )

    companion object {
        fun <T> Response<T>.toMicrosoftError()
            = errorBody()?.string()?.let { Gson().fromJson(it, Error::class.java) }
    }
}