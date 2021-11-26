@file:Suppress("ClassName")

package com.todoroo.astrid.service

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.service.Upgrade_11_12_3.Companion.LEGACY_PREFERENCE
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.TestUtilities.newPreferences
import org.tasks.preferences.Preferences

@RunWith(AndroidJUnit4::class)
class Upgrade_11_12_3_Test {
    private lateinit var preferences: Preferences
    private lateinit var upgrader: Upgrade_11_12_3

    @Test
    fun migrateNoDefaultReminders() {
        preferences.setString(LEGACY_PREFERENCE, "0")
        upgrader.migrateDefaultReminderPreference()

        assertEquals(emptySet<String>(), preferences.defaultRemindersSet)
        assertEquals(0, preferences.defaultReminders)
    }

    @Test
    fun migrateWhenDue() {
        preferences.setString(LEGACY_PREFERENCE, "2")
        upgrader.migrateDefaultReminderPreference()

        assertEquals(setOf("2"), preferences.defaultRemindersSet)
        assertEquals(2, preferences.defaultReminders)
    }

    @Test
    fun migrateWhenOverdue() {
        preferences.setString(LEGACY_PREFERENCE, "4")
        upgrader.migrateDefaultReminderPreference()

        assertEquals(setOf("4"), preferences.defaultRemindersSet)
        assertEquals(4, preferences.defaultReminders)
    }

    @Test
    fun migrateWhenDueAndOverdue() {
        preferences.setString(LEGACY_PREFERENCE, "6")
        upgrader.migrateDefaultReminderPreference()

        assertEquals(setOf("2", "4"), preferences.defaultRemindersSet)
        assertEquals(6, preferences.defaultReminders)
    }

    @Before
    fun setUp() {
        preferences = newPreferences(ApplicationProvider.getApplicationContext())
        preferences.clear()
        upgrader = Upgrade_11_12_3(preferences)
    }
}