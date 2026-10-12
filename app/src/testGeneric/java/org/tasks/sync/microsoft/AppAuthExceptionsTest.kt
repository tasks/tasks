package org.tasks.sync.microsoft

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationException.GeneralErrors
import net.openid.appauth.AuthorizationException.TokenRequestErrors
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException
import java.io.IOException

class AppAuthExceptionsTest {
    @Test
    fun offlineIsANetworkError() {
        val exception = AuthorizationException.fromTemplate(
            GeneralErrors.NETWORK_ERROR,
            IOException("Unable to resolve host \"login.windows.net\""),
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun expiredGrantNeedsSignIn() {
        val exception = AuthorizationException.fromTemplate(TokenRequestErrors.INVALID_GRANT, null)

        assertTrue(exception.toTokenException() is UnauthorizedException)
    }

    @Test
    fun otherTokenErrorsStayUnclassified() {
        val exception = AuthorizationException.fromTemplate(TokenRequestErrors.INVALID_CLIENT, null)

        assertSame(exception, exception.toTokenException())
    }
}
