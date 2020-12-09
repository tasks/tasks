package org.tasks.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService
import net.openid.appauth.browser.AnyBrowserMatcher
import kotlin.coroutines.suspendCoroutine

class AuthorizationService constructor(
        val iss: String,
        context: Context,
        debugConnectionBuilder: DebugConnectionBuilder
) {
    val isGitHub = iss == ISS_GITHUB
    val authStateManager = AuthStateManager()
    val configuration = Configuration(
            context,
            when (iss) {
                ISS_GOOGLE -> Configuration.GOOGLE_CONFIG
                ISS_GITHUB -> Configuration.GITHUB_CONFIG
                else -> throw IllegalArgumentException()
            },
            debugConnectionBuilder
    )
    private val authorizationService = AuthorizationService(
            context,
            AppAuthConfiguration.Builder()
                    .setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
                    .setConnectionBuilder(configuration.connectionBuilder)
                    .build())

    fun dispose() {
        authorizationService.dispose()
    }

    fun getAuthorizationRequestIntent(
            request: AuthorizationRequest,
            customTabsIntent: CustomTabsIntent
    ): Intent {
        return authorizationService.getAuthorizationRequestIntent(request, customTabsIntent)
    }

    fun createCustomTabsIntent(uri: Uri, color: Int): CustomTabsIntent {
        return authorizationService
                .createCustomTabsIntentBuilder(uri)
                .setToolbarColor(color)
                .build()
    }

    fun performRegistrationRequest(
            request: RegistrationRequest,
            callback: (RegistrationResponse?, AuthorizationException?) -> Unit
    ) {
        authorizationService.performRegistrationRequest(request, callback)
    }

    suspend fun performTokenRequest(request: TokenRequest, clientAuthentication: ClientAuthentication): TokenResponse? {
        return withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                authorizationService.performTokenRequest(request, clientAuthentication) { response, exception ->
                    if (exception != null) {
                        cont.resumeWith(Result.failure(exception))
                    } else {
                        cont.resumeWith(Result.success(response))
                    }
                }
            }
        }
    }

    companion object {
        const val ISS_GOOGLE = "google"
        const val ISS_GITHUB = "github"
    }
}