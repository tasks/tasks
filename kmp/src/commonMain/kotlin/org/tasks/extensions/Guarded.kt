package org.tasks.extensions

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException

suspend fun <T> guarded(
    tag: String,
    what: String,
    fallback: T,
    warnOnly: Boolean = false,
    onFailure: suspend (Throwable) -> Unit = {},
    block: suspend () -> T,
): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    if (warnOnly) {
        Logger.w(throwable = e, tag = tag) { what }
    } else {
        Logger.e(throwable = e, tag = tag) { what }
    }
    onFailure(e)
    fallback
}

inline fun closeQuietly(tag: String, what: String, block: () -> Unit) =
    step(tag, "close $what", block)

inline fun step(tag: String, what: String, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        Logger.w(throwable = e, tag = tag) { "Failed to $what" }
    }
}
