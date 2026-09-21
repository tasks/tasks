package org.tasks.appfunctions

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.tasks.api.Completion
import org.tasks.api.Creation
import org.tasks.api.ReminderEdit
import org.tasks.api.Revision
import org.tasks.api.ReminderRow
import org.tasks.api.Deletion
import org.tasks.api.PlaceRow
import org.tasks.api.TagChange
import org.tasks.api.TagEdit
import org.tasks.api.TaskRow

class AppFunctionModelsTest {

    @Test
    fun anUnsetDateArrivesNullAndAnyOtherValueIsARealTime() {
        assertNull(taskRow(due = null).toAppTask().dueDateTime)
        assertEquals(local(MILLIS), taskRow(due = MILLIS).toAppTask().dueDateTime)
        assertEquals(local(-1L), taskRow(due = -1L).toAppTask().dueDateTime)
    }

    @Test
    fun aDateSurvivesTheRoundTrip() {
        assertEquals(MILLIS, local(MILLIS)!!.asEpochMillis())
    }

    @Test
    fun aTaskCarriesEveryFieldTheSharedRowHas() {
        val app = taskRow().toAppTask()

        assertEquals(7L, app.id)
        assertEquals("Pack tent", app.title)
        assertEquals(local(MILLIS), app.dueDateTime)
        assertEquals(true, app.dueAllDay)
        assertNull("an open task has no completion time", app.completedAt)
        assertArrayEquals(longArrayOf(3L, 4L), app.tagIds)
        assertEquals(2, app.childCount)
        assertEquals(1, app.uncompletedChildCount)
        assertEquals("FREQ=WEEKLY", app.recurrence)
        assertEquals(true, app.isReadOnly)
    }

    @Test
    fun anUntaggedTaskGetsAnEmptyArrayRatherThanNull() {
        assertArrayEquals(longArrayOf(), taskRow(tagIds = emptyList()).toAppTask().tagIds)
    }

    @Test
    fun aRelativeReminderIsDescribedInWords() {
        val reminder = reminderRow(type = "relative_due", offsetMs = -3_600_000L).toAppReminder()

        assertEquals("1 hour before due", reminder.offsetDescription)
        assertNull("a relative reminder has no trigger time", reminder.triggerAt)
    }

    @Test
    fun aTimeReminderHasATriggerAndNoDescription() {
        val reminder = reminderRow(type = "date_time", triggerAt = MILLIS).toAppReminder()

        assertEquals(local(MILLIS), reminder.triggerAt)
        assertNull(reminder.offsetDescription)
    }

    @Test
    fun aPlaceWithoutANameFallsBackToItsDisplayName() {
        assertEquals("41.82, -88.19", placeRow(name = null).toAppPlace().name)
        assertEquals("Trailhead", placeRow(name = "Trailhead").toAppPlace().name)
    }

    @Test
    fun aNewTaskWritesOnlyTheFieldsItWasGiven() {
        val write = NewTask(title = "Buy firewood", dueDateTime = local(MILLIS)).toTaskWrite()

        assertEquals("Buy firewood", write.title)
        assertEquals(MILLIS, write.due)
        assertNull("an unsent flag must not become false", write.dueAllDay)
        assertNull(write.priority)
        assertNull(write.listId)
    }

    @Test
    fun aPatchLeavesOutWhatItDoesNotMention() {
        val write = TaskPatch(id = 7L, dueDateTime = local(MILLIS)).toTaskWrite()

        assertEquals(MILLIS, write.due)
        assertNull("patching a date must not clear the all-day flag", write.dueAllDay)
        assertNull(write.title)
    }

    @Test
    fun aDateIsClearedByAskingForIt() {
        val write = TaskPatch(id = 7L, clearDueDate = true, clearStartDate = true).toTaskWrite()

        assertEquals("an omitted date leaves it alone, so clearing needs its own flag", 0L, write.due)
        assertEquals(0L, write.start)
    }

