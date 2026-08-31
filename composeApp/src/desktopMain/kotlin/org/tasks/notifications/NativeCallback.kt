package org.tasks.notifications

import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal const val CALLBACK_TIMEOUT_MS = 10_000L

internal class Answer<T>(val value: T)

internal suspend fun <T> awaiting(
    tag: String,
    what: String,
    timeoutMs: Long = CALLBACK_TIMEOUT_MS,
    call: ((T) -> Unit) -> Unit,
): Answer<T>? {
    val answer = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            val answered = AtomicBoolean(false)
            call { value ->
                if (answered.compareAndSet(false, true)) {
                    continuation.resume(Answer(value))
                }
            }
        }
    }
    if (answer == null) {
        Logger.w(tag = tag) { "$what did not answer within ${timeoutMs}ms" }
    }
    return answer
}
