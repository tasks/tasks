package org.tasks.auth

const val GOOGLE_TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"
const val MICROSOFT_TASKS_SCOPE = "Tasks.ReadWrite"

private const val PROMPT = "prompt"
private const val SELECT_ACCOUNT = "select_account"
private const val GOOGLE_DISCOVERY_PATH = "/oauth/google-configuration"
private const val GOOGLE_IOS_CLIENT_ID = "363426363175-nar1o932el9fi4gogs0mgsq8160epu4t.apps.googleusercontent.com"
private const val GOOGLE_OPENID_CONFIGURATION = "https://accounts.google.com/.well-known/openid-configuration"
private const val MICROSOFT_CLIENT_ID = "9d4babd5-e7ba-4286-ba4b-17274495a901"

data class RegisteredClient(
    val discoveryPath: String = "",
    val clientId: String = "",
    val redirectUri: String? = null,
    val discoveryUrl: String? = null,
)

interface OAuthApp {
    val issuer: String
    val desktop: RegisteredClient
    val ios: RegisteredClient
    val android: RegisteredClient
}

enum class OAuthProvider(
    override val issuer: String,
    val scope: String,
    override val desktop: RegisteredClient,
    override val ios: RegisteredClient = desktop,
    override val android: RegisteredClient = ios,
    val extraAuthParams: Map<String, String> = emptyMap(),
    val discoveryUrl: String? = null,
    val serverCallbackPath: String? = null,
    val usesPkce: Boolean = true,
    val promptForAccount: Boolean = true,
    val loginHintParam: String? = "login_hint",
    val loginHintIsUsername: Boolean = false,
    val reauthPrompt: String? = null,
) : OAuthApp {
    GOOGLE(
        issuer = "google",
        scope = "openid email profile",
        desktop = RegisteredClient(
            discoveryPath = GOOGLE_DISCOVERY_PATH,
            clientId = "363426363175-eiks57t3m2er6df8orak7491s9colld2.apps.googleusercontent.com",
        ),
        ios = RegisteredClient(
            discoveryPath = GOOGLE_DISCOVERY_PATH,
            clientId = GOOGLE_IOS_CLIENT_ID,
        ),
    ),
    MICROSOFT(
        issuer = "microsoft",
        scope = "user.read $MICROSOFT_TASKS_SCOPE openid offline_access email profile",
        desktop = RegisteredClient(clientId = MICROSOFT_CLIENT_ID),
        ios = RegisteredClient(
            clientId = MICROSOFT_CLIENT_ID,
            redirectUri = "msauth.org.tasks://auth",
        ),
        android = RegisteredClient(
            clientId = MICROSOFT_CLIENT_ID,
            redirectUri = "msauth://org.tasks/8wnYBRqh5nnQgFzbIXfxXSs41xE%3D",
        ),
        discoveryUrl = "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration",
        reauthPrompt = "login",
    ),
    GITHUB(
        issuer = "github",
        scope = "none",
        desktop = RegisteredClient(discoveryPath = "/oauth/github-localhost-configuration"),
        ios = RegisteredClient(
            discoveryPath = "/oauth/github-configuration",
            clientId = "a50fdbf3e289a7fb2fc6",
            redirectUri = "org.tasks.github.a50fdbf3e289a7fb2fc6://oauth2redirect",
        ),
        loginHintParam = "login",
        loginHintIsUsername = true,
    ),
    APPLE(
        issuer = "apple",
        scope = "name email",
        desktop = RegisteredClient(discoveryPath = "/oauth/apple-configuration"),
        ios = RegisteredClient(
            discoveryPath = "/oauth/apple-configuration",
            clientId = "org.tasks",
        ),
        android = RegisteredClient(
            discoveryPath = "/oauth/apple-configuration",
            redirectUri = "org.tasks.apple://oauth2redirect",
        ),
        extraAuthParams = mapOf("response_mode" to "form_post"),
        serverCallbackPath = "/oauth/apple/callback",
        usesPkce = false,
        promptForAccount = false,
        loginHintParam = null,
    ),
    GOOGLE_TASKS(
        issuer = "google_tasks",
        scope = "$GOOGLE_TASKS_SCOPE openid email",
        desktop = RegisteredClient(discoveryPath = "/oauth/google-api-configuration"),
        ios = RegisteredClient(
            clientId = GOOGLE_IOS_CLIENT_ID,
            discoveryUrl = GOOGLE_OPENID_CONFIGURATION,
        ),
        android = RegisteredClient(discoveryPath = "/oauth/google-api-configuration"),
        extraAuthParams = mapOf(
            "access_type" to "offline",
            "prompt" to "consent",
        ),
    );

    fun authParams(loginHint: String? = null): Map<String, String> {
        val hint = loginHint
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { loginHintIsUsername && '@' in it }
            ?.let { value -> loginHintParam?.let { it to value } }
        val params = extraAuthParams + listOfNotNull(hint)
        val prompt = when {
            hint != null -> reauthPrompt
            promptForAccount -> SELECT_ACCOUNT
            else -> null
        } ?: return params
        return params + (PROMPT to listOfNotNull(extraAuthParams[PROMPT], prompt).joinToString(" "))
    }
}
