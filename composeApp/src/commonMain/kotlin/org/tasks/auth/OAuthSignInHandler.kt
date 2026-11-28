package org.tasks.auth

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.compose.accounts.Platform

fun interface PlatformSignIn {
    suspend fun signIn(loginHint: String?, onAuthorized: () -> Unit)
}

class OAuthSignInHandler(
    private val signIn: OAuthSignIn,
    private val oauthPlatforms: Set<Platform>,
    private val platformSignIns: Map<Platform, PlatformSignIn> = emptyMap(),
    private val openUrl: (String) -> Unit = {},
    private val authHeader: suspend (Platform) -> String? = { null },
    private val afterAuthorized: () -> Unit = {},
) : SignInHandler {

    override suspend fun signIn(
        platform: Platform,
        provider: OAuthProvider?,
        loginHint: String?,
        onAuthorized: () -> Unit,
    ) {
        platformSignIns[platform]?.let {
            it.signIn(loginHint, onAuthorized)
            return
        }
        if (platform !in oauthPlatforms) {
            throw UnsupportedOperationException("$platform not supported")
        }
        val oauthProvider = provider ?: when (platform) {
            Platform.TASKS_ORG -> OAuthProvider.GOOGLE
            Platform.GOOGLE_TASKS -> OAuthProvider.GOOGLE_TASKS
            Platform.MICROSOFT -> OAuthProvider.MICROSOFT
            else -> throw UnsupportedOperationException("$platform not supported")
        }
        val result = signIn.authorize(
            provider = oauthProvider,
            openUrl = openUrl,
            loginHint = loginHint,
            authHeader = authHeader(platform),
        )
        onAuthorized()
        try {
            withContext(NonCancellable) {
                when (platform) {
                    Platform.GOOGLE_TASKS -> signIn.createGoogleTasksAccount(result)
                    Platform.MICROSOFT -> signIn.createMicrosoftAccount(result)
                    else -> signIn.createAccount(oauthProvider, result)
                }
            }
        } finally {
            afterAuthorized()
        }
    }
}
