package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertTrue

class OAuthProviderScopeTest {

    @Test
    fun microsoftAsksForTheProfileSoTheIdTokenCarriesPreferredUsername() {
        assertTrue("profile" in OAuthProvider.MICROSOFT.scope.split(' '))
    }
}
