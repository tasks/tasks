package org.tasks.auth

import org.junit.Assert.assertEquals
import org.junit.Test

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
    fun additionalParamsKeepTheProvidersOwn() {
        assertEquals(
            mapOf(
                "response_mode" to "form_post",
                "login_hint" to "user@example.com",
            ),
            OAuthProvider.APPLE.authParams(mapOf("login_hint" to "user@example.com")),
        )
        assertEquals(
            mapOf(
                "access_type" to "offline",
                "prompt" to "consent select_account",
                "login_hint" to "user@example.com",
            ),
            OAuthProvider.GOOGLE_TASKS.authParams(mapOf("login_hint" to "user@example.com")),
        )
    }

    @Test
    fun aPromptFromTheCallerReplacesTheAccountChooser() {
        assertEquals(
            mapOf("prompt" to "login", "login_hint" to "user@example.com"),
            OAuthProvider.MICROSOFT.authParams(
                mapOf("prompt" to "login", "login_hint" to "user@example.com"),
            ),
        )
    }
}
