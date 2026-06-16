package org.tasks.auth

const val GOOGLE_TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"

enum class OAuthProvider(
    val issuer: String,
    val discoveryPath: String,
    val clientId: String,
    val scope: String,
    val extraAuthParams: Map<String, String> = emptyMap(),
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
