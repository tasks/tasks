@file:Suppress("ClassName")

package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import net.fortuna.ical4j.model.property.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.createDueDate
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.makers.iCalMaker
import org.tasks.makers.iCalMaker.COLLAPSED
import org.tasks.makers.iCalMaker.COMPLETED_AT
import org.tasks.makers.iCalMaker.CREATED_AT
import org.tasks.makers.iCalMaker.DESCRIPTION
import org.tasks.makers.iCalMaker.DUE_DATE
import org.tasks.makers.iCalMaker.PARENT
import org.tasks.makers.iCalMaker.PRIORITY
import org.tasks.makers.iCalMaker.RRULE
import org.tasks.makers.iCalMaker.START_DATE
import org.tasks.makers.iCalMaker.STATUS
import org.tasks.makers.iCalMaker.TITLE
import org.tasks.makers.iCalMaker.newIcal
import org.tasks.time.DateTime

class iCalendarMergeTest {
    @Test
    fun applyTitleNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(with(TITLE, "Title")),
                local = null
            )
            .let {
                assertEquals("Title", it.title)
            }

    @Test
    fun remoteUpdatedTitle() =
        newTask(with(TaskMaker.TITLE, "Title"))
            .applyRemote(
                remote = newIcal(with(TITLE, "Title2")),
                local = newIcal(with(TITLE, "Title")),
            )
            .let {
                assertEquals("Title2", it.title)
            }

    @Test
    fun localBeatsRemoteTitle() =
        newTask(with(TaskMaker.TITLE, "Title3"))
            .applyRemote(
                remote = newIcal(with(TITLE, "Title2")),
                local = newIcal(with(TITLE, "Title")),
            )
            .let {
                assertEquals("Title3", it.title)
            }

    @Test
    fun remoteRemovesTitle() =
        newTask(with(TaskMaker.TITLE, "Title"))
            .applyRemote(
                remote = newIcal(with(TITLE, null as String?)),
                local = newIcal(with(TITLE, "Title")),
            )
            .let {
                assertNull(it.title)
            }

    @Test
    fun localRemovesTitle() =
        newTask(with(TaskMaker.TITLE, null as String?))
            .applyRemote(
                remote = newIcal(with(TITLE, "Title")),
                local = newIcal(with(TITLE, "Title"))
            )
            .let {
                assertNull(it.title)
            }

    @Test
    fun applyNewDescription() =
        newTask()
            .applyRemote(
                remote = newIcal(with(DESCRIPTION, "Description")),
                local = null
            )
            .let {
                assertEquals("Description", it.notes)
            }

    @Test
    fun localBeatsRemoteDescription() =
        newTask(with(TaskMaker.DESCRIPTION, "Description3"))
            .applyRemote(
                remote = newIcal(with(DESCRIPTION, "Description2")),
                local = newIcal(with(DESCRIPTION, "Description"))
            )
            .let {
                assertEquals("Description3", it.notes)
            }

    @Test
    fun remoteUpdatesDescription() {
        newTask(with(TaskMaker.DESCRIPTION, "Description"))
            .applyRemote(
                remote = newIcal(with(DESCRIPTION, "Description2")),
                local = newIcal(with(DESCRIPTION, "Description"))
            )
            .let {
                assertEquals("Description2", it.notes)
            }
    }

    @Test
    fun localRemovedDescription() =
        newTask(with(TaskMaker.DESCRIPTION, null as String?))
            .applyRemote(
                remote = newIcal(with(DESCRIPTION, "Description")),
                local = newIcal(with(DESCRIPTION, "Description"))
            )
            .let {
                assertNull(it.notes)
            }

    @Test
    fun remoteRemovedDescription() =
        newTask(with(TaskMaker.DESCRIPTION, "Description"))
            .applyRemote(
                remote = newIcal(with(DESCRIPTION, null as String?)),
                local = newIcal(with(DESCRIPTION, "Description"))
            )
            .let {
                assertNull(it.notes)
            }

    @Test
    fun applyPriorityNewTask() =
        newTask(with(TaskMaker.PRIORITY, HIGH))
            .applyRemote(
                remote = newIcal(with(PRIORITY, 5)),
                local = null
            )
            .let {
                assertEquals(MEDIUM, it.priority)
            }

    @Test
    fun localUpdatedPriority() =
        newTask(with(TaskMaker.PRIORITY, LOW))
            .applyRemote(
                remote = newIcal(with(PRIORITY, 5)),
                local = newIcal(with(PRIORITY, 5))
            )
            .let {
                assertEquals(LOW, it.priority)
            }

    @Test
    fun remoteUpdatedPriority() =
        newTask(with(TaskMaker.PRIORITY, MEDIUM))
            .applyRemote(
                remote = newIcal(with(PRIORITY, 1)),
                local = newIcal(with(PRIORITY, 5))
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun localBeatsRemotePriority() =
        newTask(with(TaskMaker.PRIORITY, HIGH))
            .applyRemote(
                remote = newIcal(with(PRIORITY, 1)),
                local = newIcal(with(PRIORITY, 5))
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun dueDateNewTask() {
        val due = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(DUE_DATE, due)),
                local = null
            )
            .let {
                assertEquals(due.allDay(), it.dueDate)
            }
    }

    @Test
    fun remoteAddsDueDate() {
        val due = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(DUE_DATE, due)),
                local = newIcal()
            )
            .let {
                assertEquals(due.allDay(), it.dueDate)
            }
    }

    @Test
    fun remoteUpdatesDueDate() {
        val due = newDateTime()
        newTask(with(TaskMaker.DUE_DATE, due))
            .applyRemote(
                remote = newIcal(with(DUE_DATE, due.plusDays(1))),
                local = newIcal(with(DUE_DATE, due))
            )
            .let {
                assertEquals(due.plusDays(1).allDay(), it.dueDate)
            }
    }

    @Test
    fun remoteRemovesDueDate() {
        val due = newDateTime()
        newTask(with(TaskMaker.DUE_DATE, due))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(DUE_DATE, due))
            )
            .let {
                assertEquals(0, it.dueDate)
            }
    }

    @Test
    fun localRemovesDueDate() {
        val due = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(DUE_DATE, due)),
                local = newIcal(with(DUE_DATE, due))
            )
            .let {
                assertEquals(0, it.dueDate)
            }
    }

    @Test
    fun localBeatsRemoteDueDate() {
        val due = newDateTime()
        newTask(with(TaskMaker.DUE_DATE, due.plusDays(2)))
            .applyRemote(
                remote = newIcal(with(DUE_DATE, due.plusDays(1))),
                local = newIcal(with(DUE_DATE, due))
            )
            .let {
                assertEquals(due.plusDays(2).allDay(), it.dueDate)
            }
    }

    @Test
    fun startDateNewTask() {
        val start = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(START_DATE, start)),
                local = null
            )
            .let {
                assertEquals(start.startOfDay().millis, it.hideUntil)
            }
    }

    @Test
    fun remoteAddsStartDate() {
        val start = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(START_DATE, start)),
                local = newIcal()
            )
            .let {
                assertEquals(start.startOfDay().millis, it.hideUntil)
            }
    }

    @Test
    fun remoteUpdatesStartDate() {
        val start = newDateTime()
        newTask(with(TaskMaker.START_DATE, start))
            .applyRemote(
                remote = newIcal(with(START_DATE, start.plusDays(1))),
                local = newIcal(with(START_DATE, start))
            )
            .let {
                assertEquals(start.plusDays(1).startOfDay().millis, it.hideUntil)
            }
    }

    @Test
    fun remoteRemovesStartDate() {
        val start = newDateTime()
        newTask(with(TaskMaker.START_DATE, start))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(START_DATE, start))
            )
            .let {
                assertEquals(0, it.hideUntil)
            }
    }

    @Test
    fun localRemovesStartDate() {
        val start = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(START_DATE, start)),
                local = newIcal(with(START_DATE, start))
            )
            .let {
                assertEquals(0, it.hideUntil)
            }
    }

    @Test
    fun localBeatsRemoteStartDate() {
        val start = newDateTime()
        newTask(with(TaskMaker.START_DATE, start.plusDays(2)))
            .applyRemote(
                remote = newIcal(with(START_DATE, start.plusDays(1))),
                local = newIcal(with(START_DATE, start))
            )
            .let {
                assertEquals(start.plusDays(2).startOfDay().millis, it.hideUntil)
            }
    }

    @Test
    fun remoteAddsCreationDate() {
        val created = newDateTime()
        newTask(with(CREATION_TIME, created.minusMinutes(1)))
            .applyRemote(
                remote = newIcal(with(CREATED_AT, created.toUTC())),
                local = null
            )
            .let {
                assertEquals(created.millis, it.creationDate)
            }
    }

    @Test
    fun remoteSetsRecurrence() =
        newTask()
            .applyRemote(
                remote = newIcal(with(RRULE, "FREQ=DAILY")),
                local = null
            )
            .let {
                assertEquals("FREQ=DAILY", it.recurrence)
            }

    @Test
    fun remoteUpdatesRecurrence() =
        newTask(with(TaskMaker.RECUR, "FREQ=DAILY"))
            .applyRemote(
                remote = newIcal(with(RRULE, "FREQ=MONTHLY")),
                local = newIcal(with(RRULE, "FREQ=DAILY"))
            )
            .let {
                assertEquals("FREQ=MONTHLY", it.recurrence)
            }

    @Test
    fun remoteRemovesRecurrence() =
        newTask(with(TaskMaker.RECUR, "FREQ=DAILY"))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(RRULE, "FREQ=DAILY"))
            )
            .let {
                assertNull(it.recurrence)
            }

    @Test
    fun localRemovesRecurrence() =
        newTask()
            .applyRemote(
                remote = newIcal(with(RRULE, "FREQ=DAILY")),
                local = newIcal(with(RRULE, "FREQ=DAILY"))
            )
            .let {
                assertNull(it.recurrence)
            }

    @Test
    fun localBeatsRemoteRecurrence() =
        newTask(with(TaskMaker.RECUR, "FREQ=WEEKLY"))
            .applyRemote(
                remote = newIcal(with(RRULE, "FREQ=MONTHLY")),
                local = newIcal(with(RRULE, "FREQ=DAILY"))
            )
            .let {
                assertEquals("FREQ=WEEKLY", it.recurrence)
            }

    @Test
    fun remoteSetsCompletedStatus() =
        newTask()
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
                local = null
            )
            .let {
                assertTrue(it.isCompleted)
            }

    @Test
    fun remoteUpdatesCompletedStatus() =
        newTask()
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
                local = newIcal(with(STATUS, Status.VTODO_IN_PROCESS))
            )
            .let {
                assertTrue(it.isCompleted)
            }

    @Test
    fun remoteRemovesCompletedStatus() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(
                    with(STATUS, Status.VTODO_COMPLETED),
                    with(COMPLETED_AT, now)
                )
            )
            .let {
                assertFalse(it.isCompleted)
            }
    }

    @Test
    fun remoteSetsCompletedAt() {
        val now = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(COMPLETED_AT, now.toUTC())),
                local = null
            )
            .let {
                assertEquals(now.startOfSecond().millis, it.completionDate)
            }
    }

    @Test
    fun remoteUpdatesCompletedAt() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(with(COMPLETED_AT, now.plusMinutes(5).toUTC())),
                local = newIcal(
                    with(COMPLETED_AT, now.toUTC()),
                    with(STATUS, Status.VTODO_COMPLETED)
                )
            )
            .let {
                assertEquals(now.plusMinutes(5).startOfSecond().millis, it.completionDate)
            }
    }

    @Test
    fun remoteRemovesCompletedAt() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(
                    with(COMPLETED_AT, now.toUTC()),
                    with(STATUS, Status.VTODO_COMPLETED)
                )
            )
            .let {
                assertFalse(it.isCompleted)
            }
    }

    @Test
    fun localRemovesCompletedAt() {
        val now = newDateTime()
        newTask()
            .applyRemote(
                remote = newIcal(with(COMPLETED_AT, now.toUTC())),
                local = newIcal(
                    with(COMPLETED_AT, now.toUTC()),
                    with(STATUS, Status.VTODO_COMPLETED)
                )
            )
            .let {
                assertFalse(it.isCompleted)
            }
    }

    @Test
    fun localBeatsRemoteCompletedAt() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now.plusMinutes(2)))
            .applyRemote(
                remote = newIcal(with(COMPLETED_AT, now.plusMinutes(1).toUTC())),
                local = newIcal(
                    with(COMPLETED_AT, now.toUTC()),
                    with(STATUS, Status.VTODO_COMPLETED)
                )
            )
            .let {
                assertEquals(now.plusMinutes(2).millis, it.completionDate)
            }
    }

    @Test
    fun remoteSetsCollapsed() {
        newTask()
            .applyRemote(
                remote = newIcal(with(COLLAPSED, true)),
                local = null
            )
            .let {
                assertTrue(it.isCollapsed)
            }
    }

    @Test
    fun remoteRemovesCollapsed() {
        newTask(with(TaskMaker.COLLAPSED, true))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(COLLAPSED, true))
            )
            .let {
                assertFalse(it.isCollapsed)
            }
    }

    @Test
    fun localBeatsRemoteCollapsed() {
        newTask(with(TaskMaker.COLLAPSED, true))
            .applyRemote(
                remote = newIcal(with(COLLAPSED, false)),
                local = newIcal(with(COLLAPSED, false))
            )
            .let {
                assertTrue(it.isCollapsed)
            }
    }

    @Test
    fun remoteSetsOrder() =
        newTask()
            .applyRemote(
                remote = newIcal(with(iCalMaker.ORDER, 1234)),
                local = null
            )
            .let {
                assertEquals(1234L, it.order)
            }

    @Test
    fun remoteRemovesOrder() =
        newTask(with(TaskMaker.ORDER, 1234))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(iCalMaker.ORDER, 1234))
            )
            .let {
                assertNull(it.order)
            }

    @Test
    fun localRemovesOrder() =
        newTask()
            .applyRemote(
                remote = newIcal(with(iCalMaker.ORDER, 1234)),
                local = newIcal(with(iCalMaker.ORDER, 1234))
            )
            .let {
                assertNull(it.order)
            }

    @Test
    fun localBeatsRemoteOrder() =
        newTask(with(TaskMaker.ORDER, 789L))
            .applyRemote(
                remote = newIcal(with(iCalMaker.ORDER, 456L)),
                local = newIcal(with(iCalMaker.ORDER, 123))
            )
            .let {
                assertEquals(789L, it.order)
            }

    @Test
    fun remoteSetsParent() =
        newCaldavTask()
            .applyRemote(
                remote = newIcal(with(PARENT, "1234")),
                local = null
            )
            .let {
                assertEquals("1234", it.remoteParent)
            }

    @Test
    fun remoteRemovesParent() =
        newCaldavTask(with(REMOTE_PARENT, "1234"))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(PARENT, "1234"))
            )
            .let {
                assertNull(it.remoteParent)
            }

    @Test
    fun localRemovesParent() =
        newCaldavTask()
            .applyRemote(
                remote = newIcal(with(PARENT, "1234")),
                local = newIcal(with(PARENT, "1234"))
            )
            .let {
                assertNull(it.remoteParent)
            }

    @Test
    fun localBeatsRemoteParent() =
        newCaldavTask(with(REMOTE_PARENT, "789"))
            .applyRemote(
                remote = newIcal(with(PARENT, "456")),
                local = newIcal(with(PARENT, "123"))
            )
            .let {
                assertEquals("789", it.remoteParent)
            }

    companion object {
        private fun DateTime.allDay() =
            createDueDate(URGENCY_SPECIFIC_DAY, millis)
    }
}