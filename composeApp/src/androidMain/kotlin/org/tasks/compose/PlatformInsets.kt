package org.tasks.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun StatusBarScrim(color: Color, modifier: Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(color),
    )
}

@Composable
actual fun NavigationBarScrim(color: Color, modifier: Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsBottomHeight(WindowInsets.navigationBars)
            .background(color),
    )
}

actual fun Modifier.platformNavigationBarsPadding(): Modifier =
    this.navigationBarsPadding()
