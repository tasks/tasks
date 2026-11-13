package org.tasks.auth

import org.tasks.auth.OAuthAuthorizer.Companion.redactLoginHint
import kotlin.test.Test
import kotlin.test.assertEquals

class OAuthAuthorizerTest {
    @Test
    fun loginHintIsRedacted() {
        assertEquals(
            "https://login.example/authorize?client_id=abc&login_hint=***&prompt=login",
            redactLoginHint(
                "https://login.example/authorize?client_id=abc&login_hint=alice%40outlook.com&prompt=login"
            ),
        )
    }

    @Test
    fun loginHintAtTheEndIsRedacted() {
        assertEquals(
            "https://login.example/authorize?client_id=abc&login_hint=***",
            redactLoginHint("https://login.example/authorize?client_id=abc&login_hint=alice%40outlook.com"),
        )
    }

    @Test
    fun gitHubLoginIsRedacted() {
        assertEquals(
            "https://github.com/login/oauth/authorize?client_id=abc&login=***&state=xyz",
            redactLoginHint("https://github.com/login/oauth/authorize?client_id=abc&login=octocat&state=xyz"),
        )
    }

    @Test
    fun urlWithoutLoginHintIsUnchanged() {
        val url = "https://login.example/authorize?client_id=abc&state=xyz"
        assertEquals(url, redactLoginHint(url))
    }
}
