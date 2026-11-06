package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.tasks.preferences.TasksPreferences

interface PlayPurchaseStore {
    suspend fun load(): List<PlayPurchase>

    suspend fun save(purchases: List<PlayPurchase>)
}

class JsonPlayPurchaseStore(
    private val tasksPreferences: TasksPreferences,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PlayPurchaseStore {
    private val logger = Logger.withTag("JsonPlayPurchaseStore")

    override suspend fun load(): List<PlayPurchase> {
        val stored = tasksPreferences.get(TasksPreferences.playPurchases, "")
        if (stored.isBlank()) {
            return emptyList()
        }
        return try {
            json.decodeFromString(stored)
        } catch (e: Exception) {
            logger.e(e) { "Failed to read stored purchases" }
            emptyList()
        }
    }

    override suspend fun save(purchases: List<PlayPurchase>) =
        tasksPreferences.set(TasksPreferences.playPurchases, json.encodeToString(purchases))
}
