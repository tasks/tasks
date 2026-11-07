package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.auth.OAuthApp
import org.tasks.auth.OAuthRedirect
import org.tasks.auth.RegisteredClient
import java.io.IOException
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

class GitHubSponsorOAuthClient(
    private val verify: suspend (code: String, redirectUri: String) -> GitHubSponsorClient.VerifyResult,
    private val redirect: OAuthRedirect,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GitHubSponsorClient {
    private val logger = Logger.withTag(TAG)
    private val lock = Mutex()
    private var attempt: Deferred<GitHubSponsorClient.VerifyResult>? = null

    @Volatile
    private var attemptUrl: String? = null

    private val callerOpenUrl = AtomicReference<((String) -> Unit)?>()

    override suspend fun signIn(openUrl: (String) -> Unit): GitHubSponsorClient.VerifyResult {
        callerOpenUrl.set(openUrl)
        return try {
            val pending = lock.withLock {
                attempt?.takeIf { it.isActive }?.also { reopenBrowser(openUrl) }
                    ?: run {
                        attemptUrl = null
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
        val state = generateState()
        val result = try {
            redirect.await(
                app = GitHubSponsorApp,
                openUrl = { url -> callerOrAbandoned()(url) },
                authUrl = { redirectUri ->
                    callerOrAbandoned()
                    authorizationUrl(redirectUri, state).also { attemptUrl = it }
                },
                expectedState = { state },
            )
        } finally {
            attemptUrl = null
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
        callerOpenUrl.get() ?: throw IOException("The sign-in was abandoned")

    private fun reopenBrowser(openUrl: (String) -> Unit) {
        val url = attemptUrl ?: return
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

    private fun generateState(): String {
        val bytes = ByteArray(STATE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TAG = "GitHubSponsorClient"
        private const val GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
        private const val STATE_BYTES = 32
    }
}

object GitHubSponsorApp : OAuthApp {
    private const val CLIENT_ID = "Ov23limM74bOgBgCBrJ5"
    override val issuer = "GitHub sponsor"
    const val LOOPBACK_PATH = "/callback"
    override val desktop = RegisteredClient(clientId = CLIENT_ID)
    override val ios = RegisteredClient(clientId = CLIENT_ID)
    override val android = RegisteredClient(
        clientId = CLIENT_ID,
        redirectUri = "org.tasks.github.sponsor://oauth2redirect",
    )
}
