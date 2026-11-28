package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleTasksRegisteredClientTest {

    @Test
    fun iosSignsInWithGoogleDirectlyUsingTheIosClient() {
        assertEquals(
            RegisteredClient(
                clientId = OAuthProvider.GOOGLE.ios.clientId,
                redirectUri = OAuthProvider.GOOGLE.ios.redirectUri,
                discoveryUrl = "https://accounts.google.com/.well-known/openid-configuration",
            ),
            OAuthProvider.GOOGLE_TASKS.ios,
        )
    }

    @Test
    fun desktopAndAndroidUseTheServerConfiguration() {
        val server = RegisteredClient(discoveryPath = "/oauth/google-api-configuration")

        assertEquals(server, OAuthProvider.GOOGLE_TASKS.desktop)
        assertEquals(server, OAuthProvider.GOOGLE_TASKS.android)
    }

    @Test
    fun onlyGoogleTasksBypassesTheServerOnIos() {
        assertEquals(
            listOf(OAuthProvider.GOOGLE_TASKS),
            OAuthProvider.entries.filter { it.ios.discoveryUrl != null },
        )
    }
}
