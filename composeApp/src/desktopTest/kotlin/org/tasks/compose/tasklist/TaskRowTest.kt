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

private const val DEFAULT_TITLE = "Buy milk"
private const val PLACE_NAME = "The Lab"
private const val REMOTE_ID = "uuid-1"
private const val FIRST_LINE = "line one"
private const val TWO_LINES = "line one\nline two"
private const val TWO_LINES_CLAMPED = "line one line two"
private const val THREE_LINES = "line one\nline two\nline three"
private const val THREE_LINES_CLAMPED = "line one line two line three"
private const val SHOW_MORE_LINK = "Show more"

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
        title: String = DEFAULT_TITLE,
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
            remoteId = REMOTE_ID,
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
        place = Place(name = PLACE_NAME, icon = "science"),
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
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(DEFAULT_TITLE).performClick()

        assertEquals(1, clicks.opened)
    }

    @Test
    fun aRowOnItsWayOutDoesNotOpen() = runComposeUiTest {
        val clicks = Clicks()
        setContent { Row(task(), doomed = true, clicks = clicks) }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(DEFAULT_TITLE).performClick()

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
        assertEquals(RowState.Doomed, rowState(mapOf(REMOTE_ID to true), task()))
    }

    @Test
    fun aRowGoingOnlyBecauseSomethingAboveItIsGoesOutOfSightAltogether() {
        assertEquals(RowState.Hidden, rowState(mapOf(REMOTE_ID to false), task()))
    }

    @Test
    fun aRowTooOldForAUuidIsRecognisedByItsRowId() {
        val old = TaskContainer(task = Task(id = 7, title = "Old", remoteId = null))

        assertEquals(RowState.Doomed, rowState(mapOf("id:7" to true), old))
        assertEquals(RowState.Draw, rowState(mapOf("id:8" to true), old))
    }

    @Test
    fun aClampedRowShowsTheWholeTitleOnOneLine() = runComposeUiTest {
        setContent { Row(task(title = TWO_LINES), doomed = false, clicks = Clicks()) }

        waitUntil { onAllNodes(hasText(TWO_LINES_CLAMPED)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(TWO_LINES_CLAMPED).assertExists()
    }

    @Test
    fun aFullTitleRowWrapsInsteadOfClampingToOneLine() = runComposeUiTest {
        val longTitle = ("word ".repeat(40)).trim()
        val full = TaskRowStyle(showFullTitle = true)

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

        onNodeWithText(PLACE_NAME).assertExists()
    }

    @Test
    fun aPlaceChipHidesWhenTheToggleIsOff() = runComposeUiTest {
        val style = TaskRowStyle(placeChips = false)

        setContent { Row(task(location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(PLACE_NAME).assertDoesNotExist()
    }

    @Test
    fun aTextOnlyRowKeepsTheChipTextButNotTheIconCount() = runComposeUiTest {
        val style = TaskRowStyle(
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY,
        )

        setContent { Row(task(children = 2, location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        // The place chip keeps its text in text-only mode.
        onNodeWithText(PLACE_NAME).assertExists()
    }

    @Test
    fun anIconOnlyRowDrawsNoChipText() = runComposeUiTest {
        val style = TaskRowStyle(
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY,
        )

        setContent { Row(task(children = 2, location = location()), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(PLACE_NAME).assertDoesNotExist()
    }

    @Test
    fun aSubtaskChipHidesWhenItsToggleIsOff() = runComposeUiTest {
        val style = TaskRowStyle(subtaskChips = false)

        setContent { Row(task(children = 2), doomed = false, clicks = Clicks(), style = style) }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun aLongDescriptionIsClampedToTwoLinesWithAShowMoreLink() = runComposeUiTest {
        setContent {
            Row(task(notes = THREE_LINES), doomed = false, clicks = Clicks())
        }
        waitUntil { onAllNodes(hasText(SHOW_MORE_LINK)).fetchSemanticsNodes().isNotEmpty() }

        // The markdown renderer joins the collapsed lines into one paragraph.
        onNodeWithText(TWO_LINES_CLAMPED).assertExists()
        onNodeWithText("line three").assertDoesNotExist()
    }

    @Test
    fun aLongDescriptionIsHiddenWhenDescriptionsAreOff() = runComposeUiTest {
        val style = TaskRowStyle(showDescription = false)

        setContent {
            Row(task(notes = THREE_LINES), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(FIRST_LINE).assertDoesNotExist()
        onNodeWithText(SHOW_MORE_LINK).assertDoesNotExist()
    }

    @Test
    fun aFullDescriptionIsExpandedByDefaultButCanFoldAway() = runComposeUiTest {
        val style = TaskRowStyle(showFullDescription = true)

        setContent {
            Row(task(notes = THREE_LINES), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil {
            onAllNodes(hasText(THREE_LINES_CLAMPED)).fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText(THREE_LINES_CLAMPED).assertExists()
        onNodeWithText("Show less").assertExists()

        onNodeWithText("Show less").performClick()
        waitUntil { onAllNodes(hasText(SHOW_MORE_LINK)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(THREE_LINES_CLAMPED).assertDoesNotExist()
        onNodeWithText(TWO_LINES_CLAMPED).assertExists()
    }

    @Test
    fun hiddenDescriptionsWinOverFullDescriptions() = runComposeUiTest {
        val style = TaskRowStyle(
            showDescription = false,
            showFullDescription = true,
        )

        setContent {
            Row(task(notes = FIRST_LINE), doomed = false, clicks = Clicks(), style = style)
        }
        waitUntil { onAllNodes(hasText(DEFAULT_TITLE)).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText(FIRST_LINE).assertDoesNotExist()
    }
}
