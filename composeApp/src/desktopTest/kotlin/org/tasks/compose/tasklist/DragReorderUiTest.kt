package org.tasks.compose.tasklist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavListCache
import org.tasks.filters.EmptyFilter
import org.tasks.tasklist.SectionedDataSource
import kotlinx.coroutines.flow.emptyFlow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Drives the real gesture path: press the drag handle, drag vertically, release. The
 * list reports the (taskId, dropIndex) a drop would commit; the test captures that
 * pair and asserts the pointer mapping, not the database (that is covered by
 * ManualSortMoveTest and the reorderer's CalDAV round-trip).
 */
@OptIn(ExperimentalTestApi::class)
class DragReorderUiTest {
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

    private fun task(id: Long, title: String, order: Long, indent: Int = 0) = TaskContainer(
        task = Task(id = id, title = title, remoteId = "uuid-$id", parent = 0),
        primarySort = order,
        indent = indent,
    )

    private fun list() = SectionedDataSource(
        tasks = listOf(
            task(1, "A", 1),
            task(2, "B", 2),
            task(3, "C", 3),
        ),
        disableHeaders = true,
    )

    private class Dropped {
        var taskId: Long? = null
        var dropIndex: Int = -1
        var reparentTaskId: Long? = null
        var reparentTargetId: Long? = null
        var outdentTaskId: Long? = null
        var outdentDropIndex: Int = -1
    }

    @Composable
    private fun List(
        dropped: Dropped,
        dragEnabled: Boolean = true,
        tasks: SectionedDataSource = list(),
    ) {
        MaterialTheme {
            TaskList(
                tasks = tasks,
                filter = EmptyFilter(),
                chipDataProvider = chips,
                dragEnabled = dragEnabled,
                onTaskDrop = { taskId, dropIndex ->
                    dropped.taskId = taskId
                    dropped.dropIndex = dropIndex
                },
                onTaskReparent = { taskId, targetTaskId ->
                    dropped.reparentTaskId = taskId
                    dropped.reparentTargetId = targetTaskId
                },
                onTaskOutdent = { taskId, dropIndex ->
                    dropped.outdentTaskId = taskId
                    dropped.outdentDropIndex = dropIndex
                },
                onTaskClick = {},
                onCompleteTask = { _, _ -> },
            )
        }
    }

    private fun runWithKoin(block: () -> Unit) {
        startKoin { modules(module { single { SubtaskTreeRegistry() } }) }
        try {
            block()
        } finally {
            stopKoin()
        }
    }

    /**
     * The centre of the handle at [y]: the semantics tree reorders when a dragged
     * item gains zIndex, so mid-drag a row is tracked by its (stable, for
     * horizontal drags) y coordinate rather than by list index.
     */
    private fun atY(handles: SemanticsNodeInteractionCollection, y: Float): Offset {
        val count = try {
            handles.assertCountEquals(6); 6
        } catch (e: AssertionError) {
            3
        }
        var best = handles[0].center()
        var bestDist = Float.MAX_VALUE
        for (i in 0 until count) {
            val p = handles[i].center()
            val d = kotlin.math.abs(p.y - y)
            if (d < bestDist) {
                bestDist = d
                best = p
            }
        }
        return best
    }

    @Test
    fun dragDisabledHidesTheHandles() = runComposeUiTest {
        runWithKoin {
            setContent { List(dropped = Dropped(), dragEnabled = false) }
            onAllNodesWithTag(DRAG_HANDLE_TAG).assertCountEquals(0)
        }
    }

    @Test
    fun dragEnabledShowsOneHandlePerRow() = runComposeUiTest {
        runWithKoin {
            setContent { List(dropped = Dropped()) }
            onAllNodesWithTag(DRAG_HANDLE_TAG).assertCountEquals(3)
            onAllNodesWithTag(DRAG_HANDLE_TAG)[0].assertIsDisplayed()
        }
    }

    @Test
    fun draggingTheBottomRowOntoTheTopDropsAtRowZero() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            handles[2].dragTo(handles[0])
            assertEquals(3L, dropped.taskId)
            assertEquals(0, dropped.dropIndex)
        }
    }

    @Test
    fun draggingTheTopRowDownOntoTheBottomDropsAtTheLastRow() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            handles[0].dragTo(handles[2])
            assertEquals(1L, dropped.taskId)
            assertEquals(2, dropped.dropIndex)
        }
    }

    @Test
    fun anIndentedDragReparentsOntoTheNeighbour() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            val dy = handles[2].center().y - handles[0].center().y
            // More than half an indent step (10px at test density) to the right of the
            // grab point: the drop makes the task a subtask of the nearest row above
            // the settled position - here, the row it landed below.
            handles[0].performTouchInput {
                down(center)
                moveTo(center + Offset(30f, dy))
                up()
            }
            assertEquals(1L, dropped.reparentTaskId)
            assertEquals(3L, dropped.reparentTargetId)
            assertNull(dropped.taskId)
        }
    }

    @Test
    fun anIndentedDragShiftsTheDraggedRowRightMidDrag() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            // The handle sits inside the row's animated start padding, so its
            // centre tracks the indent shift. Track the dragged handle by its y
            // position rather than list index: the dragged item gains zIndex
            // mid-drag, which reorders the semantics tree and would make an
            // index-based selector resolve to a different row after the drag.
            val before = handles[1].center()
            // Drag B (row 1) to the right without releasing: past the half-step
            // threshold the dragged row indents by one step - the native app's
            // "about to become a subtask" cue (there is no colour highlight).
            // Two moves: the first one starts the drag, the second lets the
            // list-level observer compute the live indent level.
            handles[1].performTouchInput {
                down(center)
                moveTo(center + Offset(30f, 0f))
                moveTo(center + Offset(35f, 0f))
            }
            // Let the observed pointer position animate the row's indent.
            waitForIdle()
            mainClock.advanceTimeBy(2000)
            waitForIdle()
            // The drag is horizontal, so the dragged row keeps its y.
            val after = atY(handles, before.y)
            assertTrue(
                "dragged row should indent one step right (20px at test density), was ${after.x - before.x}px",
                after.x - before.x >= 15f,
            )
            handles[1].performTouchInput {
                up()
            }
            assertEquals(2L, dropped.reparentTaskId)
            assertEquals(1L, dropped.reparentTargetId)
            assertNull(dropped.taskId)
        }
    }

    @Test
    fun anIndentedDragWithNoVerticalTravelReparentsInPlace() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            // The two-item case in full: B dragged right, never moved vertically.
            handles[1].performTouchInput {
                down(center)
                moveTo(center + Offset(30f, 0f))
                up()
            }
            assertEquals(2L, dropped.reparentTaskId)
            assertEquals(1L, dropped.reparentTargetId)
            assertNull(dropped.taskId)
        }
    }

    @Test
    fun anOutdentedDragShiftsTheDraggedRowLeftMidDrag() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            val tasks = SectionedDataSource(
                tasks = listOf(
                    task(1, "A", 1),
                    task(2, "B", 2),
                    task(3, "C", 3, indent = 1),
                ),
                disableHeaders = true,
            )
            setContent { List(dropped = dropped, tasks = tasks) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            // Track the dragged handle by y position (see the indent test for why
            // an index-based selector is unreliable mid-drag).
            val before = handles[2].center()
            // Drag the indented task C to the left without releasing: past the
            // half-step threshold the dragged row un-indents by one step -
            // the "about to be promoted" cue (the mirror of the indent shift).
            handles[2].performTouchInput {
                down(center)
                moveTo(center + Offset(-30f, 0f))
                moveTo(center + Offset(-35f, 0f))
            }
            waitForIdle()
            mainClock.advanceTimeBy(2000)
            waitForIdle()
            val after = atY(handles, before.y)
            assertTrue(
                "dragged row should un-indent one step left (20px at test density), was ${after.x - before.x}px",
                before.x - after.x >= 15f,
            )
            handles[2].performTouchInput {
                up()
            }
            assertEquals(3L, dropped.outdentTaskId)
            assertEquals(2, dropped.outdentDropIndex)
            assertNull(dropped.taskId)
        }
    }

    @Test
    fun anOutdentedDragMovesTheSubtaskToTheTopLevel() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            val nested = SectionedDataSource(
                tasks = listOf(
                    task(1, "A", 1),
                    TaskContainer(
                        task = Task(id = 2, title = "B", remoteId = "uuid-2", parent = 1),
                        primarySort = 2,
                        indent = 1,
                    ),
                ),
                disableHeaders = true,
            )
            setContent { List(dropped = dropped, tasks = nested) }
            val handles = onAllNodesWithTag(DRAG_HANDLE_TAG)
            // Drag the subtask B left of the grab point: it outdents to the top
            // level, staying in its own slot.
            handles[1].performTouchInput {
                down(center)
                moveTo(center + Offset(-30f, 0f))
                up()
            }
            assertEquals(2L, dropped.outdentTaskId)
            assertEquals(1, dropped.outdentDropIndex)
            assertNull(dropped.taskId)
            assertNull(dropped.reparentTaskId)
        }
    }

    @Test
    fun aTinyDragDoesNotCommit() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handle = onAllNodesWithTag(DRAG_HANDLE_TAG)[0]
            // Below the touch slop: the gesture is never promoted to a drag.
            handle.performTouchInput {
                down(center)
                moveTo(center + Offset(0f, 2f))
                up()
            }
            assertNull(dropped.taskId)
            assertEquals(-1, dropped.dropIndex)
        }
    }

    @Test
    fun aReleaseBackOnTheDraggedRowIsANoop() = runComposeUiTest {
        runWithKoin {
            val dropped = Dropped()
            setContent { List(dropped = dropped) }
            val handle = onAllNodesWithTag(DRAG_HANDLE_TAG)[0]
            // Drag well past the slop and back onto the dragged row's own slot.
            handle.performTouchInput {
                down(center)
                moveTo(center + Offset(0f, 400f))
                moveTo(center)
                up()
            }
            assertNull(dropped.taskId)
        }
    }
}

private fun SemanticsNodeInteraction.center(): Offset = fetchSemanticsNode().boundsInRoot.center

private fun SemanticsNodeInteraction.dragTo(other: SemanticsNodeInteraction) {
    val dy = other.center().y - center().y
    performTouchInput {
        down(center)
        moveTo(center + Offset(0f, dy))
        up()
    }
}
