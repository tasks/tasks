package org.tasks.compose.tasklist

import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

/**
 * Completes a pull-to-refresh gesture for inputs that have no release event.
 *
 * [androidx.compose.material3.pulltorefresh.PullToRefreshBox] requests a refresh (its
 * `onRefresh` callback) only when a touch gesture is released past the threshold. A
 * mouse wheel or trackpad pull on desktop can drive the pull all the way to the
 * threshold — the indicator appears and fills — but never "releases" it, so the box
 * never calls `onRefresh` and the refresh is never requested.
 *
 * This observes the box's own [PullToRefreshState] and requests the refresh as soon as
 * a pull reaches the full distance, which is exactly what a release past the threshold
 * would have done. No input is read or consumed here — the state the box already
 * publishes is the only input.
 *
 * Touch is unaffected: the box's own trigger still fires on release, and both paths
 * call the same idempotent refresh.
 *
 * @param state the pull-to-refresh state shared with the box
 * @param isRefreshing whether a refresh is already in progress (the box's `isRefreshing`)
 * @param onRefresh invoked once when a pull reaches the full distance without having
 *   been completed by the box's own release trigger
 */
@Composable
internal fun CompletePullToRefresh(
    state: PullToRefreshState,
    isRefreshing: () -> Boolean,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(state) {
        // Armed between full pulls: the fraction may sit at (or keep emitting at) the
        // full distance while a refresh is in progress; only the next fresh crossing
        // fires again.
        var armed = true
        snapshotFlow { state.distanceFraction }
            .collect { fraction ->
                if (fraction >= 1f) {
                    if (armed && !isRefreshing()) {
                        onRefresh()
                    }
                    armed = false
                } else {
                    armed = true
                }
            }
    }
}
