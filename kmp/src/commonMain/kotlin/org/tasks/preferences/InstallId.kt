package org.tasks.preferences

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InstallId(private val preferences: TasksPreferences) {
    suspend fun get(): String {
        preferences.get(TasksPreferences.installId, "").takeIf { it.isNotBlank() }?.let { return it }
        return preferences.getOrCreate(TasksPreferences.installId, seed())
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun seed(): String = preferences
        .get(TasksPreferences.pushFallbackToken, "")
        .takeIf { it.isNotBlank() }
        ?: Uuid.random().toString()
}
