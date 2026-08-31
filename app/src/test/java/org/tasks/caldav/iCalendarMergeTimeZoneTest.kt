@file:Suppress("ClassName")

package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.withTZ
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask
import org.tasks.makers.iCalMaker.DUE_DATE
import org.tasks.makers.iCalMaker.START_DATE
import org.tasks.makers.iCalMaker.newIcal
import org.tasks.time.DateTime
import java.util.TimeZone

class iCalendarMergeTimeZoneTest {
    @Test
    fun remoteUpdatesDueDateAfterChangingTimeZone() {
        lateinit var task: org.tasks.data.entity.Task
        lateinit var base: Task

        withTZ(NEW_YORK) {
            val aug30 = DateTime(2026, 8, 30, 0, 0)
            task = newTask(with(TaskMaker.DUE_DATE, aug30))
            base = newIcal(with(DUE_DATE, aug30))
        }

        withTZ(CHICAGO) {
            task.applyRemote(
                remote = newIcal(with(DUE_DATE, DateTime(2026, 8, 31, 0, 0))),
                local = base,
            )

            assertEquals(DateTime(2026, 8, 31, 12, 0).millis, task.dueDate)
        }
    }

    @Test
    fun remoteUpdatesDueDateSameTimeZone() {
        withTZ(CHICAGO) {
            val aug30 = DateTime(2026, 8, 30, 0, 0)
            val task = newTask(with(TaskMaker.DUE_DATE, aug30))

            task.applyRemote(
                remote = newIcal(with(DUE_DATE, DateTime(2026, 8, 31, 0, 0))),
                local = newIcal(with(DUE_DATE, aug30)),
            )

            assertEquals(DateTime(2026, 8, 31, 12, 0).millis, task.dueDate)
        }
    }

    @Test
    fun remoteUpdatesStartDateAfterChangingTimeZone() {
        lateinit var task: org.tasks.data.entity.Task
        lateinit var base: Task

        withTZ(NEW_YORK) {
            val aug30 = DateTime(2026, 8, 30, 0, 0)
            task = newTask(with(TaskMaker.START_DATE, aug30))
            base = newIcal(with(START_DATE, aug30))
        }

        withTZ(CHICAGO) {
            task.applyRemote(
                remote = newIcal(with(START_DATE, DateTime(2026, 8, 31, 0, 0))),
                local = base,
            )

            assertEquals(DateTime(2026, 8, 31, 0, 0).millis, task.hideUntil)
        }
    }

    @Test
    fun remoteUpdatesStartDateSameTimeZone() {
        withTZ(CHICAGO) {
            val aug30 = DateTime(2026, 8, 30, 0, 0)
            val task = newTask(with(TaskMaker.START_DATE, aug30))

            task.applyRemote(
                remote = newIcal(with(START_DATE, DateTime(2026, 8, 31, 0, 0))),
                local = newIcal(with(START_DATE, aug30)),
            )

            assertEquals(DateTime(2026, 8, 31, 0, 0).millis, task.hideUntil)
        }
    }

    @Test
    fun localStartDateEditIsNotOverwritten() {
        withTZ(CHICAGO) {
            val task = newTask(with(TaskMaker.START_DATE, DateTime(2026, 8, 30, 0, 0)))

            task.applyRemote(
                remote = newIcal(with(START_DATE, DateTime(2026, 9, 1, 0, 0))),
                local = newIcal(with(START_DATE, DateTime(2026, 8, 29, 0, 0))),
            )

            assertEquals(DateTime(2026, 8, 30, 0, 0).millis, task.hideUntil)
        }
    }

    companion object {
        private val NEW_YORK: TimeZone = TimeZone.getTimeZone("America/New_York")
        private val CHICAGO: TimeZone = TimeZone.getTimeZone("America/Chicago")
    }
}
