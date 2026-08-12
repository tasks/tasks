package org.tasks.compose.drag

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.input.key.onKeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TabToIndentTest {
    private data class Pressed(val moved: List<Int>, val fellThrough: Boolean)

    private fun run(
        indent: Int,
        range: IntRange,
        swallowWhenBlocked: Boolean = false,
        keys: androidx.compose.ui.test.KeyInjectionScope.() -> Unit,
    ): Pressed {
        val moved = mutableListOf<Int>()
        var fellThrough = false
        runComposeUiTest {
            val focus = FocusRequester()
            setContent {
                Box(Modifier.onKeyEvent { fellThrough = true; false }) {
                    Box(
                        Modifier
                            .tabToIndent(
                                indent = indent,
                                range = { range },
                                onIndentChange = { moved.add(it) },
                                swallowWhenBlocked = swallowWhenBlocked,
                            )
                            .focusRequester(focus)
                            .focusable()
                    )
                }
            }
            focus.requestFocus()
            onRoot().performKeyInput(keys)
        }
        return Pressed(moved = moved, fellThrough = fellThrough)
    }

    @Test
    fun tabNestsOneLevel() {
        assertEquals(listOf(1), run(indent = 0, range = 0..2) { pressKey(Key.Tab) }.moved)
    }

    @Test
    fun shiftTabComesOutOneLevel() {
        assertEquals(
            listOf(1),
            run(indent = 2, range = 0..2) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }.moved,
        )
    }

    @Test
    fun aTabThatWouldGoDeeperThanTheSlotAllowsMovesNothing() {
        assertEquals(emptyList<Int>(), run(indent = 1, range = 0..1) { pressKey(Key.Tab) }.moved)
    }

    @Test
    fun shiftTabAtTheTopLevelMovesNothing() {
        assertEquals(
            emptyList<Int>(),
            run(indent = 0, range = 0..2) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }.moved,
        )
    }

    @Test
    fun aTabIsActedOnOnceRatherThanOnTheWayDownAndAgainOnTheWayUp() {
        assertEquals(listOf(1), run(indent = 0, range = 0..2) { pressKey(Key.Tab) }.moved)
    }

    @Test
    fun aTabThatCannotMoveTheRowFallsThroughToFocusTraversal() {
        val pressed = run(indent = 1, range = 0..1) { pressKey(Key.Tab) }

        assertEquals(emptyList<Int>(), pressed.moved)
        assertTrue(pressed.fellThrough)
    }

    @Test
    fun aTabThatCannotMoveTheRowIsSwallowedWhileTheTitleIsBeingTypedInto() {
        val pressed = run(indent = 1, range = 0..1, swallowWhenBlocked = true) { pressKey(Key.Tab) }

        assertEquals(emptyList<Int>(), pressed.moved)
        assertFalse(pressed.fellThrough)
    }

    @Test
    fun aTabThatMovesTheRowIsNeverAlsoFocusTraversal() {
        val pressed = run(indent = 0, range = 0..2) { pressKey(Key.Tab) }

        assertEquals(listOf(1), pressed.moved)
        assertFalse(pressed.fellThrough)
    }
}
