package org.tasks.ai

import org.tasks.preferences.TasksPreferences

/**
 * Bump whenever the disclosure text or the set of transmitted data materially changes;
 * raising it re-prompts every user, mirroring [TasksPreferences.acceptedTosVersion].
 */
const val AI_DISCLOSURE_VERSION = 1

interface OpenRouterClientProvider {
    /** Returns null if the feature is disabled, unconsented, or has no API key. */
    suspend fun getService(): OpenRouterService?

    /** Builds a throwaway client from [rawKey] to verify it, without reading stored state. */
    suspend fun validateKey(rawKey: String): List<ModelInfo>
}

class AiGate(
    private val tasksPreferences: TasksPreferences,
    private val credentials: AiCredentialStore,
) {
    suspend fun isEnabled(): Boolean =
        tasksPreferences.get(TasksPreferences.aiTaskCreationEnabled, false)

    suspend fun hasConsent(): Boolean =
        tasksPreferences.get(TasksPreferences.aiDisclosureVersion, 0) >= AI_DISCLOSURE_VERSION

    suspend fun acceptConsent() =
        tasksPreferences.set(TasksPreferences.aiDisclosureVersion, AI_DISCLOSURE_VERSION)

    suspend fun revokeConsent() =
        tasksPreferences.set(TasksPreferences.aiDisclosureVersion, 0)

    /** Every precondition for making a task-parsing network call. */
    suspend fun canCall(): Boolean = isEnabled() && hasConsent() && credentials.hasApiKey()
}
