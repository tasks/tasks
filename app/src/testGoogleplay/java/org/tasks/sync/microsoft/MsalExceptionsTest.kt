package org.tasks.sync.microsoft

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException
import java.net.UnknownHostException

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
    fun noNetworkIsANetworkError() {
        val exception = MsalClientException(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE)

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
