package org.tasks.auth

import android.net.Uri
import androidx.core.net.toUri

data class IdentityProvider(
    val name: String,
    val discoveryEndpoint: Uri,
    val clientId: String,
    val redirectUri: Uri,
    val scope: String
) {
    companion object {
        val MICROSOFT = IdentityProvider(
            "Microsoft",
            "https://login.microsoftonline.com/consumers/v2.0/.well-known/openid-configuration".toUri(),
            "9d4babd5-e7ba-4286-ba4b-17274495a901",
            "msauth://org.tasks/8wnYBRqh5nnQgFzbIXfxXSs41xE%3D".toUri(),
            "user.read Tasks.ReadWrite openid offline_access email"
        )
    }
}