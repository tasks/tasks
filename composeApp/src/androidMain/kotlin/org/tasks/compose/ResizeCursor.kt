package org.tasks.compose

import android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

private val resizeCursor = PointerIcon(TYPE_HORIZONTAL_DOUBLE_ARROW)

actual fun Modifier.horizontalResizeCursor(): Modifier = pointerHoverIcon(resizeCursor)
