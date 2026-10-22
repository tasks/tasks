package org.tasks.compose.tasklist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun task(id: Long, title: String, order: Long) = TaskContainer(
        task = Task(id = id, title = title, remoteId = "uuid-$id", parent = 0),
        primarySort = order,
        indent = 0,
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
    }

    @Composable
    private fun List(
        dropped: Dropped,
        dragEnabled: Boolean = true,
    ) {
        MaterialTheme {
            TaskList(
                tasks = list(),
                filter = EmptyFilter(),
                chipDataProvider = chips,
                dragEnabled = dragEnabled,
                onTaskDrop = { taskId, dropIndex ->
                    dropped.taskId = taskId
                    dropped.dropIndex = dropIndex
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
