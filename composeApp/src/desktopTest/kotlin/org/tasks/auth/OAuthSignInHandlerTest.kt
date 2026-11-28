package org.tasks.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.compose.accounts.Platform
import org.tasks.data.entity.CaldavAccount

class OAuthSignInHandlerTest {

    private val events = mutableListOf<String>()
    private val result = OAuthResult(accessToken = "access", tokenEndpoint = "https://idp.example/token", clientId = "client")

    private val signIn = object : OAuthSignIn {
        var failSetup = false

        override suspend fun authorize(
            provider: OAuthProvider,
            openUrl: (String) -> Unit,
            loginHint: String?,
            authHeader: String?,
        ): OAuthResult {
            events += "authorize $provider hint=$loginHint header=$authHeader"
            openUrl("https://idp.example/authorize")
            return result
        }

        override suspend fun createAccount(provider: OAuthProvider, result: OAuthResult): CaldavAccount {
            events += "tasks.org $provider"
            if (failSetup) throw IllegalStateException("setup failed")
            return CaldavAccount()
        }

        override suspend fun createGoogleTasksAccount(result: OAuthResult) {
            events += "google tasks"
        }

        override suspend fun createMicrosoftAccount(result: OAuthResult) {
            events += "microsoft"
        }
    }

    private fun handler(
        oauthPlatforms: Set<Platform> = setOf(Platform.TASKS_ORG, Platform.GOOGLE_TASKS, Platform.MICROSOFT),
        platformSignIns: Map<Platform, PlatformSignIn> = emptyMap(),
        authHeader: suspend (Platform) -> String? = { null },
    ) = OAuthSignInHandler(
        signIn = signIn,
        oauthPlatforms = oauthPlatforms,
        platformSignIns = platformSignIns,
        openUrl = { events += "open $it" },
        authHeader = authHeader,
        afterAuthorized = { events += "after" },
    )

    @Test
    fun tasksOrgDefaultsToGoogle() = runTest {
        handler().signIn(Platform.TASKS_ORG, onAuthorized = { events += "authorized" })

        assertEquals(
            listOf(
                "authorize GOOGLE hint=null header=null",
                "open https://idp.example/authorize",
                "authorized",
                "tasks.org GOOGLE",
                "after",
            ),
            events,
        )
    }

    @Test
    fun tasksOrgUsesTheChosenProviderAndHint() = runTest {
        handler().signIn(Platform.TASKS_ORG, provider = OAuthProvider.GITHUB, loginHint = "octocat")

        assertEquals("authorize GITHUB hint=octocat header=null", events.first())
        assertTrue("tasks.org GITHUB" in events)
    }

    @Test
    fun googleTasksSetsUpAGoogleTasksAccountWithThePlatformHeader() = runTest {
        handler(authHeader = { if (it == Platform.GOOGLE_TASKS) "Bearer jwt" else null })
            .signIn(Platform.GOOGLE_TASKS)

        assertEquals("authorize GOOGLE_TASKS hint=null header=Bearer jwt", events.first())
        assertTrue("google tasks" in events)
    }

    @Test
    fun microsoftSetsUpAMicrosoftAccount() = runTest {
        handler().signIn(Platform.MICROSOFT, loginHint = "user@example.com")

        assertEquals("authorize MICROSOFT hint=user@example.com header=null", events.first())
        assertTrue("microsoft" in events)
    }

    @Test
    fun aPlatformSignInReplacesTheOAuthFlow() = runTest {
        handler(
            oauthPlatforms = setOf(Platform.TASKS_ORG),
            platformSignIns = mapOf(
                Platform.GOOGLE_TASKS to PlatformSignIn { hint, onAuthorized ->
                    events += "device picker hint=$hint"
                    onAuthorized()
                },
            ),
        ).signIn(Platform.GOOGLE_TASKS, loginHint = "me@gmail.com", onAuthorized = { events += "authorized" })

        assertEquals(listOf("device picker hint=me@gmail.com", "authorized"), events)
    }

    @Test
    fun anUnsupportedPlatformIsRejectedBeforeAuthorizing() = runTest {
        val failure = runCatching {
            handler(oauthPlatforms = setOf(Platform.TASKS_ORG, Platform.GOOGLE_TASKS)).signIn(Platform.MICROSOFT)
        }.exceptionOrNull()

        assertTrue(failure is UnsupportedOperationException)
        assertTrue(events.isEmpty())
    }

    @Test
    fun aFailedSetupStillRunsTheAfterHook() = runTest {
        signIn.failSetup = true

        val failure = runCatching { handler().signIn(Platform.TASKS_ORG) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("after", events.last())
    }
}
