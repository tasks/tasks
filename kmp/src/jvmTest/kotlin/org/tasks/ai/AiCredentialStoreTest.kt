package org.tasks.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.preferences.TasksPreferences

class AiCredentialStoreTest {

    private val preferences = TasksPreferences(InMemoryDataStore())
    private val store = AiCredentialStore(preferences, testEncryption())

    @Test
    fun noKeyByDefault() = runBlocking {
        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey())
    }

    @Test
    fun roundTripsApiKey() = runBlocking {
        store.setApiKey("sk-or-v1-secret")

        assertEquals("sk-or-v1-secret", store.getApiKey())
        assertTrue(store.hasApiKey())
    }

    @Test
    fun storesCiphertextNotPlaintext() = runBlocking {
        store.setApiKey("sk-or-v1-secret")

        val stored = preferences.get(TasksPreferences.aiApiKey, "")
        assertTrue(stored.isNotBlank())
        assertNotEquals("sk-or-v1-secret", stored)
        assertFalse(stored.contains("secret"))
    }

    @Test
    fun trimsWhitespaceBeforeStoring() = runBlocking {
        store.setApiKey("  sk-or-v1-secret \n")

        assertEquals("sk-or-v1-secret", store.getApiKey())
    }

    @Test
    fun blankKeyClearsStoredKey() = runBlocking {
        store.setApiKey("sk-or-v1-secret")
        store.setApiKey("   ")

        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey())
    }

    @Test
    fun corruptCiphertextReadsAsNoKey() = runBlocking {
        preferences.set(TasksPreferences.aiApiKey, "not-valid-base64-ciphertext")

        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey())
    }
}
