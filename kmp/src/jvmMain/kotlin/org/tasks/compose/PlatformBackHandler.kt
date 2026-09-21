package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.Snapshot
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

private object BackHandlerRegistry {
    private val handlers = mutableListOf<BackHandlerEntry>()
    private var installed = false
    private var focusManager: KeyboardFocusManager? = null

    class BackHandlerEntry(
        val enabled: State<Boolean>,
        val onBack: State<() -> Unit>,
    )

    private val dispatcher = KeyEventDispatcher { event ->
        if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE) {
            val snapshot = Snapshot.takeSnapshot()
            try {
                val callback = snapshot.enter {
                    synchronized(handlers) {
                        handlers.lastOrNull { it.enabled.value }?.onBack?.value
                    }
                }
                if (callback != null) {
                    callback()
                    true
                } else {
                    false
                }
            } finally {
                snapshot.dispose()
            }
        } else {
            false
        }
    }

    fun add(entry: BackHandlerEntry) {
        synchronized(handlers) {
            handlers.add(entry)
            if (!installed) {
                val mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                mgr.addKeyEventDispatcher(dispatcher)
                focusManager = mgr
                installed = true
            }
        }
    }

    fun remove(entry: BackHandlerEntry) {
        synchronized(handlers) {
            handlers.remove(entry)
            if (handlers.isEmpty() && installed) {
                focusManager?.removeKeyEventDispatcher(dispatcher)
                focusManager = null
                installed = false
            }
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)
    val currentEnabled = rememberUpdatedState(enabled)
    DisposableEffect(Unit) {
        val entry = BackHandlerRegistry.BackHandlerEntry(
            enabled = currentEnabled,
            onBack = currentOnBack,
        )
        BackHandlerRegistry.add(entry)
        onDispose {
            BackHandlerRegistry.remove(entry)
        }
    }
}
