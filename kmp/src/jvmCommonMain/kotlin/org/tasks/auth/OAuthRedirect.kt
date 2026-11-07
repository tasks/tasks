package org.tasks.auth

import java.io.IOException
import java.net.URLDecoder

private const val PARAM_CODE = "code"
private const val PARAM_STATE = "state"
private const val PARAM_ERROR = "error"
private const val PARAM_ERROR_DESCRIPTION = "error_description"
private const val STATE_MISMATCH = "OAuth state mismatch"
private const val AUTHORIZATION_FAILED = "Authorization failed"

const val REDIRECT_TIMEOUT_MILLIS = 10 * 60 * 1000L

data class RedirectResult(val redirectUri: String, val params: Map<String, String>) {
    val code: String? get() = params[PARAM_CODE]
    val state: String? get() = params[PARAM_STATE]
    val error: String? get() = params[PARAM_ERROR]
    val errorDescription: String? get() = params[PARAM_ERROR_DESCRIPTION]

    fun failure(expectedState: String?): String? = when {
        code == null -> errorDescription ?: error ?: AUTHORIZATION_FAILED
        state != expectedState -> STATE_MISMATCH
        else -> null
    }

    fun authorizedCode(expectedState: String?): String {
        val failure = failure(expectedState)
        val code = code
        if (failure != null || code == null) {
            throw ConditionalAccess.devicePolicyException(error, errorDescription)
                ?: IOException(failure ?: AUTHORIZATION_FAILED)
        }
        return code
    }
}

interface OAuthRedirect {
    fun client(app: OAuthApp): RegisteredClient = app.desktop

    suspend fun await(
        app: OAuthApp,
        openUrl: (String) -> Unit,
        authUrl: (redirectUri: String) -> String,
        expectedState: () -> String?,
    ): RedirectResult

    fun reopen(openUrl: (String) -> Unit, authUrl: String) = openUrl(authUrl)
}

suspend fun OAuthRedirect.awaitAuthorization(
    provider: OAuthProvider,
    openUrl: (String) -> Unit,
    prepare: (redirectUri: String) -> Pair<OAuthConfig, String>,
): Pair<OAuthConfig, String> {
    var prepared: OAuthConfig? = null
    val result = await(
        app = provider,
        openUrl = openUrl,
        authUrl = { redirectUri ->
            val (config, authUrl) = prepare(redirectUri)
            prepared = config
            authUrl
        },
        expectedState = { prepared?.state },
    )
    val config = prepared ?: throw IOException("The OAuth config was never built")
    return config to result.authorizedCode(config.state)
}

internal fun String.redirectParams(): Map<String, String> =
    substringAfter("?", "")
        .split("&")
        .filter { it.contains("=") }
        .mapNotNull {
            val (key, value) = it.split("=", limit = 2)
            try {
                key to URLDecoder.decode(value, "UTF-8")
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        .toMap()
