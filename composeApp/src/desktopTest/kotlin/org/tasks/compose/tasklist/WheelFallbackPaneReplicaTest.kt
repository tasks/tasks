package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
 * Replica of the real task-list pane wiring: `CompletePullToRefresh` watching the state from
 * outside, a `PullToRefreshBox` with the same threshold, and the [WheelPullToRefreshFallback]
 * around a short (non-scrolling) [LazyColumn], exactly the structure that fails in the real
 * app. Every wheel event here is orphaned, so only the fallback path can fire.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class WheelFallbackPaneReplicaTest {

    private val threshold = 40.dp

    private val rows = listOf("one", "two", "three")

    @Test
    fun wheelUpOnShortListInsideBoxFiresRefresh() =
        runComposeUiTest {
            var boxRefreshes = 0
            var completeRefreshes = 0
            var refreshing = false
            val state = PullToRefreshState()
            setContent {
                Box(Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            boxRefreshes++
                        },
                        threshold = threshold,
                        state = state,
                        indicator = {},
                    ) {
                        WheelPullToRefreshFallback(
                            state = state,
                            isRefreshing = refreshing,
                            threshold = threshold,
                        ) {
                            LazyColumn(state = rememberLazyListState()) {
                                items(rows) { Text(it) }
                            }
                        }
                    }
                    CompletePullToRefresh(state, { refreshing }) {
                        refreshing = true
                        completeRefreshes++
                    }
                }
            }
            waitForIdle()

            onRoot().performMouseInput { scroll(Offset(0f, -500f)) }
            waitForIdle()

            val total = boxRefreshes + completeRefreshes
            assertTrue(
                "no refresh fired (box=$boxRefreshes complete=$completeRefreshes fraction=${state.distanceFraction})",
                total >= 1,
            )
        }

    @Test
    fun partialPullOnShortListInsideBoxRetractsItself() =
        runComposeUiTest {
            var boxRefreshes = 0
            var completeRefreshes = 0
            var refreshing = false
            val state = PullToRefreshState()
            setContent {
                Box(Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            boxRefreshes++
                        },
                        threshold = threshold,
                        state = state,
                        indicator = {},
                    ) {
                        WheelPullToRefreshFallback(
                            state = state,
                            isRefreshing = refreshing,
                            threshold = threshold,
                            retractIdleMillis = 50,
                        ) {
                            LazyColumn(state = rememberLazyListState()) {
                                items(rows) { Text(it) }
                            }
                        }
                    }
                    CompletePullToRefresh(state, { refreshing }) {
                        refreshing = true
                        completeRefreshes++
                    }
                }
            }
            waitForIdle()

            onRoot().performMouseInput { scroll(Offset(0f, -15f)) }
            waitForIdle()
            assertTrue(state.distanceFraction > 0f)

            waitUntil("partial pull retracts", 10_000) { state.distanceFraction == 0f }
            assertEquals(0, boxRefreshes)
            assertEquals(0, completeRefreshes)
        }

    /**
     * The desktop configuration: the box's own pull path is disabled (`enabled = false`),
     * so the fallback is the only trigger. The wheel pull must still complete a refresh
     * via the shared threshold trigger, and the box's own onRefresh must never fire.
     */
    @Test
    fun wheelUpWithBoxPullDisabledFiresOnlyViaFallback() =
        runComposeUiTest {
            var boxRefreshes = 0
            var completeRefreshes = 0
            var refreshing = false
            val state = PullToRefreshState()
            setContent {
                Box(Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            boxRefreshes++
                        },
                        enabled = false,
                        threshold = threshold,
                        state = state,
                        indicator = {},
                    ) {
                        WheelPullToRefreshFallback(
                            state = state,
                            isRefreshing = refreshing,
                            threshold = threshold,
                        ) {
                            LazyColumn(state = rememberLazyListState()) {
                                items(rows) { Text(it) }
                            }
                        }
                    }
                    CompletePullToRefresh(state, { refreshing }) {
                        refreshing = true
                        completeRefreshes++
                    }
                }
            }
            waitForIdle()

            onRoot().performMouseInput { scroll(Offset(0f, -50f)) }
            waitForIdle()

            assertEquals(0, boxRefreshes)
            assertEquals(1, completeRefreshes)
        }
}
