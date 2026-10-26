package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.emptyFlow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.Location
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavListCache
import org.tasks.filters.EmptyFilter
import org.tasks.viewmodel.TaskListPreferencesViewModel

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
        location: Location? = null,
        notes: String? = null,
    ) = TaskContainer(
        task = Task(
            id = 1,
            title = title,
            remoteId = "uuid-1",
            completionDate = if (completed) 1 else 0,
            notes = notes,
        ),
        children = children,
        uncompletedChildren = uncompletedChildren,
        indent = indent,
        location = location,
    )

    private fun location() = Location(
        geofence = Geofence(task = 1),
        place = Place(name = "The Lab", icon = "science"),
    )

    private class Clicks {
        var opened = 0
        var completed = 0
        var foldedAway = 0
    }

    @Composable
    private fun Row(
        task: TaskContainer,
        doomed: Boolean,
        clicks: Clicks,
        style: TaskRowStyle = DEFAULT_TASK_ROW_STYLE,
    ) {
        MaterialTheme {
            var expanded by remember { mutableStateOf(style.showFullDescription) }
            TaskRow(
                task = task,
                doomed = doomed,
                filter = EmptyFilter(),
                groupMode = 0,
                chipDataProvider = chips,
                style = style,
                is24Hour = false,
                dateFormatter = null,
                onClick = { clicks.opened++ },
                onToggleComplete = { clicks.completed++ },
                onToggleSubtasks = { clicks.foldedAway++ },
                onFilterClick = {},
                descriptionExpanded = expanded,
                onToggleDescriptionExpanded = { expanded = !expanded },
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

    @Test
    fun aClampedRowShowsTheWholeTitleOnOneLine() = runComposeUiTest {
        setContent { Row(task(title = "line one\nline two"), doomed = false, clicks = Clicks()) }

        waitUntil { onAllNodes(hasText("line one line two")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("line one line two").assertExists()
    }

    @Test
    fun aFullTitleRowWrapsInsteadOfClampingToOneLine() = runComposeUiTest {
        val longTitle = ("word ".repeat(40)).trim()
        val full = taskRowStyle(fontSize = 16, rowSpacingDp = 16, showFullTitle = true)

        setContent {
            Box(Modifier.width(240.dp)) {
                Row(task(title = longTitle), doomed = false, clicks = Clicks())
            }
        }
        waitUntil { onAllNodes(hasText(longTitle)).fetchSemanticsNodes().isNotEmpty() }
        val clampedHeight = onNodeWithText(longTitle).fetchSemanticsNode().size.height

        setContent {
            Box(Modifier.width(240.dp)) {
                Row(task(title = longTitle), doomed = false, clicks = Clicks(), style = full)
            }
        }
        waitUntil { onAllNodes(hasText(longTitle)).fetchSemanticsNodes().isNotEmpty() }
        val fullHeight = onNodeWithText(longTitle).fetchSemanticsNode().size.height

        assertTrue(
            "full title should wrap to more lines than the clamped single line " +
                    "(clamped=$clampedHeight full=$fullHeight)",
            fullHeight > clampedHeight,
        )
    }

    @Test
    fun aPlaceChipShowsByDefault() = runComposeUiTest {
        setContent { Row(task(location = location()), doomed = false, clicks = Clicks()) }

        onNodeWithText("The Lab").assertExists()
    }

    @Test
    fun aPlaceChipHidesWhenTheToggleIsOff() = runComposeUiTest {
        val style = taskRowStyle(fontSize = 16, rowSpacingDp = 16, placeChips = false)

        setContent { Row(task(location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("The Lab").assertDoesNotExist()
    }

    @Test
    fun aTextOnlyRowKeepsTheChipTextButNotTheIconCount() = runComposeUiTest {
        val style = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY,
        )

        setContent { Row(task(children = 2, location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        // The place chip keeps its text in text-only mode.
        onNodeWithText("The Lab").assertExists()
    }

    @Test
    fun anIconOnlyRowDrawsNoChipText() = runComposeUiTest {
        val style = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY,
        )

        setContent { Row(task(children = 2, location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("The Lab").assertDoesNotExist()
    }

    @Test
    fun aSubtaskChipHidesWhenItsToggleIsOff() = runComposeUiTest {
        val style = taskRowStyle(fontSize = 16, rowSpacingDp = 16, subtaskChips = false)

        setContent { Row(task(children = 2), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun aLongDescriptionIsClampedToTwoLinesWithAShowMoreLink() = runComposeUiTest {
        setContent {
            Row(task(notes = "line one\nline two\nline three"), doomed = false, clicks = Clicks())
        }
        waitUntil { onAllNodes(hasText("Show more")).fetchSemanticsNodes().isNotEmpty() }

        // The markdown renderer joins the collapsed lines into one paragraph.
        onNodeWithText("line one line two").assertExists()
        onNodeWithText("line three").assertDoesNotExist()
    }

    @Test
    fun aLongDescriptionIsHiddenWhenDescriptionsAreOff() = runComposeUiTest {
        val style = taskRowStyle(fontSize = 16, rowSpacingDp = 16, showDescription = false)

        setContent {
            Row(task(notes = "line one\nline two\nline three"), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("line one").assertDoesNotExist()
        onNodeWithText("Show more").assertDoesNotExist()
    }

    @Test
    fun aFullDescriptionIsExpandedByDefaultButCanFoldAway() = runComposeUiTest {
        val style = taskRowStyle(fontSize = 16, rowSpacingDp = 16, showFullDescription = true)

        setContent {
            Row(task(notes = "line one\nline two\nline three"), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil {
            onAllNodes(hasText("line one line two line three")).fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText("line one line two line three").assertExists()
        onNodeWithText("Show less").assertExists()

        onNodeWithText("Show less").performClick()
        waitUntil { onAllNodes(hasText("Show more")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("line one line two line three").assertDoesNotExist()
        onNodeWithText("line one line two").assertExists()
    }

    @Test
    fun hiddenDescriptionsWinOverFullDescriptions() = runComposeUiTest {
        val style = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            showDescription = false,
            showFullDescription = true,
        )

        setContent {
            Row(task(notes = "line one"), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil { onAllNodes(hasText("Buy milk")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("line one").assertDoesNotExist()
    }
}
