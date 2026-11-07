package org.tasks.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OAuthTokenRefreshErrorsTest {
    @Test
    fun offlineKeepsItsSubclassSoSyncReportsANetworkError() {
        for (offline in listOf(
            UnknownHostException("login.microsoftonline.com"),
            ConnectException("connection refused"),
            SocketTimeoutException("timed out"),
        )) {
            val thrown = runCatching {
                OAuthTokenRefresh.refreshOrThrowIO { throw offline }
            }.exceptionOrNull()

            assertSame(offline, thrown)
        }
    }

    @Test
    fun aRefusedRefreshBecomesAPlainIoFailure() {
        val thrown = runCatching {
            OAuthTokenRefresh.refreshOrThrowIO {
                throw IllegalStateException("Refresh failed: invalid_grant")
            }
        }.exceptionOrNull()

        assertTrue(thrown is IOException)
        assertEquals("Refresh failed: invalid_grant", thrown?.message)
        assertTrue(thrown?.cause is IllegalStateException)
    }
}
