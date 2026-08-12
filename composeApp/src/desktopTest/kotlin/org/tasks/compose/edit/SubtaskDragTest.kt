package org.tasks.compose.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.entity.Task

@OptIn(ExperimentalTestApi::class)
class SubtaskDragTest {
    private fun row(
        title: String,
        indent: Int = 0,
        children: Int = 0,
        deleted: Boolean = false,
    ) = SubtaskRow(
        node = SubtaskNode(
            key = "uuid-$title",
            parentKey = "root",
            sequence = 0,
            task = Task(id = title.hashCode().toLong(), title = title, remoteId = "uuid-$title"),
            deleted = deleted,
        ),
        indent = indent,
        children = children,
    )

    private class Calls {
        val moves = mutableListOf<Triple<String, String, Int?>>()
        val indents = mutableListOf<Pair<String, Int>>()
    }

    @Composable
    private fun Section(subtasks: List<SubtaskRow>, calls: Calls, allowsNesting: Boolean = true) {
        MaterialTheme {
            SubtasksSection(
                subtasks = subtasks,
                focusSubtask = null,
                unsupportedMessage = null,
                flattenWarning = null,
                allowsNesting = allowsNesting,
                bottomInset = 0.dp,
                onAddSubtask = {}, onAddAfter = {}, onOpenSubtask = {}, onCompleteSubtask = {},
                onToggleCollapsed = {},
                onMoveSubtask = { from, to, indent -> calls.moves.add(Triple(from, to, indent)) },
                onIndentSubtask = { node, steps -> calls.indents.add(node.key to steps) },
                onTitleChange = { _, _ -> }, onRemoveSubtask = {}, onBackspaceSubtask = {},
                onRestoreSubtask = {}, onSubtaskFocused = {},
            )
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.drag(key: String, by: Offset) {
        onNodeWithTag("$DRAG_HANDLE_TAG$key").performMouseInput {
            moveTo(center)
            press()
            moveBy(by)
            release()
        }
        waitForIdle()
    }

    @Test
    fun draggingARowSidewaysNestsItWhereItStands() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(listOf(row("a"), row("b")), calls) }

        drag("uuid-b", Offset(40f, 0f))

        assertTrue(calls.moves.isEmpty())
        assertEquals(listOf("uuid-b" to 1), calls.indents)
    }

    @Test
    fun aRowCannotNestUnderNothing() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(listOf(row("a"), row("b")), calls) }

        drag("uuid-a", Offset(40f, 0f))

        assertTrue(calls.indents.isEmpty())
        assertTrue(calls.moves.isEmpty())
    }

    @Test
    fun aRowDraggedSidewaysOnAListThatWillNotNestStaysWhereItIs() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(listOf(row("a"), row("b")), calls, allowsNesting = false) }

        drag("uuid-b", Offset(40f, 0f))

        assertTrue(calls.indents.isEmpty())
    }

    @Test
    fun aRowDraggedOutFromUnderItsParentComesOut() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(listOf(row("a", children = 1), row("b", indent = 1)), calls) }

        drag("uuid-b", Offset(-40f, 0f))

        assertEquals(listOf("uuid-b" to -1), calls.indents)
    }

    @Test
    fun aDragThatGoesNowhereAsksForNothing() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(listOf(row("a"), row("b")), calls) }

        drag("uuid-b", Offset(2f, 2f))

        assertTrue(calls.moves.isEmpty())
        assertTrue(calls.indents.isEmpty())
    }

    @Test
    fun aDeletedRowHasNoDragHandle() = runComposeUiTest {
        setContent { Section(listOf(row("a"), row("b", deleted = true)), Calls()) }

        onNodeWithTag("${DRAG_HANDLE_TAG}uuid-a").assertExists()
        onNodeWithTag("${DRAG_HANDLE_TAG}uuid-b").assertDoesNotExist()
    }
}
