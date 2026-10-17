package org.tasks.http

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.fromHttpToGmtDate
import org.tasks.time.DateTimeUtils2.currentTimeMillis

val HttpResponse.retryAfterMillis: Long?
    get() = headers[HttpHeaders.RetryAfter]?.retryAfterMillis()

fun String.retryAfterMillis(now: Long = currentTimeMillis()): Long? {
    val value = trim()
    val millis = value.toLongOrNull()?.times(1000)
        ?: try {
            value.fromHttpToGmtDate().timestamp - now
        } catch (_: Exception) {
            null
        }
    return millis?.takeIf { it > 0 }
}
