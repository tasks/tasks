package org.tasks.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService
import net.openid.appauth.browser.AnyBrowserMatcher
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class AuthorizationService @Inject constructor(
        @ApplicationContext context: Context,
        private val authStateManager: AuthStateManager,
        configuration: Configuration
) {
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

    suspend fun getFreshToken(): String? = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            authStateManager
                    .current
                    .performActionWithFreshTokens(authorizationService) { _, idToken, exception ->
                        if (exception == null) {
                            cont.resumeWith(Result.success(idToken))
                        } else {
                            cont.resumeWith(Result.failure(exception))
                        }
                    }
        }
    }

    fun signOut() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        val currentState = authStateManager.current
        val clearedState = AuthState(currentState.authorizationServiceConfiguration!!)
        if (currentState.lastRegistrationResponse != null) {
            clearedState.update(currentState.lastRegistrationResponse)
        }
        authStateManager.replace(clearedState)
    }
}