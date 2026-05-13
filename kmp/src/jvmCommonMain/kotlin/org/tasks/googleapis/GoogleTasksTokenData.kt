package org.tasks.googleapis

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GoogleTasksTokenData(
    val accessToken: String,
    val refreshToken: String,
    val tokenEndpoint: String,
    val clientId: String,
    val expiresAt: Long,
) {
    fun serialize(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun deserialize(data: String): GoogleTasksTokenData =
            json.decodeFromString(serializer(), data)
    }
}
