package org.tasks.ai

import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption

/**
 * The only code that touches the stored OpenRouter API key ciphertext.
 */
class AiCredentialStore(
    private val tasksPreferences: TasksPreferences,
    private val encryption: KeyStoreEncryption,
) {
    suspend fun getApiKey(): String? =
        tasksPreferences.get(TasksPreferences.aiApiKey, "")
            .takeIf { it.isNotBlank() }
            ?.let { encryption.decrypt(it) }
            ?.takeIf { it.isNotBlank() }

    suspend fun setApiKey(key: String) {
        if (key.isBlank()) {
            tasksPreferences.set(TasksPreferences.aiApiKey, "")
        } else {
            encryption.encrypt(key.trim())
                ?.let { tasksPreferences.set(TasksPreferences.aiApiKey, it) }
        }
    }

    suspend fun hasApiKey(): Boolean = getApiKey() != null
}
