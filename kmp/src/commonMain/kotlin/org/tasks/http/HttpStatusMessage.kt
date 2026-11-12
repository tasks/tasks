package org.tasks.http

import io.ktor.http.HttpStatusCode

internal fun httpStatusMessage(statusCode: Int): String =
    HttpStatusCode
        .fromValue(statusCode)
        .description
        .takeIf { it.isNotBlank() && it != UNKNOWN_STATUS }
        ?.let { "HTTP $statusCode - $it" }
        ?: "HTTP $statusCode"

private const val UNKNOWN_STATUS = "Unknown Status Code"
