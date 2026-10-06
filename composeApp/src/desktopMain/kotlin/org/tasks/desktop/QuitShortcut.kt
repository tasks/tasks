package org.tasks.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Whether a key press is the "quit the application" shortcut.
 *
 * Split out from the [KeyEvent] overload so it can be unit tested without building AWT
 * events. Matches on KeyDown only: matching KeyUp as well would run the close path twice
 * for a single press.
 *
 * @param key the key that was pressed
 * @param type whether this is the key going down or coming back up
 * @param ctrlPressed whether Ctrl was held (Linux/Windows convention)
 * @param metaPressed whether Cmd was held (macOS convention)
 * @return true if this press means "quit"
 */
internal fun isQuitShortcut(
    key: Key,
    type: KeyEventType,
    ctrlPressed: Boolean,
    metaPressed: Boolean,
): Boolean = type == KeyEventType.KeyDown && key == Key.Q && (ctrlPressed || metaPressed)

/**
 * Whether [event] is the "quit the application" shortcut.
 *
 * @param event the key event to inspect
 * @return true if this event means "quit"
 */
internal fun isQuitShortcut(event: KeyEvent): Boolean =
    isQuitShortcut(event.key, event.type, event.isCtrlPressed, event.isMetaPressed)
