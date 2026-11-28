@file:OptIn(ExperimentalAtomicApi::class)

package org.tasks.auth

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.io.IOException
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class AuthSessionRedirect(
    private val authenticate: suspend (authUrl: String, callbackScheme: String) -> String,
) : OAuthRedirect {
    private class Session(val authUrl: String, val job: Job)

    private val session = AtomicReference<Session?>(null)

    override fun client(app: OAuthApp): RegisteredClient = app.ios

    override suspend fun await(
        app: OAuthApp,
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult {
        val redirectUri = client(app).redirectUri
            ?: throw IOException("${app.issuer} sign-in is not configured for this build")
        val url = authUrl(redirectUri)
        val current = Session(url, currentCoroutineContext().job)
        session.exchange(current)?.job?.cancel()
        try {
            val callback = authenticate(url, redirectUri.substringBefore(':'))
            return RedirectResult(redirectUri, callback.redirectParams())
        } finally {
            session.compareAndSet(current, null)
        }
    }

    override fun reopen(openUrl: (String) -> Unit, authUrl: String) {
        val stale = session.load()?.takeIf { it.authUrl == authUrl } ?: return
        if (session.compareAndSet(stale, null)) {
            stale.job.cancel()
        }
    }
}
