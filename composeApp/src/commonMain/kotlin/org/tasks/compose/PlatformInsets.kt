package org.tasks.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun StatusBarScrim(color: Color, modifier: Modifier = Modifier)

@Composable
expect fun NavigationBarScrim(color: Color, modifier: Modifier = Modifier)

expect fun Modifier.platformNavigationBarsPadding(): Modifier

@Composable
expect fun platformSidebarInsets(): PaddingValues

@Composable
expect fun platformStatusBarInsets(): PaddingValues
