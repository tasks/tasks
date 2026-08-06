package com.todoroo.astrid.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.preferences.Preferences
import java.util.concurrent.TimeUnit

class Upgrade_15_10_Test {
    private val preferences: Preferences = mock()
    private val upgrade = Upgrade_15_10(preferences)

    private val legacyKey = "notif_default_reminder"

    private val existingDefaults = listOf(
        Alarm(time = 0, type = TYPE_REL_START),
        Alarm(time = 0, type = TYPE_REL_END),
    )

    private fun stubPreferences(legacyHours: Int) {
        preferences.stub {
            onBlocking { defaultAlarms() } doReturn existingDefaults
            on { getIntegerFromString(legacyKey, 0) } doReturn legacyHours
        }
    }

    @Test
    fun configuredIntervalJoinsTheDefaultAlarms() = runTest {
        stubPreferences(legacyHours = 24)

        upgrade.migrateRandomReminder()

        verify(preferences).setDefaultAlarms(
            existingDefaults + Alarm(time = TimeUnit.DAYS.toMillis(1), type = TYPE_RANDOM)
        )
    }

    @Test
    fun existingDefaultsArePreserved() = runTest {
        stubPreferences(legacyHours = 1)

        upgrade.migrateRandomReminder()

        verify(preferences).setDefaultAlarms(
            check {
                assertEquals(existingDefaults, it.dropLast(1))
                assertEquals(TYPE_RANDOM, it.last().type)
            }
        )
    }

    @Test
    fun disabledRandomReminderWritesNothing() = runTest {
        stubPreferences(legacyHours = 0)

        upgrade.migrateRandomReminder()

        verify(preferences, never()).setDefaultAlarms(any())
    }

    @Test
    fun disabledRandomReminderMigratesToNothing() {
        assertNull(Upgrade_15_10.randomAlarm(0))
    }

    @Test
    fun negativeHoursMigrateToNothing() {
        assertNull(Upgrade_15_10.randomAlarm(-1))
    }

    @Test
    fun hourlyBecomesAnHourLongPeriod() {
        val alarm = Upgrade_15_10.randomAlarm(1)!!
        assertEquals(TYPE_RANDOM, alarm.type)
        assertEquals(TimeUnit.HOURS.toMillis(1), alarm.time)
    }

    @Test
    fun dailyBecomesADayLongPeriod() {
        assertEquals(TimeUnit.DAYS.toMillis(1), Upgrade_15_10.randomAlarm(24)!!.time)
    }

    @Test
    fun weeklyBecomesAWeekLongPeriod() {
        assertEquals(TimeUnit.DAYS.toMillis(7), Upgrade_15_10.randomAlarm(168)!!.time)
    }

    @Test
    fun biWeeklyBecomesATwoWeekPeriod() {
        assertEquals(TimeUnit.DAYS.toMillis(14), Upgrade_15_10.randomAlarm(336)!!.time)
    }

    @Test
    fun migratedAlarmIsUnowned() {
        val alarm = Upgrade_15_10.randomAlarm(24)!!
        assertEquals(0L, alarm.id)
        assertEquals(0L, alarm.task)
    }
}
