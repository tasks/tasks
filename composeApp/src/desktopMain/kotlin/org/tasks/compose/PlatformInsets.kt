package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun StatusBarScrim(color: Color, modifier: Modifier) {
    // No system bars on desktop
}

@Composable
actual fun NavigationBarScrim(color: Color, modifier: Modifier) {
    // No system bars on desktop
}

actual fun Modifier.platformNavigationBarsPadding(): Modifier = this
