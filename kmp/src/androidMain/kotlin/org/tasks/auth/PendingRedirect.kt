package org.tasks.auth

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RedirectSupersededException : CancellationException("Another sign-in was started")

internal enum class Delivery { DELIVERED, IGNORED, NOTHING_WAITING }

internal object PendingRedirect {
    private var waiting: CancellableContinuation<RedirectResult>? = null
    private var redirectUri: String? = null
    private var authUrl: String? = null
    private var expectedState: () -> String? = { null }

    suspend fun await(
        redirectUri: String,
        authUrl: String,
        expectedState: () -> String?,
        openBrowser: () -> Unit,
    ): RedirectResult = suspendCancellableCoroutine { continuation ->
        synchronized(this) {
            waiting?.cancel(RedirectSupersededException())
            waiting = continuation
            this.redirectUri = redirectUri
            this.authUrl = authUrl
            this.expectedState = expectedState
        }
        continuation.invokeOnCancellation {
            synchronized(this) {
                if (waiting === continuation) {
                    clear()
                }
            }
        }
        try {
            openBrowser()
        } catch (e: Throwable) {
            fail(e)
        }
    }

    fun pendingAuthUrl(): String? = synchronized(this) { authUrl }

    fun deliver(uri: String): Delivery {
        val (continuation, result) = synchronized(this) {
            val continuation = waiting ?: return Delivery.NOTHING_WAITING
            val redirectUri = this.redirectUri ?: return Delivery.NOTHING_WAITING
            val result = RedirectResult(redirectUri, uri.redirectParams())
            if (!uri.isRedirectTo(redirectUri) || !result.matches(expectedState())) {
                return Delivery.IGNORED
            }
            clear()
            continuation to result
        }
        if (!continuation.isActive) return Delivery.NOTHING_WAITING
        continuation.resume(result)
        return Delivery.DELIVERED
    }

    fun cancel(): Boolean = fail(CancellationException("Sign in cancelled"))

    fun fail(error: Throwable): Boolean {
        val continuation = synchronized(this) {
            val continuation = waiting ?: return false
            clear()
            continuation
        }
        if (!continuation.isActive) return false
        continuation.resumeWithException(error)
        return true
    }

    private fun String.isRedirectTo(redirectUri: String): Boolean =
        substringBefore('?').substringBefore('#').trimEnd('/') ==
                redirectUri.substringBefore('?').trimEnd('/')

    private fun RedirectResult.matches(expectedState: String?): Boolean =
        state == expectedState || (state == null && code == null)

    private fun clear() {
        waiting = null
        redirectUri = null
        authUrl = null
        expectedState = { null }
    }
}
