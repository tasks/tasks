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
 * Tests for [WheelPullToRefreshFallback]: an unconsumed wheel pull drives the shared
 * pull-to-refresh state to the full-pull point (thanks to the wheel gain), the shared
 * threshold trigger ([CompletePullToRefresh], the same composition as the task list
 * pane) completes it into a refresh, and a pull that stops short retracts once the
 * wheel goes quiet.
 *
 * The content is deliberately not scrollable, so every wheel event is exactly the
 * orphaned event the fallback exists for.
 */
@OptIn(ExperimentalTestApi::class)
class WheelPullToRefreshFallbackTest {

    private val threshold = 40.dp

    @Test
    fun shortWheelPullFillsIndicatorToFullPullAndFiresRefreshOnce() =
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

            // A short wheel-up (a few notches) fills the shared state to the full-pull
            // point (the wheel gain amplifies the small notch deltas) and the shared
            // threshold trigger fires exactly once.
            onRoot().performMouseInput { scroll(Offset(0f, -50f)) }
            waitForIdle()
            assertEquals(1, refreshes)
            assertTrue(state.distanceFraction >= 1f)
            assertTrue(state.distanceFraction <= 2f)
        }

    @Test
    fun wheelDownRetractsAPartialPullBeforeItTriggers() =
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

            // A small wheel-up is a partial pull: the indicator moves but the pull does
            // not reach the full-pull point, so nothing fires.
            onRoot().performMouseInput { scroll(Offset(0f, -15f)) }
            waitForIdle()
            assertEquals(0, refreshes)
            assertTrue(state.distanceFraction > 0f)
            assertTrue(state.distanceFraction < 1f)

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
            assertTrue(state.distanceFraction < 1f)

            // No more wheel input: the partial pull retracts on its own once the idle
            // timer (a virtual-clock delay in the test environment) elapses.
            waitUntil("partial pull retracts", 10_000) { state.distanceFraction == 0f }
            assertEquals(0, refreshes)
        }

    @Test
    fun aLongWheelPullOvershootsButStillFiresOnce() =
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

            // A firm wheel-up overshoots the full-pull point into the tension curve;
            // the shared trigger fires once as the pull crosses it, and the indicator
            // is capped at its maximum travel.
            onRoot().performMouseInput { scroll(Offset(0f, -300f)) }
            waitForIdle()
            assertEquals(1, refreshes)
            assertTrue(state.distanceFraction >= 1f)
            assertTrue(state.distanceFraction <= 2f)
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

            onRoot().performMouseInput { scroll(Offset(0f, -300f)) }
            waitForIdle()
            assertEquals(0, refreshes)
            assertEquals(0f, state.distanceFraction, 0.001f)
        }
}
