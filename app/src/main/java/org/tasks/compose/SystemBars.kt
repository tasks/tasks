package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
) {
    val systemUiController = rememberSystemUiController()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                systemUiController.setStatusBarColor(color = statusBarColor)
                systemUiController.setNavigationBarColor(color = navigationBarColor)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
