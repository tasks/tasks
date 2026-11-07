package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tasks.auth.LoopbackRedirect
import java.security.SecureRandom
import java.util.Base64

class LoopbackGitHubSponsorClient(
    private val verify: suspend (code: String, redirectUri: String) -> GitHubSponsorClient.VerifyResult,
    private val openUrl: (String) -> Unit,
    private val redirect: LoopbackRedirect = LoopbackRedirect(
        path = LOOPBACK_CALLBACK_PATH,
        closeBrowserTab = false,
    ),
    private val clientId: String = GITHUB_CLIENT_ID,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GitHubSponsorClient {
    private val logger = Logger.withTag(TAG)
    private val lock = Mutex()
    private var attempt: Deferred<GitHubSponsorClient.VerifyResult>? = null

    @Volatile
    private var attemptUrl: String? = null

    override suspend fun signIn(): GitHubSponsorClient.VerifyResult {
        val pending = lock.withLock {
            attempt?.takeIf { it.isActive }?.also { reopenBrowser() }
                ?: run {
                    attemptUrl = null
                    scope.async { awaitAuthorization() }.also { attempt = it }
                }
        }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "GitHub OAuth flow failed" }
            GitHubSponsorClient.VerifyResult.Failed
        }
    }

    private suspend fun awaitAuthorization(): GitHubSponsorClient.VerifyResult {
        val state = generateState()
        val result = redirect.await(
            openUrl = openUrl,
            authUrl = { redirectUri ->
                authorizationUrl(redirectUri, state).also { attemptUrl = it }
            },
            validate = { if (it.state == state) null else STATE_MISMATCH },
        )
        val code = result.code
        return when {
            code == null -> {
                logger.w { "GitHub authorization failed: ${result.error ?: "no code"}" }
                GitHubSponsorClient.VerifyResult.Failed
            }
            result.state != state -> {
                logger.w { "GitHub authorization came back with the wrong state" }
                GitHubSponsorClient.VerifyResult.Failed
            }
            else -> verify(code, result.redirectUri)
        }
    }

    private fun reopenBrowser() {
        val url = attemptUrl ?: return
        try {
            openUrl(url)
        } catch (e: Exception) {
            logger.w(e) { "Failed to reopen the browser" }
        }
    }

    private fun authorizationUrl(redirectUri: String, state: String) = buildString {
        append(GITHUB_AUTHORIZE_URL)
        append("?client_id=")
        append(clientId.encodeURLParameter())
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
        private const val GITHUB_CLIENT_ID = "Ov23limM74bOgBgCBrJ5"
        private const val STATE_BYTES = 32
        private const val STATE_MISMATCH = "OAuth state mismatch"
        const val LOOPBACK_CALLBACK_PATH = "/callback"
    }
}
