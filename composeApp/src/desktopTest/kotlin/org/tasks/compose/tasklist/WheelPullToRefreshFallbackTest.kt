package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [WheelPullToRefreshFallback]: unconsumed wheel input drives the shared
 * pull-to-refresh state, a full pull is completed into a refresh by
 * [CompletePullToRefresh] (the same composition as the task list pane), and a partial
 * pull retracts once the wheel goes quiet.
 *
 * The content is deliberately not scrollable, so every wheel event is exactly the
 * orphaned event the fallback exists for.
 */
@OptIn(ExperimentalTestApi::class)
class WheelPullToRefreshFallbackTest {

    private val threshold = 40.dp

    @Test
    fun wheelUpPastThresholdDrivesStateAndFiresRefreshOnce() =
        runComposeUiTest {
            var refreshes = 0
            var refreshing = false
            val state = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state,
                        isRefreshing = refreshing,
                        threshold = threshold,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state, { refreshing }) {
                        refreshing = true
                        refreshes++
                    }
                }
            }
            waitForIdle()

            // One big wheel-up: the state is driven to the full distance (capped at
            // twice the threshold) and the refresh fires exactly once.
            onRoot().performMouseInput { scroll(Offset(0f, -500f)) }
            waitForIdle()
            assertEquals(1, refreshes)
            assertTrue(state.distanceFraction >= 1f)
            assertTrue(state.distanceFraction <= 2f)
        }

    @Test
    fun smallWheelPullsTriggerAtTheTriggerFraction() =
        runComposeUiTest {
            var refreshes = 0
            val state = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state,
                        isRefreshing = false,
                        threshold = threshold,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state, { false }) { refreshes++ }
                }
            }
            waitForIdle()

            // A pull just past the trigger fraction (0.2, ~three notches) fires the
            // refresh — the wheel path does not require a full pull's travel.
            onRoot().performMouseInput { scroll(Offset(0f, -35f)) }
            waitForIdle()
            assertTrue(state.distanceFraction >= TRIGGER_FRACTION)
            assertEquals(1, refreshes)

            // Below the trigger fraction nothing fires; the pull retracts on its own.
            var refreshedAgain = 0
            val state2 = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state2,
                        isRefreshing = false,
                        threshold = threshold,
                        retractIdleMillis = 50,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state2, { false }) { refreshedAgain++ }
                }
            }
            waitForIdle()
            onRoot().performMouseInput { scroll(Offset(0f, -15f)) }
            waitForIdle()
            assertEquals(0, refreshedAgain)
            waitUntil("sub-trigger pull retracts", 10_000) { state2.distanceFraction == 0f }
            assertEquals(0, refreshedAgain)
        }

    @Test
    fun partialPullDoesNotFireAndWheelDownRetractsIt() =
        runComposeUiTest {
            var refreshes = 0
            val state = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state,
                        isRefreshing = false,
                        threshold = threshold,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state, { false }) { refreshes++ }
                }
            }
            waitForIdle()

            // A small wheel-up is a partial pull: the indicator moves, no refresh.
            onRoot().performMouseInput { scroll(Offset(0f, -15f)) }
            waitForIdle()
            assertEquals(0, refreshes)
            assertTrue(state.distanceFraction > 0f)
            assertTrue(state.distanceFraction < TRIGGER_FRACTION)

            // Wheeling down retracts the pull back to zero.
            onRoot().performMouseInput { scroll(Offset(0f, 100f)) }
            waitForIdle()
            assertEquals(0, refreshes)
            assertEquals(0f, state.distanceFraction, 0.001f)
        }

    @Test
    fun partialPullRetractsItselfOnceTheWheelIsQuiet() =
        runComposeUiTest {
            var refreshes = 0
            val state = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state,
                        isRefreshing = false,
                        threshold = threshold,
                        retractIdleMillis = 50,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state, { false }) { refreshes++ }
                }
            }
            waitForIdle()

            onRoot().performMouseInput { scroll(Offset(0f, -15f)) }
            waitForIdle()
            assertTrue(state.distanceFraction > 0f)

            // No more wheel input: the partial pull retracts on its own once the idle
            // timer (a virtual-clock delay in the test environment) elapses.
            waitUntil("partial pull retracts", 10_000) { state.distanceFraction == 0f }
            assertEquals(0, refreshes)
        }

    @Test
    fun wheelIsIgnoredWhileRefreshing() =
        runComposeUiTest {
            var refreshes = 0
            var refreshing = true
            val state = PullToRefreshState()
            setContent {
                Box {
                    WheelPullToRefreshFallback(
                        state = state,
                        isRefreshing = refreshing,
                        threshold = threshold,
                    ) { Box(Modifier.fillMaxSize()) }
                    CompletePullToRefresh(state, { refreshing }) { refreshes++ }
                }
            }
            waitForIdle()

            onRoot().performMouseInput { scroll(Offset(0f, -500f)) }
            waitForIdle()
            assertEquals(0, refreshes)
            assertEquals(0f, state.distanceFraction, 0.001f)
        }
}
