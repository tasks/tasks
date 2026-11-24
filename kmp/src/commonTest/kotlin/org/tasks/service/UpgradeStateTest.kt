package org.tasks.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpgradeStateTest {
    @Test
    fun awaitReadyWaitsWhilePending() = runTest {
        val state = UpgradeState()
        val waiter = async { state.awaitReady("test") }
        runCurrent()
        assertFalse(waiter.isCompleted)

        state.ready()
        runCurrent()

        assertTrue(waiter.isCompleted)
    }

    @Test
    fun awaitReadyReturnsWhenReady() = runTest {
        val state = UpgradeState()
        state.ready()

        state.awaitReady("test")

        assertTrue(state.isReady)
    }

    @Test
    fun launchIsUpgradingUntilBlockFinishes() = runTest {
        val state = UpgradeState()
        val release = CompletableDeferred<Unit>()

        val job = state.launch(scope(), upgrading = true) { release.await() }

        assertTrue(state.isUpgrading)
        assertFalse(state.isReady)

        release.complete(Unit)
        job.join()

        assertFalse(state.isUpgrading)
        assertTrue(state.isReady)
    }

    @Test
    fun failedLaunchIsNeverReady() = runTest {
        val state = UpgradeState()

        state.launch(scope(), upgrading = true) { throw IllegalStateException("boom") }.join()

        assertTrue(state.isUpgrading)
        assertFalse(state.isReady)
    }

    private fun scope() = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, _ -> })
}
