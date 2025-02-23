package org.tasks.kmp.org.tasks.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import co.touchlab.kermit.Logger

@Composable
fun rememberImeState(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeState = remember {
        derivedStateOf {
            ime.getBottom(density) > 0
        }
    }
    LaunchedEffect(imeState.value) {
        Logger.d("imeState") { "keyboardOpen=${imeState.value}" }
    }
    return imeState
}
