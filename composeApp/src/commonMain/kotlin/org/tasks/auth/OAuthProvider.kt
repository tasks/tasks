package org.tasks.auth

enum class OAuthProvider(
    val issuer: String,
    val discoveryPath: String,
    val clientId: String,
    val scope: String,
) {
    GOOGLE(
        issuer = "google",
        discoveryPath = "/oauth/google-configuration",
        clientId = "363426363175-eiks57t3m2er6df8orak7491s9colld2.apps.googleusercontent.com",
        scope = "openid email profile",
    ),
    GITHUB(
        issuer = "github",
        discoveryPath = "/oauth/github-localhost-configuration",
        clientId = "",
        scope = "none",
    ),
}
