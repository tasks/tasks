package org.tasks.compose.edit

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key

import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MakeParentTaskPanelTest {

    private val accepted = mutableListOf<String>()
    private var cancelled = 0

    @Composable
    private fun panel(working: Boolean = false) {
        MakeParentTaskPanel(
            working = working,
            onAccept = { accepted.add(it) },
            onCancel = { cancelled++ },
        )
    }

    @Test
    fun acceptIsDisabledUntilThereIsATitle() = runComposeUiTest {
        setContent { panel() }
        val accept = onNodeWithContentDescription("Move into new parent task")
        val cancel = onNodeWithContentDescription("Cancel")

        accept.assertIsNotEnabled()
        cancel.assertIsEnabled()

        onNodeWithText("New parent task").performTextInput("Project")
        accept.assertIsEnabled()
        accept.performClick()
        assertEquals(listOf("Project"), accepted)
    }

    @Test
    fun theTitleIsTrimmedBeforeItIsAccepted() = runComposeUiTest {
        setContent { panel() }
        onNodeWithText("New parent task").performTextInput("  Padded  ")
        onNodeWithContentDescription("Move into new parent task").performClick()
        assertEquals(listOf("Padded"), accepted)
    }

    @Test
    fun enterAcceptsAndEscapeCancels() = runComposeUiTest {
        setContent { panel() }
        onNodeWithText("New parent task")
            .performKeyInput { keyDown(Key.Escape) }
        assertEquals(1, cancelled)

        onNodeWithText("New parent task").performTextInput("Project")
        onNodeWithText("Project")
            .performKeyInput { keyDown(Key.Enter) }
        assertEquals(listOf("Project"), accepted)
    }

    @Test
    fun aWorkingPanelTakesNoInput() = runComposeUiTest {
        setContent { panel(working = true) }

        onNodeWithText("New parent task").assertIsNotEnabled()
        onNodeWithContentDescription("Move into new parent task").assertIsNotEnabled()
        // Nothing is left to cancel while create/nest is in flight; the op is a fast DB
        // write and the button re-enables when the working flag clears.
        onNodeWithContentDescription("Cancel").assertIsNotEnabled()
    }

    @Test
    fun thePanelRendersExactlyOneInput() = runComposeUiTest {
        setContent { panel() }
        assertEquals(1, onAllNodes(isEditable()).fetchSemanticsNodes().size)
    }
}
