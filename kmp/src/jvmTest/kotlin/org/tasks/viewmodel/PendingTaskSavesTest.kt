package org.tasks.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingTaskSavesTest {
    private val testDispatcher = StandardTestDispatcher()
    private val pendingSaves = PendingTaskSaves(CoroutineScope(testDispatcher))

    @Test
    fun theEnqueuedSaveRunsWithTheKeysLockAlreadyHeld() = runTest(testDispatcher) {
        val running = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        pendingSaves.enqueueLocked("key") {
            running.complete(Unit)
            release.await()
        }
        runCurrent()
        running.await()
        var claimed = false

        val contender = async { pendingSaves.withLock("key") { claimed = true } }
        runCurrent()

        assertFalse(claimed)
        release.complete(Unit)
        contender.await()
        assertTrue(claimed)
    }

    @Test
    fun aSaveIsPendingFromTheMomentItIsEnqueued() = runTest(testDispatcher) {
        var saved = false

        pendingSaves.enqueueLocked("key") { saved = true }

        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            pendingSaves.awaitPending("key")
        }

        assertFalse(saved)
        assertTrue(waiter.isActive)
        advanceUntilIdle()
        assertTrue(saved)
        assertFalse(waiter.isActive)
    }

    @Test
    fun aSaveOnAnotherKeyDoesNotHoldTheLock() = runTest(testDispatcher) {
        val release = CompletableDeferred<Unit>()
        pendingSaves.enqueueLocked("other") { release.await() }
        runCurrent()
        var claimed = false

        val contender = async { pendingSaves.withLock("key") { claimed = true } }
        runCurrent()

        assertTrue(claimed)
        release.complete(Unit)
        contender.await()
    }
}
