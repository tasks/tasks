package org.tasks.kmp.org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

@Composable
fun TouchSlopMultiplier(
    multiplier: Float = 3f,
    content: @Composable () -> Unit,
) {
    val current = LocalViewConfiguration.current

    val viewConfiguration = object : ViewConfiguration by current {
        override val touchSlop: Float
            get() = current.touchSlop * multiplier
    }
    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
        content()
    }
}
