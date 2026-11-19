package org.tasks.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

object AppAuthTokenMigration {

    fun toOAuthTokenData(serializedAuthState: String): OAuthTokenData? {
        val authState = runCatching { Json.parseToJsonElement(serializedAuthState) }
            .getOrNull() as? JsonObject
            ?: return null
        val tokenResponse = authState.child(LAST_TOKEN_RESPONSE)
        val tokenRequest = tokenResponse?.child(REQUEST)
        val tokenEndpoint = authState.child(CONFIG)?.string(TOKEN_ENDPOINT)
            ?: tokenRequest?.child(CONFIGURATION)?.string(TOKEN_ENDPOINT)
            ?: return null
        val clientId = tokenRequest?.string(CLIENT_ID)
            ?: authState.child(LAST_AUTHORIZATION_RESPONSE)?.child(REQUEST)?.string(CLIENT_ID)
            ?: return null
        val refreshToken = authState.string(REFRESH_TOKEN)
            ?: tokenResponse?.string(TOKEN_REFRESH_TOKEN)
        val accessToken = tokenResponse?.string(ACCESS_TOKEN)
        return OAuthTokenData(
            accessToken = accessToken.orEmpty(),
            refreshToken = refreshToken.orEmpty(),
            tokenEndpoint = tokenEndpoint,
            clientId = clientId,
            expiresAt = accessToken?.let { tokenResponse.long(EXPIRES_AT) } ?: 0,
        )
    }

    private fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

    private const val REFRESH_TOKEN = "refreshToken"
    private const val CONFIG = "config"
    private const val LAST_TOKEN_RESPONSE = "mLastTokenResponse"
    private const val LAST_AUTHORIZATION_RESPONSE = "lastAuthorizationResponse"
    private const val REQUEST = "request"
    private const val CONFIGURATION = "configuration"
    private const val TOKEN_ENDPOINT = "tokenEndpoint"
    private const val CLIENT_ID = "clientId"
    private const val ACCESS_TOKEN = "access_token"
    private const val TOKEN_REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
}
