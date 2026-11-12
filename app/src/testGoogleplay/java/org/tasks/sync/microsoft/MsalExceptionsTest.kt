package org.tasks.sync.microsoft

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.microsoft.identity.common.java.exception.ErrorStrings
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class MsalExceptionsTest {
    @Test
    fun offlineIsANetworkError() {
        val exception = MsalClientException(
            MsalClientException.IO_ERROR,
            "An IO error occurred in the network layer",
            UnknownHostException("Unable to resolve host \"login.windows.net\""),
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun timeoutIsANetworkError() {
        val exception = MsalClientException(
            MsalClientException.IO_ERROR,
            "An IO error occurred in the network layer",
            SocketTimeoutException("timeout"),
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun tlsFailureStaysUnclassified() {
        val exception = MsalClientException(
            MsalClientException.IO_ERROR,
            "An IO error occurred in the network layer",
            SSLHandshakeException("Chain validation failed"),
        )

        assertSame(exception, exception.toTokenException())
    }

    @Test
    fun noNetworkIsANetworkError() {
        val exception = MsalClientException(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE)

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun offlineBrokerIsANetworkError() {
        val exception = MsalClientException(
            "unknown_error",
            "An IO error occurred in the network layer: Unable to resolve host \"login.windows.net\": No address associated with hostname",
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun dozeModeIsANetworkError() {
        val exception = MsalClientException(
            ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION,
            "Connection is not available to refresh token because power optimization is enabled.",
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun unreachableBrokerIsANetworkError() {
        val exception = MsalClientException(
            MsalClientException.BROKER_BIND_FAILURE,
            "Unable to connect to the broker.",
        )

        assertTrue(exception.toTokenException() is ConnectionException)
    }

    @Test
    fun expiredGrantNeedsSignIn() {
        val exception = MsalUiRequiredException(
            MsalUiRequiredException.INVALID_GRANT,
            "AADSTS70000: The user could not be authenticated as the grant is expired.",
        )

        assertTrue(exception.toTokenException() is UnauthorizedException)
    }

    @Test
    fun redirectUriMismatchStaysUnclassified() {
        val exception = MsalClientException(
            MsalClientException.REDIRECT_URI_VALIDATION_ERROR,
            "The redirect URI doesn't match the uri generated with caller package name and signature",
        )

        assertSame(exception, exception.toTokenException())
    }
}
