package org.tasks.auth

import org.tasks.data.entity.CaldavAccount
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksOrgProviderTest {

    @Test
    fun aGithubUsernameSignsInWithGithub() {
        assertEquals(OAuthProvider.GITHUB, account("github_1234").tasksOrgProvider)
    }

    @Test
    fun anAppleUsernameSignsInWithApple() {
        assertEquals(OAuthProvider.APPLE, account("apple_1234").tasksOrgProvider)
    }

    @Test
    fun aGoogleUsernameSignsInWithGoogle() {
        assertEquals(OAuthProvider.GOOGLE, account("google_1234").tasksOrgProvider)
    }

    @Test
    fun aLegacyUsernameSignsInWithGoogle() {
        assertEquals(OAuthProvider.GOOGLE, account("1234").tasksOrgProvider)
    }

    @Test
    fun withoutAChosenProviderTheExistingAccountChoosesProviderAndHint() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.GITHUB, "bob@example.com"),
            tasksOrgSignIn(bob, provider = null),
        )
    }

    @Test
    fun choosingTheExistingAccountsProviderAddsItsHint() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.GITHUB, "bob@example.com"),
            tasksOrgSignIn(bob, provider = OAuthProvider.GITHUB),
        )
    }

    @Test
    fun theExistingAccountOverridesAChosenProvider() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.GITHUB, "bob@example.com"),
            tasksOrgSignIn(bob, provider = OAuthProvider.APPLE),
        )
    }

    @Test
    fun theExistingAccountOverridesAChosenHint() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.GITHUB, "bob@example.com"),
            tasksOrgSignIn(bob, provider = OAuthProvider.GOOGLE, loginHint = "carol@example.com"),
        )
    }

    @Test
    fun withoutAnAccountTheRequestIsUnchanged() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.APPLE, "carol@example.com"),
            tasksOrgSignIn(existing = null, provider = OAuthProvider.APPLE, loginHint = "carol@example.com"),
        )
    }

    @Test
    fun withoutAnAccountOrChoiceThereIsNoProvider() {
        assertEquals(
            TasksOrgSignIn(null, null),
            tasksOrgSignIn(existing = null, provider = null),
        )
    }

    @Test
    fun aBlankNameGivesNoHint() {
        assertEquals(
            TasksOrgSignIn(OAuthProvider.GITHUB, null),
            tasksOrgSignIn(bob.copy(name = ""), provider = null),
        )
    }

    private val bob = CaldavAccount(
        accountType = CaldavAccount.TYPE_TASKS,
        username = "github_5678",
        name = "bob@example.com",
    )

    private fun account(username: String) =
        CaldavAccount(accountType = CaldavAccount.TYPE_TASKS, username = username)
}
