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
 * a pull reaches [triggerFraction], which is exactly what a release past the threshold
 * would have done. No input is read or consumed here — the state the box already
 * publishes is the only input.
 *
 * Touch is unaffected in practice: the box's own trigger still fires on release, and
 * both paths call the same idempotent refresh (the `SyncRunner` coalesces duplicate
 * requests). Wheel input drives small increments (a notch is well under a full pull),
 * so the trigger fraction is set well below a full pull for the wheel path — a few
 * notches — while the indicator's travel is still a full pull's worth.
 *
 * @param state the pull-to-refresh state shared with the box
 * @param isRefreshing whether a refresh is already in progress (the box's `isRefreshing`)
 * @param triggerFraction the pull distance (as a fraction of the box's threshold) at
 *   which the refresh is requested; defaults to [TRIGGER_FRACTION]
 * @param onRefresh invoked once when a pull reaches [triggerFraction] without having
 *   been completed by the box's own release trigger
 */
@Composable
internal fun CompletePullToRefresh(
    state: PullToRefreshState,
    isRefreshing: () -> Boolean,
    triggerFraction: Float = TRIGGER_FRACTION,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(state) {
        // Armed between pulls: the fraction may sit at (or keep emitting at) the
        // trigger distance while a refresh is in progress; only the next fresh
        // crossing fires again.
        var armed = true
        snapshotFlow { state.distanceFraction }
            .collect { fraction ->
                if (fraction >= triggerFraction) {
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

/**
 * Wheel notches are small compared with a touch drag (a notch is well under a full
 * pull), so the wheel path requests the refresh at a quarter of a full pull — about
 * three to four notches — rather than requiring the whole indicator travel.
 */
internal const val TRIGGER_FRACTION = 0.2f
