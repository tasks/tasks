package com.todoroo.astrid.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Task.Companion.NOTIFY_AFTER_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_START
import java.util.concurrent.TimeUnit

class Upgrade_14_11_Test {
    @Test
    fun noFlags() {
        assertTrue(Upgrade_14_11.fromLegacyFlags(0).isEmpty())
    }

    @Test
    fun notifyAtStart() {
        val alarms = Upgrade_14_11.fromLegacyFlags(NOTIFY_AT_START)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_REL_START, alarms[0].type)
        assertEquals(0L, alarms[0].time)
    }

    @Test
    fun notifyAtDeadline() {
        val alarms = Upgrade_14_11.fromLegacyFlags(NOTIFY_AT_DEADLINE)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_REL_END, alarms[0].type)
        assertEquals(0L, alarms[0].time)
        assertEquals(0, alarms[0].repeat)
    }

    @Test
    fun notifyAfterDeadline() {
        val alarms = Upgrade_14_11.fromLegacyFlags(NOTIFY_AFTER_DEADLINE)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_REL_END, alarms[0].type)
        assertEquals(TimeUnit.DAYS.toMillis(1), alarms[0].time)
        assertEquals(6, alarms[0].repeat)
        assertEquals(TimeUnit.DAYS.toMillis(1), alarms[0].interval)
    }

    @Test
    fun allFlags() {
        val flags = NOTIFY_AT_START or NOTIFY_AT_DEADLINE or NOTIFY_AFTER_DEADLINE
        val alarms = Upgrade_14_11.fromLegacyFlags(flags)
        assertEquals(3, alarms.size)
        assertEquals(TYPE_REL_START, alarms[0].type)
        assertEquals(TYPE_REL_END, alarms[1].type)
        assertEquals(0L, alarms[1].time)
        assertEquals(TYPE_REL_END, alarms[2].type)
        assertEquals(TimeUnit.DAYS.toMillis(1), alarms[2].time)
    }

    @Test
    fun taskIdAssigned() {
        val alarms = Upgrade_14_11.fromLegacyFlags(
            NOTIFY_AT_START or NOTIFY_AT_DEADLINE or NOTIFY_AFTER_DEADLINE,
            task = 42
        )
        alarms.forEach { assertEquals(42L, it.task) }
    }

    @Test
    fun unrelatedFlagsIgnored() {
        val alarms = Upgrade_14_11.fromLegacyFlags(0xFF and NOTIFY_AT_DEADLINE.inv())
        // Only NOTIFY_AT_START (32) and NOTIFY_AFTER_DEADLINE (4) match within 0xFF
        assertEquals(2, alarms.size)
    }

    @Test
    fun defaultTaskIdIsZero() {
        val alarms = Upgrade_14_11.fromLegacyFlags(NOTIFY_AT_DEADLINE)
        assertEquals(0L, alarms[0].task)
    }
}
