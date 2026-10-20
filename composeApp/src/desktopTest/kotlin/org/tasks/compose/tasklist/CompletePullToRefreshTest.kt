package org.tasks.compose.tasklist

import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [CompletePullToRefresh]: the refresh is requested exactly once when the
 * pull state reaches the full distance — the "release" an input like a mouse wheel
 * never provides — and the watcher re-arms after the pull retracts.
 *
 * The pull state is driven directly with [PullToRefreshState.snapTo], which is the same
 * public state the [androidx.compose.material3.pulltorefresh.PullToRefreshBox] drives
 * from scroll input; no input simulation is involved.
 */
@OptIn(ExperimentalTestApi::class)
class CompletePullToRefreshTest {

    @Test
    fun fullPullFiresRefreshOnceAndRearmsAfterRetract() =
        runComposeUiTest {
            var refreshes = 0
            val state = PullToRefreshState()
            setContent {
                CompletePullToRefresh(state, isRefreshing = { false }) {
                    refreshes++
                }
            }
            waitForIdle()

            // A partial pull (below the trigger fraction) does not fire.
            state.snapTo(0.1f)
            waitForIdle()
            assertEquals(0, refreshes)

            // Reaching the trigger distance fires exactly once...
            state.snapTo(1f)
            waitForIdle()
            assertEquals(1, refreshes)

            // ...and the fraction staying at the full distance (while a refresh is in
            // progress, the box pins it there) does not fire again.
            state.snapTo(1f)
            waitForIdle()
            assertEquals(1, refreshes)

            // Retracting re-arms the watcher for the next pull.
            state.snapTo(0f)
            waitForIdle()
            state.snapTo(1f)
            waitForIdle()
            assertEquals(2, refreshes)
        }

    @Test
    fun doesNotFireWhileRefreshing() =
        runComposeUiTest {
            var refreshes = 0
            var refreshing = false
            val state = PullToRefreshState()
            setContent {
                CompletePullToRefresh(state, isRefreshing = { refreshing }) {
                    refreshes++
                }
            }
            waitForIdle()

            // The box's own release trigger has already fired (a refresh is in
            // progress) when the fraction reaches the full distance: no second fire.
            refreshing = true
            state.snapTo(1f)
            waitForIdle()
            assertEquals(0, refreshes)
        }
}
