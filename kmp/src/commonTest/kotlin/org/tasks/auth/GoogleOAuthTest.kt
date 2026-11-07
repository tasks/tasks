package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleOAuthTest {
    @Test
    fun theSchemeIsTheClientIdReversed() {
        assertEquals(
            "com.googleusercontent.apps.363426363175-rg39b1q2302l6mlkup40l4d6ids4osiv",
            GoogleOAuth.scheme(
                "363426363175-rg39b1q2302l6mlkup40l4d6ids4osiv.apps.googleusercontent.com"
            ),
        )
    }

    @Test
    fun theClientIdComesBackOutOfTheScheme() {
        assertEquals(
            "363426363175-rg39b1q2302l6mlkup40l4d6ids4osiv.apps.googleusercontent.com",
            GoogleOAuth.clientId(
                "com.googleusercontent.apps.363426363175-rg39b1q2302l6mlkup40l4d6ids4osiv"
            ),
        )
    }

    @Test
    fun theRedirectHasOneSlashAfterTheScheme() {
        assertEquals(
            "com.googleusercontent.apps.1-abc:/oauth2redirect",
            GoogleOAuth.redirectUri("com.googleusercontent.apps.1-abc"),
        )
    }
}
