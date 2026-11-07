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

    override fun client(app: OAuthApp): RegisteredClient = when (app) {
        OAuthProvider.GOOGLE -> app.android.copy(
            clientId = GoogleOAuth.clientId(googleScheme),
            redirectUri = googleScheme.ifEmpty { null }?.let(GoogleOAuth::redirectUri),
        )
        else -> app.android
    }

    override suspend fun await(
        app: OAuthApp,
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult {
        val redirectUri = client(app).redirectUri
            ?: throw IOException("${app.issuer} sign-in is not configured for this build")
        logger.d { "Waiting for ${app.issuer} to redirect to $redirectUri" }
        context.rememberOriginTask()
        return PendingRedirect.await(redirectUri, authUrl(redirectUri), expectedState) {
            OAuthRedirectActivity.start(context)
        }
    }

    override fun reopen(openUrl: (String) -> Unit, authUrl: String) {
        if (PendingRedirect.pendingAuthUrl() == authUrl && !context.bringBackBrowser()) {
            OAuthRedirectActivity.start(context)
        }
    }

    companion object {
        private const val TAG = "SchemeRedirect"
    }
}
