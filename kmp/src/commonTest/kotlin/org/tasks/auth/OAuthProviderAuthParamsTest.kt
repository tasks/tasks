package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class OAuthProviderAuthParamsTest {

    @Test
    fun googleIsAskedToShowTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "select_account"),
            OAuthProvider.GOOGLE.authParams(),
        )
    }

    @Test
    fun microsoftIsAskedToShowTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "select_account"),
            OAuthProvider.MICROSOFT.authParams(),
        )
    }

    @Test
    fun gitHubIsAskedToShowTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "select_account"),
            OAuthProvider.GITHUB.authParams(),
        )
    }

    @Test
    fun theAccountChooserIsAddedToWhatTheProviderAlreadyAsksFor() {
        assertEquals(
            mapOf("access_type" to "offline", "prompt" to "consent select_account"),
            OAuthProvider.GOOGLE_TASKS.authParams(),
        )
    }

    @Test
    fun appleKeepsItsOwnParamsAndIgnoresAHint() {
        assertEquals(
            mapOf("response_mode" to "form_post"),
            OAuthProvider.APPLE.authParams(loginHint = "user@example.com"),
        )
    }

    @Test
    fun signingGoogleInAgainGoesStraightToTheHintedAccount() {
        assertEquals(
            mapOf("login_hint" to "user@example.com"),
            OAuthProvider.GOOGLE.authParams(loginHint = "user@example.com"),
        )
    }

    @Test
    fun signingGoogleTasksInAgainStillAsksForConsent() {
        assertEquals(
            mapOf(
                "access_type" to "offline",
                "prompt" to "consent",
                "login_hint" to "user@example.com",
            ),
            OAuthProvider.GOOGLE_TASKS.authParams(loginHint = "user@example.com"),
        )
    }

    @Test
    fun signingMicrosoftInAgainAsksForThatAccountsPassword() {
        assertEquals(
            mapOf("login_hint" to "user@example.com", "prompt" to "login"),
            OAuthProvider.MICROSOFT.authParams(loginHint = "user@example.com"),
        )
    }

    @Test
    fun gitHubTakesTheHintAsALogin() {
        assertEquals(
            mapOf("login" to "octocat"),
            OAuthProvider.GITHUB.authParams(loginHint = "octocat"),
        )
    }

    @Test
    fun gitHubIgnoresAnEmailHintAndShowsTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "select_account"),
            OAuthProvider.GITHUB.authParams(loginHint = "octocat@example.com"),
        )
    }

    @Test
    fun aBlankHintShowsTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "select_account"),
            OAuthProvider.GOOGLE.authParams(loginHint = " "),
        )
    }
}
