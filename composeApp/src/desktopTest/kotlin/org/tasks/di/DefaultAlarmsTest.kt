package org.tasks.di

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.preferences.DEFAULT_ALARMS_JSON
import org.tasks.preferences.toAlarmJson
import org.tasks.preferences.toAlarms

class DefaultAlarmsTest {
    @Test
    fun defaultsMatchAndroid() {
        assertEquals(
            listOf(
                Alarm.whenStarted(0),
                Alarm.whenDue(0),
                Alarm.whenOverdue(0),
            ),
            DEFAULT_ALARMS_JSON.toAlarms(),
        )
    }

    @Test
    fun noneIsAnAnswer() {
        assertEquals(emptyList<Alarm>(), emptySet<String>().toAlarms())
    }

    @Test
    fun unreadableAlarmsAreSkipped() {
        val stored = setOf("not json", Json.encodeToString(Alarm.whenDue(0)))

        assertEquals(listOf(Alarm.whenDue(0)), stored.toAlarms())
    }

    @Test
    fun whatIsWrittenIsWhatComesBack() {
        val chosen = listOf(Alarm.whenDue(0), Alarm.whenOverdue(0))

        assertEquals(chosen, chosen.toAlarmJson().toAlarms())
    }

    @Test
    fun choosingNoneRoundTrips() {
        assertEquals(emptyList<Alarm>(), emptyList<Alarm>().toAlarmJson().toAlarms())
    }

    @Test
    fun theStoredFormIsTheOneAndroidWrites() {
        assertEquals(
            DEFAULT_ALARMS_JSON,
            listOf(Alarm.whenStarted(0), Alarm.whenDue(0), Alarm.whenOverdue(0)).toAlarmJson(),
        )
    }

    @Test
    fun alarmsAreSortedByTypeThenTime() {
        val stored = setOf(
            Json.encodeToString(Alarm.whenOverdue(0)),
            Json.encodeToString(Alarm.whenDue(0)),
            Json.encodeToString(Alarm.whenStarted(0)),
        )

        assertEquals(
            listOf(Alarm.whenStarted(0), Alarm.whenDue(0), Alarm.whenOverdue(0)),
            stored.toAlarms(),
        )
    }
}
