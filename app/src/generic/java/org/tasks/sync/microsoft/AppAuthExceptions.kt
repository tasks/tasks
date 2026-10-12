package org.tasks.sync.microsoft

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationException.GeneralErrors
import net.openid.appauth.AuthorizationException.TokenRequestErrors
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException

internal fun AuthorizationException.toTokenException(): Exception = when {
    matches(GeneralErrors.NETWORK_ERROR) -> ConnectionException(errorMessage, this)
    matches(TokenRequestErrors.INVALID_GRANT) -> UnauthorizedException(errorMessage, this)
    else -> this
}

private fun AuthorizationException.matches(other: AuthorizationException) =
    type == other.type && code == other.code

private val AuthorizationException.errorMessage: String?
    get() = errorDescription ?: error ?: message
