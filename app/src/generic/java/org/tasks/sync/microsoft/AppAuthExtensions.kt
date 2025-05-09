package org.tasks.sync.microsoft

import android.content.Context
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import kotlin.coroutines.suspendCoroutine

suspend fun Context.requestTokenRefresh(state: AuthState) =
    requestToken(state.createTokenRefreshRequest())

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
