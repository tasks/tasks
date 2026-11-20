package org.tasks.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.compose.accounts.Platform
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount

class TasksOrgAccountSignInHandlerTest {

    private val requests = mutableListOf<Request>()

    @Test
    fun anExistingTasksOrgAccountChoosesTheProviderAndHint() = runTest {
        handler(existing = githubAccount).signIn(Platform.TASKS_ORG)

        assertEquals(
            listOf(Request(Platform.TASKS_ORG, OAuthProvider.GITHUB, "bob@example.com")),
            requests,
        )
    }

    @Test
    fun anExistingTasksOrgAccountOverridesAChosenProvider() = runTest {
        handler(existing = githubAccount).signIn(Platform.TASKS_ORG, OAuthProvider.APPLE)

        assertEquals(
            listOf(Request(Platform.TASKS_ORG, OAuthProvider.GITHUB, "bob@example.com")),
            requests,
        )
    }

    @Test
    fun withoutATasksOrgAccountTheChosenProviderIsUsed() = runTest {
        handler(existing = null).signIn(Platform.TASKS_ORG, OAuthProvider.APPLE)

        assertEquals(listOf(Request(Platform.TASKS_ORG, OAuthProvider.APPLE, null)), requests)
    }

    @Test
    fun anExistingTasksOrgAccountOverridesAChosenHint() = runTest {
        handler(existing = githubAccount)
            .signIn(Platform.TASKS_ORG, OAuthProvider.GOOGLE, loginHint = "carol@example.com")

        assertEquals(
            listOf(Request(Platform.TASKS_ORG, OAuthProvider.GITHUB, "bob@example.com")),
            requests,
        )
    }

    @Test
    fun withoutATasksOrgAccountTheChosenHintIsUsed() = runTest {
        handler(existing = null)
            .signIn(Platform.TASKS_ORG, OAuthProvider.GOOGLE, loginHint = "carol@example.com")

        assertEquals(
            listOf(Request(Platform.TASKS_ORG, OAuthProvider.GOOGLE, "carol@example.com")),
            requests,
        )
    }

    @Test
    fun otherPlatformsAreLeftAlone() = runTest {
        handler(existing = githubAccount).signIn(Platform.MICROSOFT)

        assertEquals(listOf(Request(Platform.MICROSOFT, null, null)), requests)
    }

    private fun handler(existing: CaldavAccount?) = TasksOrgAccountSignInHandler(
        delegate = object : SignInHandler {
            override suspend fun signIn(
                platform: Platform,
                provider: OAuthProvider?,
                loginHint: String?,
                onAuthorized: () -> Unit,
            ) {
                requests += Request(platform, provider, loginHint)
            }
        },
        caldavDao = mock<CaldavDao> {
            onBlocking { getAccounts(any<List<Int>>()) } doReturn listOfNotNull(existing)
        },
    )

    private data class Request(
        val platform: Platform,
        val provider: OAuthProvider?,
        val loginHint: String?,
    )

    private val githubAccount = CaldavAccount(
        accountType = CaldavAccount.TYPE_TASKS,
        username = "github_5678",
        name = "bob@example.com",
    )
}
