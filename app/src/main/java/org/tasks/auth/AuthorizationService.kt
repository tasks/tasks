package org.tasks.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.RegistrationRequest
import net.openid.appauth.RegistrationResponse
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.openid.appauth.browser.AnyBrowserMatcher
import kotlin.coroutines.suspendCoroutine

class AuthorizationService(
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
    ): Intent = authorizationService.getAuthorizationRequestIntent(request, customTabsIntent)

    fun createCustomTabsIntent(uri: Uri, color: Int): CustomTabsIntent =
            authorizationService
                .createCustomTabsIntentBuilder(uri)
                .setToolbarColor(color)
                .build()

    fun performRegistrationRequest(
            request: RegistrationRequest,
            callback: (RegistrationResponse?, AuthorizationException?) -> Unit
    ) {
        authorizationService.performRegistrationRequest(request, callback)
    }

    suspend fun performTokenRequest(request: TokenRequest, clientAuthentication: ClientAuthentication): TokenResponse? =
        withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                authorizationService.performTokenRequest(request, clientAuthentication) { response, exception ->
                    cont.resumeWith(
                            if (exception != null)
                                Result.failure(exception)
                            else
                                Result.success(response)
                    )
                }
            }
        }

    companion object {
        const val ISS_GOOGLE = "google"
        const val ISS_GITHUB = "github"
    }
}