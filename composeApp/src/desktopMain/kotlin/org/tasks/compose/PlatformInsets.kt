package org.tasks.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun StatusBarScrim(color: Color, modifier: Modifier) {
    // No system bars on desktop
}

@Composable
actual fun NavigationBarScrim(color: Color, modifier: Modifier) {
    // No system bars on desktop
}

actual fun Modifier.platformNavigationBarsPadding(): Modifier = this

@Composable
actual fun platformSidebarInsets(): PaddingValues = PaddingValues(0.dp)

@Composable
actual fun platformStatusBarInsets(): PaddingValues = PaddingValues(0.dp)
