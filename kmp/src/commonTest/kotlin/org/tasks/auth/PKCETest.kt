package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PKCETest {
    @Test
    fun challengeMatchesRfc7636AppendixB() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", PKCE.generateChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test
    fun verifiersAreUnpaddedUrlSafeBase64OfThirtyTwoRandomBytes() {
        val verifier = PKCE.generateVerifier()
        assertEquals(43, verifier.length)
        assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(verifier, PKCE.generateVerifier())
    }

    @Test
    fun authUrlEncodesEveryParameter() {
        val config = OAuthConfig(
            authorizationEndpoint = "https://github.com/login/oauth/authorize",
            tokenEndpoint = "https://caldav.tasks.org/github_token",
            clientId = "abc",
            redirectUri = "org.tasks.github.abc://oauth2redirect",
            scope = "openid email",
        )
        assertEquals(
            "https://github.com/login/oauth/authorize?client_id=abc&redirect_uri=org.tasks.github.abc%3A%2F%2Foauth2redirect" +
                "&response_type=code&scope=openid+email&code_challenge=ch&code_challenge_method=S256&state=st+a&prompt=select_account",
            TasksOAuthClient().buildAuthUrl(config, "ch", "st a", mapOf("prompt" to "select_account")),
        )
    }
}
