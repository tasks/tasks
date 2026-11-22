package org.tasks.auth

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.http.ConnectionException
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_encryption_failed
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SignInErrorTest {
    @Test
    fun anInterruptedSignInSaysTheSignInFailed() = runBlocking {
        assertEquals(
            "Sign-in failed",
            SignInInterruptedException(CancellationException("Parent job is Cancelled")).signInError(),
        )
    }

    @Test
    fun anUnresolvableHostReadsAsANetworkErrorNotTheResolverMessage() = runBlocking {
        assertEquals(
            "Could not connect",
            UnknownHostException("Unable to resolve host \"caldav.tasks.org\": No address associated with hostname")
                .signInError(),
        )
    }

    @Test
    fun aRefusedConnectionReadsAsANetworkError() = runBlocking {
        assertEquals(
            "Could not connect",
            ConnectionException("Failed to connect to /1.2.3.4:443").signInError(),
        )
    }

    @Test
    fun aReadTimeoutSaysTheSignInFailedRatherThanBlamingTheConnection() = runBlocking {
        assertEquals("Sign-in failed", SocketTimeoutException("Read timed out").signInError())
    }

    @Test
    fun aConnectTimeoutReadsAsANetworkError() = runBlocking {
        assertEquals("Could not connect", ConnectTimeoutException("Connect timeout has expired").signInError())
    }

    @Test
    fun aWrappedUnresolvableHostReadsAsANetworkError() = runBlocking {
        assertEquals(
            "Could not connect",
            IOException(
                "Unable to resolve host \"caldav.tasks.org\"",
                UnknownHostException("Unable to resolve host \"caldav.tasks.org\""),
            ).signInError(),
        )
    }

    @Test
    fun aRefusedConnectionStraightFromTheSocketReadsAsANetworkError() = runBlocking {
        assertEquals("Could not connect", ConnectException("Connection refused").signInError())
    }

    @Test
    fun aKtorRequestTimeoutReadsAsANetworkError() = runBlocking {
        assertEquals(
            "Could not connect",
            HttpRequestTimeoutException("https://caldav.tasks.org/", 10_000).signInError(),
        )
    }

    @Test
    fun anIOExceptionFromTheSignInFlowKeepsItsMessage() = runBlocking {
        assertEquals(
            "Timed out waiting for the sign-in to finish",
            IOException("Timed out waiting for the sign-in to finish").signInError(),
        )
    }

    @Test
    fun anIOExceptionWithABlankMessageReadsAsANetworkError() = runBlocking {
        assertEquals("Could not connect", IOException(" ").signInError())
    }

    @Test
    fun anErrorWithABlankMessageSaysTheSignInFailed() = runBlocking {
        assertEquals("Sign-in failed", IllegalStateException("").signInError())
    }

    @Test
    fun anErrorWithoutAMessageSaysTheSignInFailed() = runBlocking {
        assertEquals("Sign-in failed", NullPointerException().signInError())
    }

    @Test
    fun aDisplayableExceptionShowsItsString() = runBlocking {
        assertEquals(
            "Encryption failed. Check that Tasks.org can access your system keychain, then try again.",
            DisplayableException(Res.string.error_encryption_failed).signInError(),
        )
    }
}
