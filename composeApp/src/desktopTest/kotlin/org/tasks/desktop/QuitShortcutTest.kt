package org.tasks.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuitShortcutTest {
    @Test
    fun ctrlQOnKeyDownIsTheQuitShortcut() {
        assertTrue(isQuitShortcut(Key.Q, KeyEventType.KeyDown, ctrlPressed = true, metaPressed = false))
    }

    @Test
    fun metaQOnKeyDownIsTheQuitShortcut() {
        assertTrue(isQuitShortcut(Key.Q, KeyEventType.KeyDown, ctrlPressed = false, metaPressed = true))
    }

    @Test
    fun qWithoutAModifierIsNotTheQuitShortcut() {
        assertFalse(isQuitShortcut(Key.Q, KeyEventType.KeyDown, ctrlPressed = false, metaPressed = false))
    }

    @Test
    fun anotherKeyWithCtrlIsNotTheQuitShortcut() {
        assertFalse(isQuitShortcut(Key.W, KeyEventType.KeyDown, ctrlPressed = true, metaPressed = false))
    }

    // KeyUp must not match, or the close path runs twice for a single press.
    @Test
    fun keyUpIsNotTheQuitShortcut() {
        assertFalse(isQuitShortcut(Key.Q, KeyEventType.KeyUp, ctrlPressed = true, metaPressed = false))
    }
}
