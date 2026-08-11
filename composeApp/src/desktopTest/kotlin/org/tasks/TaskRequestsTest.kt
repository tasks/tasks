package org.tasks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRequestsTest {
    private val requests = TaskRequests()

    @Test
    fun aSnoozeAskedForBeforeAnyoneIsListeningIsStillDelivered() = runTest {
        requests.snooze(42L)

        assertEquals(42L, requests.snoozeRequests.first())
    }

    @Test
    fun snoozeRequestsArriveInOrder() = runTest {
        requests.snooze(1L)
        requests.snooze(2L)
        requests.snooze(3L)

        assertEquals(listOf(1L, 2L, 3L), requests.snoozeRequests.take(3).toList())
    }

    @Test
    fun aSnoozeHandedBackIsOfferedAgain() = runTest {
        requests.snooze(42L)
        assertEquals(42L, requests.snoozeRequests.first())

        requests.snooze(42L)

        assertEquals(42L, requests.snoozeRequests.first())
    }

    @Test
    fun openReportsWhatTheAppDidWithIt() = runTest(StandardTestDispatcher()) {
        val answered = collectOneOpenRequest(answer = true)

        assertTrue(requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")))
        assertEquals(42L, answered.await().destination.taskId)
    }

    @Test
    fun openReportsARefusal() = runTest(StandardTestDispatcher()) {
        collectOneOpenRequest(answer = false)

        assertFalse(requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")))
    }

    @Test
    fun openWithNobodyListeningAtAllIsARefusal() = runTest(StandardTestDispatcher()) {
        val accepted = async { requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")) }

        runCurrent()
        advanceTimeBy(OPEN_TIMEOUT_MS + 1)
        runCurrent()

        assertFalse(accepted.await())
    }

    @Test
    fun openGivesUpWhenTheCollectorNeverAnswers() = runTest(StandardTestDispatcher()) {
        val collecting = backgroundScope.async { requests.openRequests.first() }
        runCurrent()

        val accepted = async { requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")) }
        runCurrent()
        advanceTimeBy(OPEN_TIMEOUT_MS + 1)
        runCurrent()

        assertFalse(accepted.await())

        assertEquals(42L, collecting.await().destination.taskId)
    }

    @Test
    fun openIsRefusedOutrightWhileTheAppIsShuttingDown() = runTest(StandardTestDispatcher()) {
        val collecting = collectOneOpenRequest(answer = true)
        requests.acceptOpenRequests(false)

        val accepted = async { requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")) }
        runCurrent()

        assertFalse(accepted.await())

        assertTrue(collecting.isActive)
        collecting.cancel()
    }

    @Test
    fun openWorksAgainWhenAnAbandonedQuitBringsTheWindowBack() = runTest(StandardTestDispatcher()) {
        requests.acceptOpenRequests(false)
        requests.acceptOpenRequests(true)
        collectOneOpenRequest(answer = true)

        val accepted = async { requests.open(TaskEditDestination(taskId = 42L, remoteId = "abc")) }
        runCurrent()

        assertTrue(accepted.await())
    }

    private fun TestScope.collectOneOpenRequest(answer: Boolean) =
        backgroundScope.async { requests.openRequests.first().also { it.complete(answer) } }
            .also { runCurrent() }

    companion object {
        private const val OPEN_TIMEOUT_MS = 5_000L
    }
}
