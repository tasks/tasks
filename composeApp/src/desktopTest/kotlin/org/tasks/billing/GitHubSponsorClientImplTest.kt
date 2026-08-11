package org.tasks.billing

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory
import java.io.IOException

class GitHubSponsorClientImplTest {
    @Test(timeout = 30_000)
    fun aSignInThatCannotOpenABrowserFailsAtOnce() = runBlocking {
        val client = GitHubSponsorClientImpl(
            httpClientFactory = mock<OkHttpClientFactory>(),
            serverEnvironment = mock<TasksServerEnvironment>(),
            desktopEntitlement = mock<DesktopEntitlement>(),
            json = Json,
            openUrl = { throw IOException("nothing on this system could open it") },
        )

        assertEquals(GitHubSponsorClient.VerifyResult.Failed, client.signIn())
    }
}
