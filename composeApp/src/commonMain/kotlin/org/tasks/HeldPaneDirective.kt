package org.tasks

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberHeldPaneDirective(
    directive: PaneScaffoldDirective,
    showing: Boolean,
): PaneScaffoldDirective {
    var held by remember { mutableStateOf(directive) }
    LaunchedEffect(directive, showing) {
        if (showing) held = directive
    }
    return held
}
