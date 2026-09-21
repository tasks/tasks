package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinuxNotificationIdsTest {
    private var now = 1_000L
    private val ids = LinuxNotificationIds(elapsedRealtime = { now })

    @Test
    fun aTasksCurrentIdIsWhatReplacesIt() {
        assertNull(ids.idFor(1L))

        ids.posted(taskId = 1L, id = 7)

        assertEquals(7, ids.idFor(1L))
    }

    @Test
    fun reusingAnIdDoesNotLeaveItPointingAtTwoTasks() {
        ids.posted(taskId = 1L, id = 7)

        ids.posted(taskId = 2L, id = 7)

        assertNull(ids.idFor(1L))
        assertEquals(7, ids.idFor(2L))
        assertEquals(2L, ids.actionInvoked(7))
    }

    @Test
    fun anIdNobodyPostedIsNotATask() {
        assertNull(ids.actionInvoked(7))
        assertEquals(LinuxNotificationIds.Closed(null, acted = false), ids.closed(7))
    }

    @Test
    fun theCloseThatFollowsAnActionIsNotADismissal() {
        ids.posted(taskId = 1L, id = 7)
        ids.actionInvoked(7)

        assertEquals(LinuxNotificationIds.Closed(1L, acted = true), ids.closed(7))
    }

    @Test
    fun aCloseLongAfterTheActionIsAGenuineDismissal() {
        ids.posted(taskId = 1L, id = 7)
        ids.actionInvoked(7)
        now += LinuxNotificationIds.ACTED_WINDOW_MS + 1

        assertEquals(LinuxNotificationIds.Closed(1L, acted = false), ids.closed(7))
    }

    @Test
    fun aDismissalTakesTheIdsItCanAddressAndForgetsThem() {
        ids.posted(taskId = 1L, id = 7)
        ids.posted(taskId = 2L, id = 8)

        assertEquals(mapOf(1L to 7, 2L to 8), ids.dismissing(listOf(1L, 2L, 3L)))

        assertNull(ids.idFor(1L))
        assertTrue(ids.dismissing(listOf(1L, 2L)).isEmpty())
    }

    @Test
    fun adoptedIdsCanBeDismissed() {
        ids.adopt(mapOf(1L to 7, 2L to 8))

        assertEquals(mapOf(1L to 7, 2L to 8), ids.dismissing(listOf(1L, 2L)))
    }

    @Test
    fun adoptingNeverDisplacesWhatThisRunPosted() {
        ids.posted(taskId = 1L, id = 42)

        ids.adopt(mapOf(1L to 7))

        assertEquals(42, ids.idFor(1L))
    }

    @Test
    fun adoptingSkipsAnIdThisRunHasAlreadyUsed() {
        ids.posted(taskId = 1L, id = 7)

        ids.adopt(mapOf(2L to 7))

        assertNull(ids.idFor(2L))
        assertEquals(1L, ids.actionInvoked(7))
    }

    @Test
    fun aClimbingCounterIsNotARestart() {
        assertEquals(false, ids.counterWentBackwards(1))
        assertEquals(false, ids.counterWentBackwards(2))
        assertEquals(false, ids.counterWentBackwards(50))
    }

    @Test
    fun aCounterBackAtTheStartIsARestart() {
        ids.counterWentBackwards(50)

        assertEquals(true, ids.counterWentBackwards(1))
    }

    @Test
    fun theRestartResetsTheMarkSoTheNextIdIsNotAlsoARestart() {
        ids.counterWentBackwards(50)
        ids.counterWentBackwards(1)

        assertEquals(false, ids.counterWentBackwards(2))
    }

    @Test
    fun adoptedIdsCountTowardsTheMarkSoARestartIsStillSpotted() {
        ids.adopt(mapOf(1L to 50))

        assertEquals(true, ids.counterWentBackwards(1))
    }

    @Test
    fun serverRestartHandsBackEveryTaskAndForgetsTheIds() {
        ids.posted(taskId = 1L, id = 7)
        ids.posted(taskId = 2L, id = 8)

        assertEquals(listOf(1L, 2L), ids.serverRestarted().sorted())

        assertNull(ids.idFor(1L))
        assertNull(ids.actionInvoked(7))
        assertEquals(emptyList<Long>(), ids.serverRestarted())
    }

    @Test
    fun takeAllEmptiesTheMapping() {
        ids.posted(taskId = 1L, id = 7)
        ids.posted(taskId = 2L, id = 8)

        assertEquals(listOf(7, 8), ids.takeAll())

        assertNull(ids.idFor(1L))
        assertEquals(LinuxNotificationIds.Closed(null, acted = false), ids.closed(7))
    }
}
