package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.exception.HttpException

class CaldavHttpError(serverType: Int, cause: HttpException) : Exception(
    buildString {
        append("HTTP ${cause.statusCode} serverType=$serverType")
        cause.errors.takeIf { it.isNotEmpty() }?.let { errors ->
            append(" errors=")
            append(errors.joinToString(",") { it.name.name })
        }
    },
    cause,
)
