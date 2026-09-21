package org.tasks.compose.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.entity.Task

@OptIn(ExperimentalTestApi::class)
class SubtasksSectionTest {
    private fun node(title: String, key: String) = SubtaskNode(
        key = key,
        parentKey = "root",
        sequence = 0,
        task = Task(id = 1, title = title, remoteId = key),
    )

    private fun row(title: String, key: String = "uuid-$title") =
        SubtaskRow(node = node(title, key), indent = 0)

    private class Calls {
        val addedAfter = mutableListOf<String>()
        val backspaced = mutableListOf<String>()
        val titles = mutableListOf<Pair<String, String>>()
    }

    @Composable
    private fun Section(
        subtasks: List<SubtaskRow>,
        calls: Calls,
        unsupportedMessage: String? = null,
        flattenWarning: String? = null,
        focusSubtask: String? = null,
    ) {
        MaterialTheme {
            SubtasksSection(
                subtasks = subtasks,
                focusSubtask = focusSubtask,
                unsupportedMessage = unsupportedMessage,
                flattenWarning = flattenWarning,
                allowsNesting = true,
                bottomInset = 0.dp,
                onAddSubtask = {},
                onAddAfter = { calls.addedAfter.add(it.key) },
                onOpenSubtask = {},
                onCompleteSubtask = {},
                onToggleCollapsed = {},
                onMoveSubtask = { _, _, _ -> },
                onIndentSubtask = { _, _ -> },
                onTitleChange = { node, title -> calls.titles.add(node.key to title) },
                onRemoveSubtask = {},
                onBackspaceSubtask = { calls.backspaced.add(it.key) },
                onRestoreSubtask = {},
                onSubtaskFocused = {},
            )
        }
    }

    @Test
    fun enterAddsTheNextSubtask() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("buy milk")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(listOf("uuid-buy milk"), calls.addedAfter)
    }

    @Test
    fun enterOnAnEmptyTitleAddsNothing() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("", key = "uuid-blank")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(calls.addedAfter.isEmpty())
    }

    @Test
    fun shiftEnterIsLeftToTheField() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("buy milk")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()

        assertTrue(calls.addedAfter.isEmpty())
    }

    @Test
    fun enterAddsNothingWhenTheListWillNotHoldAnotherLevel() = runComposeUiTest {
        val calls = Calls()
        setContent {
            Section(
                subtasks = listOf(row("buy milk")),
                calls = calls,
                unsupportedMessage = "This list keeps one level of subtasks",
            )
        }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(calls.addedAfter.isEmpty())
    }

    @Test
    fun backspaceOnAnEmptyTitleTakesTheRowWithIt() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("", key = "uuid-blank")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals(listOf("uuid-blank"), calls.backspaced)
    }

    @Test
    fun backspaceInATitleWithTextInItIsJustBackspace() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("buy milk")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertTrue(calls.backspaced.isEmpty())
    }

    @Test
    fun typingIsPushedOutOfTheRow() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("", key = "uuid-blank")), calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performTextInput("ab")
        waitForIdle()

        assertEquals("uuid-blank" to "ab", calls.titles.last())
    }

    @Test
    fun aTitleBeingTypedIntoIsNotOverwrittenByItsOwnEchoComingBack() = runComposeUiTest {
        val calls = Calls()
        var rows by mutableStateOf(listOf(row("", key = "uuid-blank")))
        setContent { Section(subtasks = rows, calls = calls) }

        val field = onAllNodes(hasSetTextAction())[0]
        field.performClick()
        field.performTextInput("ab")
        waitForIdle()
        rows = listOf(row("a", key = "uuid-blank"))
        waitForIdle()

        field.assert(hasText("ab"))
    }

    @Test
    fun aRenameFromSomewhereElseIsTakenUpWhileNothingIsBeingTyped() = runComposeUiTest {
        val calls = Calls()
        var rows by mutableStateOf(listOf(row("a", key = "uuid-1")))
        setContent { Section(subtasks = rows, calls = calls) }

        rows = listOf(row("renamed", key = "uuid-1"))
        waitForIdle()

        onAllNodes(hasSetTextAction())[0].assert(hasText("renamed"))
    }

    @Test
    fun aRowOnItsWayOutHasNothingToTypeInto() = runComposeUiTest {
        val calls = Calls()
        val doomed = SubtaskRow(node = node("gone", "uuid-gone").copy(deleted = true), indent = 0)
        setContent { Section(subtasks = listOf(doomed), calls = calls) }
        waitForIdle()

        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun theArrowsMoveTheCaretToTheNextRow() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("a"), row("b")), calls = calls) }

        val fields = onAllNodes(hasSetTextAction())
        fields[0].performClick()
        fields[0].performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()

        fields[1].assertIsFocused()
    }

    @Test
    fun theArrowsComeBackUpAgain() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("a"), row("b")), calls = calls) }

        val fields = onAllNodes(hasSetTextAction())
        fields[1].performClick()
        fields[1].performKeyInput { pressKey(Key.DirectionUp) }
        waitForIdle()

        fields[0].assertIsFocused()
    }

    @Test
    fun anArrowWithNothingThatWayLeavesTheCaretWhereItIs() = runComposeUiTest {
        val calls = Calls()
        setContent { Section(subtasks = listOf(row("a"), row("b")), calls = calls) }

        val fields = onAllNodes(hasSetTextAction())
        fields[0].performClick()
        fields[0].performKeyInput { pressKey(Key.DirectionUp) }
        waitForIdle()

        fields[0].assertIsFocused()
    }

    @Test
    fun theRowTheListJustAddedTakesTheCaret() = runComposeUiTest {
        val calls = Calls()
        setContent {
            Section(
                subtasks = listOf(row("a"), row("b")),
                calls = calls,
                focusSubtask = "uuid-b",
            )
        }
        waitForIdle()

        onAllNodes(hasSetTextAction())[1].assertIsFocused()
    }

    @Test
    fun aListThatWillNotHoldSubtasksThisDeepStillDrawsTheOnesAlreadyThere() {
        val calls = Calls()
        runComposeUiTest {
            setContent {
                Section(
                    subtasks = listOf(row("kept")),
                    calls = calls,
                    unsupportedMessage = "no room for these",
                )
            }

            onNode(hasText("no room for these")).assertExists()
            onNode(hasSetTextAction()).performTextInput("!")
            waitForIdle()

            assertEquals("uuid-kept" to "kept!", calls.titles.last())
        }
    }

    @Test
    fun noFlattenWarningIsShownWhenThereIsNoneToShow() {
        val calls = Calls()
        val nested = SubtaskRow(node = node("nested", "uuid-nested"), indent = 1)
        runComposeUiTest {
            setContent {
                Section(
                    subtasks = listOf(row("top"), nested),
                    calls = calls,
                    flattenWarning = null,
                )
            }

            onNode(hasText("these will be flattened")).assertDoesNotExist()
            assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun theFlattenWarningIsShownAboveRowsThatLookFlat() {
        val calls = Calls()
        runComposeUiTest {
            setContent {
                Section(
                    subtasks = listOf(row("collapsed parent")),
                    calls = calls,
                    flattenWarning = "these will be flattened",
                )
            }

            onNode(hasText("these will be flattened")).assertExists()
        }
    }
}
