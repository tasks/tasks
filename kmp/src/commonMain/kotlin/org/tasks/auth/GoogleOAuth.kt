package org.tasks.auth

object GoogleOAuth {
    private const val SCHEME_PREFIX = "com.googleusercontent.apps."
    private const val CLIENT_ID_SUFFIX = ".apps.googleusercontent.com"
    private const val REDIRECT_PATH = ":/oauth2redirect"

    fun scheme(clientId: String): String = SCHEME_PREFIX + clientId.removeSuffix(CLIENT_ID_SUFFIX)

    fun clientId(scheme: String): String = scheme.removePrefix(SCHEME_PREFIX) + CLIENT_ID_SUFFIX

    fun redirectUri(scheme: String): String = scheme + REDIRECT_PATH
}
