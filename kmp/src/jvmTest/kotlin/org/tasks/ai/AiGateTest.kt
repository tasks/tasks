package org.tasks.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.preferences.TasksPreferences

class AiGateTest {

    private val preferences = TasksPreferences(InMemoryDataStore())
    private val credentials = AiCredentialStore(preferences, testEncryption())
    private val gate = AiGate(preferences, credentials)

    private suspend fun configure(enabled: Boolean, consented: Boolean, keyed: Boolean) {
        preferences.set(TasksPreferences.aiTaskCreationEnabled, enabled)
        preferences.set(
            TasksPreferences.aiDisclosureVersion,
            if (consented) AI_DISCLOSURE_VERSION else 0,
        )
        credentials.setApiKey(if (keyed) "sk-or-v1-secret" else "")
    }

    @Test
    fun cannotCallWhenNothingConfigured() = runBlocking {
        assertFalse(gate.isEnabled())
        assertFalse(gate.hasConsent())
        assertFalse(gate.canCall())
    }

    @Test
    fun canCallOnlyWhenEnabledConsentedAndKeyed() = runBlocking {
        for (enabled in listOf(false, true)) {
            for (consented in listOf(false, true)) {
                for (keyed in listOf(false, true)) {
                    configure(enabled, consented, keyed)
                    assertEquals(
                        "enabled=$enabled consented=$consented keyed=$keyed",
                        enabled && consented && keyed,
                        gate.canCall(),
                    )
                }
            }
        }
    }

    @Test
    fun acceptConsentStoresCurrentDisclosureVersion() = runBlocking {
        gate.acceptConsent()

        assertTrue(gate.hasConsent())
        assertEquals(
            AI_DISCLOSURE_VERSION,
            preferences.get(TasksPreferences.aiDisclosureVersion, 0),
        )
    }

    @Test
    fun revokeConsentClearsIt() = runBlocking {
        gate.acceptConsent()
        gate.revokeConsent()

        assertFalse(gate.hasConsent())
    }

    @Test
    fun staleConsentVersionDoesNotCount() = runBlocking {
        configure(enabled = true, consented = true, keyed = true)
        preferences.set(TasksPreferences.aiDisclosureVersion, AI_DISCLOSURE_VERSION - 1)

        assertFalse(gate.hasConsent())
        assertFalse(gate.canCall())
    }

    @Test
    fun newerConsentVersionStillCounts() = runBlocking {
        configure(enabled = true, consented = true, keyed = true)
        preferences.set(TasksPreferences.aiDisclosureVersion, AI_DISCLOSURE_VERSION + 1)

        assertTrue(gate.hasConsent())
        assertTrue(gate.canCall())
    }
}