    @Test
    fun aDateAndItsClearFlagTogetherAreRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskPatch(id = 7L, dueDateTime = local(MILLIS), clearDueDate = true).toTaskWrite()
        }
    }

    @Test
    fun aNewReminderWritesOnlyTheFieldsItWasGiven() {
        val write = NewReminder(type = "date_time", triggerAt = local(MILLIS)).toReminderWrite()

        assertEquals("date_time", write.type)
        assertEquals(MILLIS, write.triggerAt)
        assertNull(write.offsetMs)
        assertNull(write.placeId)
    }

    @Test
    fun aCreationCarriesTheRowsAndWhichLandedOnAParentsList() {
        val result = Creation(
            ids = listOf(11L, 12L),
            tasks = listOf(taskRow(id = 11L), taskRow(id = 12L)),
            movedToParentListIds = listOf(11L),
        ).toAppTaskCreation()

        assertEquals(listOf(11L, 12L), result.tasks.map { it.id })
        assertArrayEquals(longArrayOf(11L), result.movedToParentListTaskIds)
    }

    @Test
    fun aRevisionCarriesWhatAppliedAndWhatDidNot() {
        val result = Revision(
            rowsChanged = listOf(1, 0, 1),
            tasks = listOf(taskRow(id = 11L), taskRow(id = 13L)),
            unchangedIds = listOf(12L),
            movedToParentListIds = listOf(13L),
            detachedIds = listOf(11L),
        ).toAppTaskUpdate()

        assertEquals(listOf(11L, 13L), result.tasks.map { it.id })
        assertArrayEquals(longArrayOf(12L), result.unchangedTaskIds)
        assertArrayEquals(longArrayOf(13L), result.movedToParentListTaskIds)
        assertArrayEquals(longArrayOf(11L), result.detachedTaskIds)
    }

    @Test
    fun aCompletionCarriesTheTasksAndNamesWhatAdvancedOrWasMissing() {
        val result = Completion(
            taskIds = listOf(11L, 12L, 13L),
            rowsChanged = listOf(1, 1, 0),
            advancedTaskIds = listOf(12L),
            alsoCompletedTaskIds = listOf(21L, 22L),
            reopenedTaskIds = listOf(),
            tasks = listOf(taskRow(id = 11L), taskRow(id = 12L)),
        ).toAppCompletion()

        assertEquals(listOf(11L, 12L), result.tasks.map { it.id })
        assertArrayEquals(longArrayOf(12L), result.advancedTaskIds)
        assertArrayEquals(longArrayOf(13L), result.unchangedTaskIds)
        assertArrayEquals(longArrayOf(21L, 22L), result.alsoCompletedTaskIds)
        assertArrayEquals(longArrayOf(), result.reopenedTaskIds)
    }

    @Test
    fun aTagChangeKeepsWhatEachTaskGainedAndLost() {
        val change = TagChange(
            edits = listOf(
                TagEdit(taskId = 11L, added = 2, removed = 0),
                TagEdit(taskId = 12L, added = 0, removed = 1),
            ),
            tasks = listOf(taskRow(id = 11L), taskRow(id = 12L)),
        ).toAppTagChange()

        assertEquals(2, change.added)
        assertEquals(1, change.removed)
        assertEquals(listOf(11L, 12L), change.edits.map { it.taskId })
        assertEquals(listOf(11L, 12L), change.tasks.map { it.id })
    }

    @Test
    fun aReminderChangeKeepsWhatWasAddedAndHowManyWent() {
        val change = ReminderEdit(
            addedIds = listOf(4L, 5L),
            removed = 1,
            reminders = listOf(reminderRow(type = "date_time", triggerAt = MILLIS)),
            task = taskRow(id = 7L),
        ).toAppReminderChange()

        assertArrayEquals(longArrayOf(4L, 5L), change.addedReminderIds)
        assertEquals(1, change.removed)
        assertEquals(listOf(1L), change.reminders.map { it.id })
        assertEquals("the task comes back, so a location reminder's filing is visible", 7L, change.task!!.id)
    }

    @Test
    fun aDeletionCountsNothingWhenTheRowWasNotThere() {
        assertEquals(AppDeletion(true, 3), Deletion(rowsDeleted = 1, alsoAffected = 3).toAppDeletion())
        assertEquals(AppDeletion(false, 0), Deletion(rowsDeleted = 0, alsoAffected = 0).toAppDeletion())
    }

    private fun local(millis: Long): LocalDateTime? =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun taskRow(
        tagIds: List<Long> = listOf(3L, 4L),
        due: Long? = MILLIS,
        id: Long = 7L,
        listId: Long? = 2L,
    ) = TaskRow(
        id = id,
        title = "Pack tent",
        notes = null,
        priority = "high",
        due = due,
        dueAllDay = true,
        start = null,
        startAllDay = false,
        completed = null,
        created = MILLIS,
        modified = MILLIS,
        recurrence = "FREQ=WEEKLY",
        repeatFrom = "completion_date",
        parentId = null,
        listId = listId,
        tagIds = tagIds,
        placeId = null,
        childCount = 2,
        uncompletedChildCount = 1,
        isReadOnly = true,
    )

    private fun reminderRow(
        type: String = "date_time",
        triggerAt: Long? = null,
        offsetMs: Long? = null,
    ) = ReminderRow(
        id = 1L,
        taskId = 7L,
        type = type,
        placeId = null,
        triggerAt = triggerAt,
        offsetMs = offsetMs,
        repeatCount = null,
        intervalMs = null,
    )

    private fun placeRow(name: String?) = PlaceRow(
        id = 1L,
        name = name,
        displayName = "41.82, -88.19",
        address = null,
        phone = null,
        url = null,
        latitude = 41.82,
        longitude = -88.19,
        radius = 120,
        color = null,
        icon = null,
    )

    private companion object {
        const val MILLIS = 1_757_000_000_000L
    }
}
