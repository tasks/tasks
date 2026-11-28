package org.tasks.billing

import org.tasks.auth.OAuthApp
import org.tasks.auth.RegisteredClient

object GitHubSponsorApp : OAuthApp {
    private const val CLIENT_ID = "Ov23limM74bOgBgCBrJ5"
    override val issuer = "GitHub sponsor"
    const val LOOPBACK_PATH = "/callback"
    private const val REDIRECT_URI = "org.tasks.github.sponsor://oauth2redirect"
    override val desktop = RegisteredClient(clientId = CLIENT_ID)
    override val ios = RegisteredClient(
        clientId = CLIENT_ID,
        redirectUri = REDIRECT_URI,
    )
    override val android = RegisteredClient(
        clientId = CLIENT_ID,
        redirectUri = REDIRECT_URI,
    )
}
