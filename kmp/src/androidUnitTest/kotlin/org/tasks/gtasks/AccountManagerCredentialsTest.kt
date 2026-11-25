package org.tasks.gtasks

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.tasks.TasksScopes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.tasks.data.entity.CaldavAccount
import java.io.IOException

class AccountManagerCredentialsTest {

    private val googleAccountManager = mock<GoogleAccountManager>()
    private val credentials = AccountManagerCredentials(googleAccountManager)
    private val account = CaldavAccount(
        accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
        uuid = "someone@gmail.com",
        username = "someone@gmail.com",
    )

    private fun authorization(adapter: org.tasks.googleapis.CredentialsAdapter): String? {
        val request = NetHttpTransport().createRequestFactory()
            .buildGetRequest(GenericUrl("https://tasks.googleapis.com/tasks/v1/users/@me/lists"))
        adapter.initialize(request)
        return request.headers.authorization
    }

    @Test
    fun theTokenIsFetchedOnceAndSentAsABearer() = runBlocking {
        whenever(googleAccountManager.getAccessToken("someone@gmail.com", TasksScopes.TASKS))
            .doReturn("token")
        val adapter = credentials.adapter(account)

        adapter.checkToken()
        adapter.checkToken()

        assertEquals("Bearer token", authorization(adapter))
        verifyBlocking(googleAccountManager, times(1)) {
            getAccessToken("someone@gmail.com", TasksScopes.TASKS)
        }
    }

    @Test
    fun aRejectedTokenIsInvalidatedAndFetchedAgain() = runBlocking {
        whenever(googleAccountManager.getAccessToken("someone@gmail.com", TasksScopes.TASKS))
            .doReturnConsecutively(listOf("stale", "fresh"))
        val adapter = credentials.adapter(account)
        adapter.checkToken()

        assertTrue(adapter.invalidateToken())
        assertNull(authorization(adapter))
        adapter.checkToken()

        verify(googleAccountManager).invalidateToken("stale")
        assertEquals("Bearer fresh", authorization(adapter))
    }

    @Test
    fun noHeaderIsSentWithoutAToken() = runBlocking {
        val adapter = credentials.adapter(account)

        adapter.checkToken()

        assertNull(authorization(adapter))
    }

    @Test
    fun anAccountIsAccessibleOnlyWhileItIsOnTheDevice() {
        whenever(googleAccountManager.getAccount("someone@gmail.com"))
            .doReturn(mock<android.accounts.Account>())

        assertTrue(credentials.canAccess(account))
        assertFalse(credentials.canAccess(account.copy(username = "gone@gmail.com")))
    }

    @Test
    fun aFailedTokenFetchFailsTheRequestAndIsRetriedNextTime() = runBlocking {
        whenever(googleAccountManager.getAccessToken("someone@gmail.com", TasksScopes.TASKS))
            .thenThrow(GoogleAccountManager.TokenUnavailableException(IOException("NetworkError")))
            .thenReturn("token")
        val adapter = credentials.adapter(account)

        val thrown = runCatching { adapter.checkToken() }.exceptionOrNull()
        adapter.checkToken()

        assertTrue(thrown is IOException)
        assertEquals("Bearer token", authorization(adapter))
    }
}
