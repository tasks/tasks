package org.tasks.auth

const val GOOGLE_TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"

enum class OAuthProvider(
    val issuer: String,
    val discoveryPath: String,
    val clientId: String,
    val scope: String,
    val extraAuthParams: Map<String, String> = emptyMap(),
    val discoveryUrl: String? = null,
    val loopbackHost: String = "127.0.0.1",
    val iosDiscoveryPath: String = discoveryPath,
    val iosClientId: String = clientId,
    val iosRedirectUri: String? = null,
) {
    GOOGLE(
        issuer = "google",
        discoveryPath = "/oauth/google-configuration",
        clientId = "363426363175-eiks57t3m2er6df8orak7491s9colld2.apps.googleusercontent.com",
        scope = "openid email profile",
        iosClientId = "363426363175-nar1o932el9fi4gogs0mgsq8160epu4t.apps.googleusercontent.com",
    ),
    MICROSOFT(
        issuer = "microsoft",
        discoveryPath = "",
        clientId = "9d4babd5-e7ba-4286-ba4b-17274495a901",
        scope = "user.read Tasks.ReadWrite openid offline_access email",
        discoveryUrl = "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration",
        iosRedirectUri = "msauth.org.tasks://auth",
    ),
    GITHUB(
        issuer = "github",
        discoveryPath = "/oauth/github-localhost-configuration",
        clientId = "",
        scope = "none",
        iosDiscoveryPath = "/oauth/github-configuration",
        iosClientId = "a50fdbf3e289a7fb2fc6",
        iosRedirectUri = "org.tasks.github.a50fdbf3e289a7fb2fc6://oauth2redirect",
    ),
    GOOGLE_TASKS(
        issuer = "google_tasks",
        discoveryPath = "/oauth/google-api-configuration",
        clientId = "",
        scope = "$GOOGLE_TASKS_SCOPE openid email",
        extraAuthParams = mapOf(
            "access_type" to "offline",
            "prompt" to "consent",
        ),
    ),
}
