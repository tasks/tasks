package org.tasks.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@Serializable
data class OAuthTokenData(
    val accessToken: String,
    val refreshToken: String,
    val tokenEndpoint: String,
    val clientId: String,
    val expiresAt: Long,
) {
    fun serialize(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun deserialize(data: String): OAuthTokenData =
            json.decodeFromString(serializer(), data)
    }
}

fun OAuthResult.toOAuthTokenData(refreshToken: String): OAuthTokenData =
    OAuthTokenData(
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenEndpoint = tokenEndpoint ?: throw Exception("No token_endpoint in OAuth result"),
        clientId = clientId ?: throw Exception("No client_id in OAuth result"),
        expiresAt = expiresIn?.let { currentTimeMillis() + it * 1000 } ?: 0,
    )
