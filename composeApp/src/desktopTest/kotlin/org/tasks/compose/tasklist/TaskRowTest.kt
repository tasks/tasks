package org.tasks.compose.tasklist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
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

@OptIn(ExperimentalTestApi::class)
class TaskRowTest {
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

    private fun task(
        title: String = "Buy milk",
        children: Int = 0,
        uncompletedChildren: Int = children,
        completed: Boolean = false,
        indent: Int = 0,
    ) = TaskContainer(
        task = Task(
            id = 1,
            title = title,
            remoteId = "uuid-1",
            completionDate = if (completed) 1 else 0,
        ),
        children = children,
        uncompletedChildren = uncompletedChildren,
        indent = indent,
    )

    private class Clicks {
        var opened = 0
        var completed = 0
        var foldedAway = 0
    }

    @Composable
    private fun Row(task: TaskContainer, doomed: Boolean, clicks: Clicks) {
        MaterialTheme {
            TaskRow(
                task = task,
                doomed = doomed,
                filter = EmptyFilter(),
                groupMode = 0,
                chipDataProvider = chips,
                is24Hour = false,
                dateFormatter = null,
                onClick = { clicks.opened++ },
                onToggleComplete = { clicks.completed++ },
                onToggleSubtasks = { clicks.foldedAway++ },
                onFilterClick = {},
            )
        }
    }

    @Test
    fun anOrdinaryRowOpensWhenItIsClicked() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(), doomed = false, clicks = clicks) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("Buy milk").performClick()

        assertEquals(1, clicks.opened)
    }

    @Test
    fun aRowOnItsWayOutDoesNotOpen() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(), doomed = true, clicks = clicks) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("Buy milk").performClick()

        assertEquals(0, clicks.opened)
    }

    @Test
    fun anOrdinaryRowCanBeTickedOff() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(), doomed = false, clicks = clicks) }

        onNodeWithTag(COMPLETE_BUTTON_TAG).performClick()

        assertEquals(1, clicks.completed)
    }

    @Test
    fun aRowOnItsWayOutCannotBeTickedOff() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(), doomed = true, clicks = clicks) }

        onNodeWithTag(COMPLETE_BUTTON_TAG).assertIsNotEnabled().performClick()

        assertEquals(0, clicks.completed)
    }

    @Test
    fun anOrdinaryRowWithSubtasksCanFoldThemAway() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(children = 2), doomed = false, clicks = clicks) }

        onNodeWithText("2").assertIsEnabled().performClick()

        assertEquals(1, clicks.foldedAway)
    }

    @Test
    fun theSubtaskChipCountsWhatIsLeftToDo() = runComposeUiTest {
        setContent {
            Row(task(children = 13, uncompletedChildren = 5), doomed = false, clicks = Clicks())
        }

        onNodeWithText("5").assertIsEnabled()
    }

    @Test
    fun aRowWithNothingLeftToDoKeepsItsChip() = runComposeUiTest {
        val clicks = Clicks()
        setContent {
            Row(task(children = 3, uncompletedChildren = 0), doomed = false, clicks = clicks)
        }

        onNodeWithText("0").assertIsEnabled().performClick()

        assertEquals(1, clicks.foldedAway)
    }

    @Test
    fun aTickedOffRowCountsWhatIsUnderItInstead() = runComposeUiTest {
        setContent {
            Row(
                task(children = 13, uncompletedChildren = 0, completed = true),
                doomed = false,
                clicks = Clicks(),
            )
        }

        onNodeWithText("13").assertIsEnabled()
    }

    @Test
    fun aRowOnItsWayOutStillCountsItsSubtasksButWillNotFoldThem() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(children = 2), doomed = true, clicks = clicks) }

        onNodeWithText("2").assertIsNotEnabled()

        assertEquals(0, clicks.foldedAway)
    }

    @Test
    fun aRowNoOpenEditorHasMarkedIsDrawnAsItAlwaysWas() {
        assertEquals(RowState.Draw, rowState(emptyMap(), task()))
    }

    @Test
    fun aRowTheUserMarkedIsDrawnAsBeingOnItsWayOut() {
        assertEquals(RowState.Doomed, rowState(mapOf("uuid-1" to true), task()))
    }

    @Test
    fun aRowGoingOnlyBecauseSomethingAboveItIsGoesOutOfSightAltogether() {
        assertEquals(RowState.Hidden, rowState(mapOf("uuid-1" to false), task()))
    }

    @Test
    fun aRowTooOldForAUuidIsRecognisedByItsRowId() {
        val old = TaskContainer(task = Task(id = 7, title = "Old", remoteId = null))

        assertEquals(RowState.Doomed, rowState(mapOf("id:7" to true), old))
        assertEquals(RowState.Draw, rowState(mapOf("id:8" to true), old))
    }
}
