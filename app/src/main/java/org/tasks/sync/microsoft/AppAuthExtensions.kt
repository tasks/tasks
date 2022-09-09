package org.tasks.sync.microsoft

import android.content.Context
import net.openid.appauth.*
import org.tasks.auth.IdentityProvider
import kotlin.coroutines.suspendCoroutine

suspend fun IdentityProvider.retrieveConfig(): AuthorizationServiceConfiguration {
    return suspendCoroutine { cont ->
        AuthorizationServiceConfiguration.fetchFromUrl(discoveryEndpoint) { serviceConfiguration, ex ->
            cont.resumeWith(
                when {
                    ex != null -> Result.failure(ex)
                    serviceConfiguration != null -> Result.success(serviceConfiguration)
                    else -> Result.failure(IllegalStateException())
                }
            )
        }
    }
}

suspend fun Context.requestTokenExchange(response: AuthorizationResponse) =
    requestToken(response.createTokenExchangeRequest())

private suspend fun Context.requestToken(tokenRequest: TokenRequest): Pair<TokenResponse?, AuthorizationException?> {
    val authService = AuthorizationService(this)
    return try {
        suspendCoroutine { cont ->
            authService.performTokenRequest(tokenRequest) { response, ex ->
                cont.resumeWith(Result.success(Pair(response, ex)))
            }
        }
    } finally {
        authService.dispose()
    }
}
