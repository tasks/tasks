package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.flow.emptyFlow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavListCache
import org.tasks.filters.EmptyFilter

/**
 * Verifies the M3 single-selection list-item wiring end to end at the row level: the
 * selected row reports the selected state, an unselected row does not, the selection
 * follows whatever `selected` value the list hands down, and a selected row still opens
 * on click. These come from the `ListItem(selected, onClick)` overload — not a custom
 * tag — so this is what a screen reader and the list's own logic see.
 */
@OptIn(ExperimentalTestApi::class)
class TaskRowSelectionTest {
    private val firstTitle = "First"
    private val secondTitle = "Second"

    private val chips = ChipDataProvider(
        caldavLists = CaldavListCache(
            mock {
                on { watchAccounts() } doReturn emptyFlow()
                on { subscribeToCalendars() } doReturn emptyFlow()
            }
        ),
        tagDataDao = mock { on { subscribeToTags() } doReturn emptyFlow() },
        refreshBroadcaster = mock(),
    )

    private fun task(id: Long, title: String) = TaskContainer(
        task = Task(
            id = id,
            title = title,
            remoteId = "uuid-$id",
        ),
        children = 0,
        uncompletedChildren = 0,
        indent = 0,
    )

    @Composable
    private fun TwoRows(selectedId: Long) {
        MaterialTheme {
            Column {
                TaskRow(
                    task = task(1, firstTitle),
                    selected = selectedId == 1L,
                    doomed = false,
                    filter = EmptyFilter(),
                    groupMode = 0,
                    chipDataProvider = chips,
                    is24Hour = false,
                    dateFormatter = null,
                    onClick = {},
                    onToggleComplete = {},
                    onToggleSubtasks = {},
                    onFilterClick = {},
                )
                TaskRow(
                    task = task(2, secondTitle),
                    selected = selectedId == 2L,
                    doomed = false,
                    filter = EmptyFilter(),
                    groupMode = 0,
                    chipDataProvider = chips,
                    is24Hour = false,
                    dateFormatter = null,
                    onClick = {},
                    onToggleComplete = {},
                    onToggleSubtasks = {},
                    onFilterClick = {},
                )
            }
        }
    }

    @Test
    fun bothRowsArePresentAndOnlyOneIsSelected() = runComposeUiTest {
        setContent { TwoRows(selectedId = 1L) }
        // Two distinct titles rendered, each resolvable as its own list item.
        onAllNodesWithText(firstTitle).assertCountEquals(1)
        onAllNodesWithText(secondTitle).assertCountEquals(1)

        onNodeWithText(firstTitle).assertIsSelected()
        onNodeWithText(secondTitle).assertIsNotSelected()
    }

    @Test
    fun theSelectionFollowsTheValuePassedToTheRow() = runComposeUiTest {
        setContent { TwoRows(selectedId = 2L) }
        onNodeWithText(firstTitle).assertIsNotSelected()
        onNodeWithText(secondTitle).assertIsSelected()
    }

    @Test
    fun aSelectedRowStillOpensWhenClicked() = runComposeUiTest {
        var opened = 0
        setContent {
            MaterialTheme {
                TaskRow(
                    task = task(1, firstTitle),
                    selected = true,
                    doomed = false,
                    filter = EmptyFilter(),
                    groupMode = 0,
                    chipDataProvider = chips,
                    is24Hour = false,
                    dateFormatter = null,
                    onClick = { opened++ },
                    onToggleComplete = {},
                    onToggleSubtasks = {},
                    onFilterClick = {},
                )
            }
        }
        onNodeWithText(firstTitle).assertIsSelected()
        onNodeWithText(firstTitle).performClick()
        assertEquals(1, opened)
    }
}
