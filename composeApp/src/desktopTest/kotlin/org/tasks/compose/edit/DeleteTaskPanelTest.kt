package org.tasks.compose.edit

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key

import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DeleteTaskPanelTest {

    private var deleted = 0
    private var cancelled = 0

    // Three deletable rows (root, child, done child) plus one read-only row
    // that the deletion skips — so the label counts "3", not 4.
    private val rows = listOf(
        DeletePreviewRow(title = "root"),
        DeletePreviewRow(title = "child", indent = 1),
        DeletePreviewRow(title = "done child", indent = 1, completed = true),
        DeletePreviewRow(title = "shared", indent = 1, readOnly = true),
    )

    @Composable
    private fun panel(working: Boolean = false) {
        DeleteTaskPanel(
            rows = rows,
            working = working,
            onDelete = { deleted++ },
            onCancel = { cancelled++ },
        )
    }

    @Test
    fun theWholeTreeIsListedWithItsAnnotations() = runComposeUiTest {
        setContent { panel() }

        onNodeWithText("These tasks will be deleted").assertIsDisplayed()
        onNodeWithText("root").assertIsDisplayed()
        onNodeWithText("child", substring = false).assertIsDisplayed()
        onNodeWithText("done child").assertIsDisplayed()
        onNodeWithText("shared").assertIsDisplayed()
        // Exactly one row (the read-only one) carries the "not deleted"
        // annotation.
        onAllNodesWithText("protected — not deleted").assertCountEquals(1)
    }

    @Test
    fun theDeleteButtonCountsTheDeletableRowsAndCancelsLeftOfIt() = runComposeUiTest {
        setContent { panel() }

        // 3 deletable rows → plural label; the read-only row is not counted.
        onNodeWithText("Delete 3 tasks").performClick()
        assertEquals(1, deleted)
        assertEquals(0, cancelled)

        // Cancel is a labelled button, not a bare cross.
        onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun aSingleTaskGetsTheSingularLabel() = runComposeUiTest {
        setContent {
            DeleteTaskPanel(
                rows = listOf(DeletePreviewRow(title = "solo")),
                working = false,
                onDelete = { deleted++ },
                onCancel = { cancelled++ },
            )
        }
        onNodeWithText("Delete task").assertIsEnabled()
        onNodeWithText("Delete 3 tasks").assertDoesNotExist()
    }

    @Test
    fun enterDeletesAndEscapeCancels() = runComposeUiTest {
        setContent { panel() }

        onNodeWithText("child", substring = false).performKeyInput { keyDown(Key.Escape) }
        assertEquals(1, cancelled)

        onNodeWithText("child", substring = false).performKeyInput { keyDown(Key.Enter) }
        assertEquals(1, deleted)
    }

    @Test
    fun aWorkingPanelTakesNoInput() = runComposeUiTest {
        setContent { panel(working = true) }

        onNodeWithText("Delete 3 tasks").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun anEmptyPreviewStillListsTheTaskItself() = runComposeUiTest {
        // A fetch failure degrades to a single row (the task's own title) rather
        // than an empty panel with nothing to confirm.
        setContent {
            DeleteTaskPanel(
                rows = listOf(DeletePreviewRow(title = "solo")),
                working = false,
                onDelete = { deleted++ },
                onCancel = { cancelled++ },
            )
        }
        onNodeWithText("These tasks will be deleted").assertIsDisplayed()
        onNodeWithText("solo").assertIsDisplayed()
        onNodeWithText("Delete task").assertIsEnabled()
    }
}
