package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

@Composable
fun StableWindowSize(content: @Composable () -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val containerDpSize = windowInfo.containerDpSize
    val lastValid = remember(windowInfo) { LastValidWindowSize() }
    val stable = remember(windowInfo, containerSize, containerDpSize) {
        val (px, dp) = lastValid.stabilize(containerSize, containerDpSize)
        if (px == containerSize) windowInfo else HeldSizeWindowInfo(windowInfo, px, dp)
    }
    CompositionLocalProvider(LocalWindowInfo provides stable, content = content)
}

internal class LastValidWindowSize {
    private var size: IntSize? = null
    private var dpSize: DpSize? = null

    fun stabilize(current: IntSize, currentDp: DpSize): Pair<IntSize, DpSize> {
        if (current.width > 0 && current.height > 0) {
            size = current
            dpSize = currentDp
            return current to currentDp
        }
        return (size ?: IntSize.Zero) to (dpSize ?: DpSize.Zero)
    }
}

private class HeldSizeWindowInfo(
    delegate: WindowInfo,
    override val containerSize: IntSize,
    override val containerDpSize: DpSize,
) : WindowInfo by delegate
