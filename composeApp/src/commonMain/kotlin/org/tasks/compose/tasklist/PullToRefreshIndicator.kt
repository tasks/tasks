package org.tasks.compose.tasklist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons

/**
 * Refresh indicator in the style of the legacy Android app: a clockwise arrow on a
 * surface-coloured circle. It slides in from behind the overlaid top bar as the list
 * is pulled, rotates with the pull distance, and spins while a sync is running.
 */
@Composable
internal fun BoxScope.SyncPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    topBarHeight: Dp,
) {
    val pullRotation by animateFloatAsState(
        targetValue = state.distanceFraction * 360f,
        animationSpec = tween(durationMillis = 100),
        label = "pullRotation",
    )
    val spinRotation = if (isRefreshing) {
        // Only composed while refreshing, so the animation is torn down afterwards.
        val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "spin",
        ).value
    } else {
        pullRotation
    }

    PullToRefreshDefaults.IndicatorBox(
        state = state,
        isRefreshing = isRefreshing,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topBarHeight),
        // The circle slides from fully hidden behind the top bar to fully visible
        // just below it at distanceFraction == 1: one circle diameter of travel.
        maxDistance = PULL_REFRESH_CIRCLE_SIZE,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SymbolIcon(
            name = TasksIcons.REFRESH,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = spinRotation },
        )
    }
}

private val PULL_REFRESH_CIRCLE_SIZE = 40.dp
