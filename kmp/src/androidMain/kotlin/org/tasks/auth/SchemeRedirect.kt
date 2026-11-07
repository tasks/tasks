package org.tasks.auth

import android.content.Context
import co.touchlab.kermit.Logger
import org.tasks.kmp.R
import java.io.IOException

class SchemeRedirect(
    private val context: Context,
    private val googleScheme: String = context.getString(R.string.google_oauth_scheme),
) : OAuthRedirect {
    private val logger = Logger.withTag(TAG)

    override fun client(provider: OAuthProvider): RegisteredClient = when (provider) {
        OAuthProvider.GOOGLE -> provider.android.copy(
            clientId = GoogleOAuth.clientId(googleScheme),
            redirectUri = googleScheme.ifEmpty { null }?.let(GoogleOAuth::redirectUri),
        )
        else -> provider.android
    }

    override suspend fun await(
        provider: OAuthProvider,
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult {
        val redirectUri = client(provider).redirectUri
            ?: throw IOException("${provider.issuer} sign-in is not configured for this build")
        logger.d { "Waiting for ${provider.issuer} to redirect to $redirectUri" }
        return PendingRedirect.await(redirectUri, authUrl(redirectUri), expectedState) {
            OAuthRedirectActivity.start(context)
        }
    }

    companion object {
        private const val TAG = "SchemeRedirect"
    }
}
