package org.tasks.compose

/**
 * Simple Swipe-to-delete implementation
 */

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.tasks.R
import kotlin.math.roundToInt

object SwipeOut {

    private enum class Anchors { Left, Center, Right }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun SwipeOut(
        modifier: Modifier = Modifier,
        index: Int,
        onSwipe: (Int) -> Unit,
        decoration: @Composable BoxScope.() -> Unit = {},
        content: @Composable BoxScope.() -> Unit
    ) {
        val screenWidthPx =
            with(LocalDensity.current) {
                LocalConfiguration.current.screenWidthDp.dp.roundToPx().toFloat()
            }

        val decayAnimationSpec = rememberSplineBasedDecay<Float>()
        val dragState = remember {
            AnchoredDraggableState(
                initialValue = Anchors.Center,
                anchors = DraggableAnchors {
                    Anchors.Left at -screenWidthPx * 3/4
                    Anchors.Center at 0f
                    Anchors.Right at screenWidthPx * 3/4
                },
                positionalThreshold = { screenWidthPx/3 },
                velocityThreshold = { 100f },
                snapAnimationSpec = tween(),
                decayAnimationSpec = decayAnimationSpec,
            )
        }

        if (dragState.currentValue == Anchors.Left || dragState.currentValue == Anchors.Right) {
            onSwipe(index)
        }

        Box(    /* container for swipeable and it's background decoration */
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {

            decoration()

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = dragState
                                .requireOffset()
                                .roundToInt(),
                            y = 0
                        )
                    }
                    .background(colorResource(id = R.color.content_background)) // MUST BE AFTER .offset modifier (?!?!)
                    .anchoredDraggable(dragState, reverseDirection = false, orientation = Orientation.Horizontal)
            ) {
                content()
            }
        }
    }
}