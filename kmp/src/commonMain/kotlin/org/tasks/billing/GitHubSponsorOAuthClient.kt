@file:OptIn(ExperimentalAtomicApi::class)

package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.PKCE
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class GitHubSponsorOAuthClient(
    private val verify: suspend (code: String, redirectUri: String) -> GitHubSponsorClient.VerifyResult,
    private val redirect: OAuthRedirect,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GitHubSponsorClient {
    private val logger = Logger.withTag(TAG)
    private val lock = Mutex()
    private var attempt: Deferred<GitHubSponsorClient.VerifyResult>? = null

    private val attemptUrl = AtomicReference<String?>(null)

    private val callerOpenUrl = AtomicReference<((String) -> Unit)?>(null)

    override suspend fun signIn(openUrl: (String) -> Unit): GitHubSponsorClient.VerifyResult {
        callerOpenUrl.store(openUrl)
        return try {
            val pending = lock.withLock {
                attempt
                    ?.takeIf { it.isActive }
                    ?.also { reopenBrowser(openUrl) }
                    ?.takeIf { it.isActive }
                    ?: run {
                        attemptUrl.store(null)
                        scope.async { awaitAuthorization() }.also { attempt = it }
                    }
            }
            pending.await()
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            logger.i { "GitHub sign-in ended without a result: ${e.message}" }
            GitHubSponsorClient.VerifyResult.Cancelled
        } catch (e: Exception) {
            logger.e(e) { "GitHub OAuth flow failed" }
            GitHubSponsorClient.VerifyResult.Failed
        } finally {
            callerOpenUrl.compareAndSet(openUrl, null)
        }
    }

    private suspend fun awaitAuthorization(): GitHubSponsorClient.VerifyResult {
        val state = PKCE.generateVerifier()
        var issuedUrl: String? = null
        val result = try {
            redirect.await(
                app = GitHubSponsorApp,
                openUrl = { url -> callerOrAbandoned()(url) },
                authUrl = { redirectUri ->
                    callerOrAbandoned()
                    authorizationUrl(redirectUri, state).also {
                        issuedUrl = it
                        attemptUrl.store(it)
                    }
                },
                expectedState = { state },
            )
        } finally {
            issuedUrl?.let { attemptUrl.compareAndSet(it, null) }
        }
        val failure = result.failure(state)
        val code = result.code
        if (failure != null || code == null) {
            logger.w { "GitHub authorization failed: $failure" }
            return GitHubSponsorClient.VerifyResult.Failed
        }
        return verify(code, result.redirectUri)
    }

    private fun callerOrAbandoned(): (String) -> Unit =
        callerOpenUrl.load() ?: throw IOException("The sign-in was abandoned")

    private fun reopenBrowser(openUrl: (String) -> Unit) {
        val url = attemptUrl.load() ?: return
        try {
            redirect.reopen(openUrl, url)
        } catch (e: Exception) {
            logger.w(e) { "Failed to reopen the browser" }
        }
    }

    private fun authorizationUrl(redirectUri: String, state: String) = buildString {
        append(GITHUB_AUTHORIZE_URL)
        append("?client_id=")
        append(redirect.client(GitHubSponsorApp).clientId.encodeURLParameter())
        append("&redirect_uri=")
        append(redirectUri.encodeURLParameter())
        append("&state=")
        append(state.encodeURLParameter())
        append("&prompt=select_account")
    }

    companion object {
        private const val TAG = "GitHubSponsorClient"
        private const val GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    }
}
