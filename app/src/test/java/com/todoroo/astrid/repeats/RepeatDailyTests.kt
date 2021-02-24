package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.withTZ
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import java.util.*

class RepeatDailyTests : RepeatTests() {
    @Test
    fun testRepeatDailyFromDueDate() {
        val task = newFromDue("FREQ=DAILY;INTERVAL=3", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 12, 30), next)
    }

    @Test
    fun testRepeatDailyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 30, 0, 25), next)
    }

    @Test
    fun testRepeatDailyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 30, 0, 4), next)
    }

    @Test
    fun allDayRepeatNewYork() = withTZ(NEW_YORK) {
        val task = newTask(
            with(DUE_DATE, newDay(2021, 2, 24)),
            with(RECUR, "FREQ=DAILY;INTERVAL=1")
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDay(2021, 2, 25), next)
    }

    @Test
    fun dueTimeRepeatNewYork() = withTZ(NEW_YORK) {
        val next = calculateNextDueDate(
            newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2021, 2, 24, 13, 30))
        )

        assertEquals(newDayTime(2021, 2, 25, 13, 30), next)
    }

    @Test
    fun allDayRepeatLondon() = withTZ(LONDON) {

        val next = calculateNextDueDate(
            newTask(
                with(DUE_DATE, newDay(2021, 2, 24)),
                with(RECUR, "FREQ=DAILY;INTERVAL=1")
            )
        )

        assertEquals(newDay(2021, 2, 25), next)
    }

    @Test
    fun dueTimeRepeatLondon() = withTZ(LONDON) {
        val next = calculateNextDueDate(
            newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2021, 2, 24, 13, 30))
        )

        assertEquals(newDayTime(2021, 2, 25, 13, 30), next)
    }

    @Test
    fun allDayRepeatBerlin() = withTZ(BERLIN) {
        val next = calculateNextDueDate(
            newTask(
                with(DUE_DATE, newDay(2021, 2, 24)),
                with(RECUR, "FREQ=DAILY;INTERVAL=1")
            )
        )

        assertEquals(newDay(2021, 2, 25), next)
    }

    @Test
    fun dueTimeRepeatBerlin() = withTZ(BERLIN) {
        val next = calculateNextDueDate(
            newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2021, 2, 24, 13, 30))
        )

        assertEquals(newDayTime(2021, 2, 25, 13, 30), next)
    }

    @Test
    fun allDayRepeatUtcPlus13() = withTZ(APIA) {
        val next = calculateNextDueDate(
            newTask(
                with(DUE_DATE, newDay(2021, 2, 24)),
                with(RECUR, "FREQ=DAILY;INTERVAL=1")
            )
        )

        assertEquals(newDay(2021, 2, 25), next)
    }

    @Test
    fun dueTimeRepeatUtcPlus13() = withTZ(APIA) {
        val next = calculateNextDueDate(
            newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2021, 2, 24, 13, 30))
        )

        assertEquals(newDayTime(2021, 2, 25, 13, 30), next)
    }

    companion object {
        private val BERLIN = TimeZone.getTimeZone("Europe/Berlin")
        private val LONDON = TimeZone.getTimeZone("Europe/London")
        private val NEW_YORK = TimeZone.getTimeZone("America/New_York")
        private val APIA = TimeZone.getTimeZone("Pacific/Apia")
    }
}