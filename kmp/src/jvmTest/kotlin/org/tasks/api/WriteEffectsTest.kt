package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Reminders
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDay
import java.time.LocalDateTime
import java.time.ZoneId

class WriteEffectsTest : ApiEngineTestCase() {

    @Test
    fun deletingATaskCountsEveryDescendant() = runBlocking {
        val root = createTask("Trip")
        val child = engine.createTasks(writer, listOf(TaskWrite(title = "Pack", parentId = root))).ids.single()
        engine.createTasks(writer, listOf(TaskWrite(title = "Tent", parentId = child), TaskWrite(title = "Poles", parentId = child)))

        val deletion = engine.deleteTask(writer, root)

        assertEquals(1, deletion.rowsDeleted)
        assertEquals(3, deletion.alsoAffected)
        assertEquals(0, engine.countTasks(TaskQuery(status = "any")))
    }

    @Test
    fun completionReportsWhatElseChanged() = runBlocking {
        val root = createTask("Trip")
        val child = engine.createTasks(writer, listOf(TaskWrite(title = "Pack", parentId = root))).ids.single()
        val grandchild = engine.createTasks(writer, listOf(TaskWrite(title = "Tent", parentId = child))).ids.single()

        val completed = engine.completeTasks(writer, listOf(root), completed = true)
        val reopened = engine.completeTasks(writer, listOf(grandchild), completed = false)
        engine.completeTasks(writer, listOf(root), completed = true)
        val reopenedDown = engine.completeTasks(writer, listOf(root), completed = false)

        assertEquals(listOf(child, grandchild).sorted(), completed.alsoCompletedTaskIds.sorted())
        assertTrue(completed.reopenedTaskIds.isEmpty())
        assertEquals(listOf(root, child).sorted(), reopened.reopenedTaskIds.sorted())
        assertTrue(reopened.alsoCompletedTaskIds.isEmpty())
        assertEquals(listOf(child, grandchild).sorted(), reopenedDown.reopenedTaskIds.sorted())
    }

    @Test
    fun aRelativeReminderNeedsItsDate() = runBlocking {
        val dateless = createTask("Someday")
        val dated = createTask("Dentist", dueDate = DUE)

        val noDue = runCatching { engine.setTaskReminders(writer, dateless, listOf(ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = -60_000)), emptyList()) }.exceptionOrNull()
        val noStart = runCatching { engine.setTaskReminders(writer, dated, listOf(ReminderWrite(Reminders.TYPE_RELATIVE_START, offsetMs = 0)), emptyList()) }.exceptionOrNull()
        engine.setTaskReminders(writer, dated, listOf(ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = 0)), emptyList())
        engine.setTaskReminders(writer, dated, listOf(ReminderWrite(Reminders.TYPE_SNOOZE, triggerAt = DUE)), emptyList())

        assertTrue(noDue?.message ?: "no error", "no due date" in noDue?.message.orEmpty())
        assertTrue(noStart?.message ?: "no error", "no start date" in noStart?.message.orEmpty())
        val rows = engine.findReminders(ReminderQuery(taskIds = listOf(dated))).rows
        assertEquals(listOf(Reminders.TYPE_RELATIVE_DUE, Reminders.TYPE_SNOOZE), rows.map { it.type }.sorted())
        val row = rows.single { it.type == Reminders.TYPE_RELATIVE_DUE }
        assertEquals(0L, row.offsetMs)
        assertEquals("at due time", describeOffset(row.offsetMs!!, row.type))
        assertEquals("randomly, about every 1 day", describeOffset(86_400_000L, Reminders.TYPE_RANDOM))
    }

    @Test
    fun tagsRemindersListsAndParentsAllCountAsChanges() = runBlocking {
        val id = createTask("Dentist", dueDate = DUE)
        val parent = createTask("Health")
        val elsewhere = engine.createList(writer, ListWrite(title = "Elsewhere", accountId = accountId())).id
        val tag = engine.createTag(writer, TagWrite(name = "urgent")).row.id
        var last = engine.taskRow(id)!!.modified!!
        suspend fun advanced(what: String) {
            val now = engine.taskRow(id)!!.modified!!
            assertTrue("$what did not bump modified", now > last)
            last = now
        }

        Thread.sleep(5); engine.setTaskTags(writer, listOf(id), add = listOf(tag), remove = emptyList()); advanced("tagging")
        Thread.sleep(5); engine.setTaskReminders(writer, id, listOf(ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = -60_000)), emptyList()); advanced("a reminder")
        Thread.sleep(5); engine.updateTasks(writer, listOf(TaskUpdate(id, TaskWrite(listId = elsewhere)))); advanced("a list move")
        Thread.sleep(5); engine.updateTasks(writer, listOf(TaskUpdate(parent, TaskWrite(listId = elsewhere)), TaskUpdate(id, TaskWrite(parentId = parent)))); advanced("re-parenting")
    }

    @Test
    fun idempotentCreatesSayWhetherTheyCreated() = runBlocking {
        val tag = engine.createTag(writer, TagWrite(name = "urgent"))
        val again = engine.createTag(writer, TagWrite(name = "Urgent"))
        val place = engine.createPlace(writer, PlaceWrite(name = "Home", latitude = 1.0, longitude = 2.0))
        val samePlace = engine.createPlace(writer, PlaceWrite(name = "Home again", latitude = 1.0, longitude = 2.0))

        assertTrue("tag", tag.created)
        assertFalse("tag again", again.created)
        assertEquals(tag.row.id, again.row.id)
        assertTrue("place", place.created)
        assertFalse("place again", samePlace.created)
        assertEquals("Home", samePlace.row.name)
    }

    @Test
    fun aRefusedBatchNamesTheEntry() = runBlocking {
        val error = runCatching {
            engine.createTasks(writer, listOf(TaskWrite(title = "Fine"), TaskWrite(title = "Orphan", parentId = 999L)))
        }.exceptionOrNull()

        assertTrue(error?.message ?: "no error", error!!.message!!.startsWith("Entry 2 of 2 (Orphan): "))
        assertEquals(0, engine.countTasks(TaskQuery()))
    }

    @Test
    fun overdueIsWhatTheAppCallsOverdue() = runBlocking {
        val now = System.currentTimeMillis()
        createTask("Timed, passed", dueDate = now - 60_000)
        createTask("Timed, ahead", dueDate = now + 60_000)
        createTask("All day, yesterday", dueDate = now.startOfDay() - ONE_DAY, dueAllDay = true)
        createTask("All day, today", dueDate = now, dueAllDay = true)

        val titles = engine.findTasks(TaskQuery(due = "overdue")).rows.map { it.title }.sorted()

        assertEquals(listOf("All day, yesterday", "Timed, passed"), titles)
        assertTrue(runCatching { TaskQuery(due = "today") }.isFailure)
    }

    @Test
    fun everyTimestampTakesALocalDate() = runBlocking {
        val before = createTask("Before", dueDate = local("2026-09-11T09:00:00"))
        createTask("Sep 12", dueDate = local("2026-09-12T09:00:00"))
        createTask("After", dueDate = local("2026-09-13T09:00:00"))

        val sep12 = engine.findTasks(TaskQuery(dueAfter = "2026-09-12", dueBefore = "2026-09-13")).rows.map { it.title }
        val morning = engine.findTasks(TaskQuery(dueBefore = "2026-09-12T08:00:00")).rows.map { it.title }
        engine.completeTasks(writer, listOf(before), completed = true, completedAt = "2026-09-11T10:00:00")
        engine.setTaskReminders(writer, before, listOf(ReminderWrite(Reminders.TYPE_SNOOZE, triggerAt = "2026-09-11T11:00:00")), emptyList())
        val error = runCatching { engine.findTasks(TaskQuery(dueBefore = "next week")) }.exceptionOrNull()

        assertEquals(listOf("Sep 12"), sep12)
        assertEquals(listOf("Before"), morning)
        assertEquals(local("2026-09-11T10:00:00"), engine.taskRow(before)!!.completed)
        assertEquals(local("2026-09-11T11:00:00"), engine.findReminders(ReminderQuery(taskIds = listOf(before))).rows.single().triggerAt)
        assertTrue(error?.message ?: "no error", "due_before must be" in error?.message.orEmpty())
    }

    @Test
    fun aReminderThatCouldNeverFireIsRefused() = runBlocking {
        val id = createTask("Dentist", dueDate = DUE)
        val dead = listOf(
            ReminderWrite(Reminders.TYPE_DATE_TIME),
            ReminderWrite(Reminders.TYPE_SNOOZE, triggerAt = 0),
            ReminderWrite(Reminders.TYPE_RANDOM, offsetMs = -3_600_000),
            ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = 0, repeatCount = 3),
            ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = 0, repeatCount = -1, intervalMs = 60_000),
            ReminderWrite(Reminders.TYPE_RELATIVE_DUE, offsetMs = 0, repeatCount = 3, intervalMs = 15),
            ReminderWrite(Reminders.TYPE_RANDOM, offsetMs = 15),
        )

        val errors = dead.map { runCatching { engine.setTaskReminders(writer, id, listOf(it), emptyList()) }.exceptionOrNull() }

        errors.forEachIndexed { index, e -> assertTrue("reminder $index: ${e?.message}", e is IllegalArgumentException) }
        assertEquals(0, engine.findReminders(ReminderQuery(taskIds = listOf(id))).total)
    }

    @Test
    fun aReminderIsOnlyRemovedThroughItsOwnTask() = runBlocking {
        val mine = createTask("Dentist", dueDate = DUE)
        val other = createTask("Taxes")
        val reminder = engine.setTaskReminders(writer, mine, listOf(ReminderWrite(Reminders.TYPE_DATE_TIME, triggerAt = DUE)), emptyList()).addedIds.single()

        val error = runCatching { engine.setTaskReminders(writer, other, emptyList(), listOf(reminder)) }.exceptionOrNull()

        assertTrue(error?.message ?: "no error", "belongs to task $mine" in error?.message.orEmpty())
        assertEquals(1, engine.findReminders(ReminderQuery(taskIds = listOf(mine))).total)
    }

    @Test
    fun aContradictoryOrUnknownTagEditIsRefused() = runBlocking {
        val id = createTask("Dentist")
        val tag = engine.createTag(writer, TagWrite(name = "urgent")).row.id

        val both = runCatching { engine.setTaskTags(writer, listOf(id), add = listOf(tag), remove = listOf(tag)) }.exceptionOrNull()
        val unknown = runCatching { engine.setTaskTags(writer, listOf(id), add = emptyList(), remove = listOf(999L)) }.exceptionOrNull()

        assertTrue(both?.message ?: "no error", "both" in both?.message.orEmpty())
        assertTrue(unknown?.message ?: "no error", "No tag with id 999" in unknown?.message.orEmpty())
    }

    @Test
    fun aPlaceIsOnTheMapWithARealRadius() = runBlocking {
        val offMap = runCatching { engine.createPlace(writer, PlaceWrite(latitude = 91.0, longitude = 0.0)) }.exceptionOrNull()
        val flat = runCatching { engine.createPlace(writer, PlaceWrite(latitude = 1.0, longitude = 2.0, radius = 0)) }.exceptionOrNull()
        val place = engine.createPlace(writer, PlaceWrite(name = "   ", latitude = 1.0, longitude = 2.0, address = "1 Main St")).row
        val shrunk = runCatching { engine.changePlace(writer, place.id, PlaceWrite(radius = -10)) }.exceptionOrNull()

        assertTrue(offMap?.message ?: "no error", "not on the map" in offMap?.message.orEmpty())
        assertTrue(flat?.message ?: "no error", "radius" in flat?.message.orEmpty())
        assertTrue(shrunk?.message ?: "no error", "radius" in shrunk?.message.orEmpty())
        assertEquals(null, place.name)
        assertEquals("1 Main St", place.displayName)
        assertEquals(0, engine.findPlaces(PlaceQuery()).rows.count { it.latitude == 91.0 })
    }

    @Test
    fun aZeroCompletionStampMeansNow() = runBlocking {
        val id = createTask("Dentist")

        val done = engine.completeTasks(writer, listOf(id), completed = true, completedAt = 0).tasks.single().completed

        assertTrue("$done", done != null && done > System.currentTimeMillis() - 60_000)
    }

    @Test
    fun aRunawayPatternIsCutOffRatherThanPeggingACore() = runBlocking {
        createTask("Notes", notes = "n".repeat(200_000))

        val started = System.nanoTime()
        val error = runCatching { engine.findTasks(TaskQuery(matches = "(n+)+x", matchFields = listOf("notes"))) }.exceptionOrNull()
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0

        assertTrue(error?.message ?: "no error", "simplify the pattern" in error?.message.orEmpty())
        assertTrue("took ${seconds}s", seconds < 10)
        assertEquals(1, engine.findTasks(TaskQuery(matches = "^n+$", matchFields = listOf("notes"))).total)
    }

    @Test
    fun negativePagingIsRefusedRatherThanReadAsACount() {
        assertTrue(runCatching { TaskQuery(limit = -1) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { TaskQuery(offset = -1) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun movingASubtaskAloneDetachesItAndSaysSo() = runBlocking {
        val parent = createTask("Trip")
        val child = engine.createTasks(writer, listOf(TaskWrite(title = "Pack", parentId = parent))).ids.single()
        val elsewhere = engine.createList(writer, ListWrite(title = "Elsewhere", accountId = accountId())).id

        val revision = engine.updateTasks(writer, listOf(TaskUpdate(child, TaskWrite(listId = elsewhere))))

        assertEquals(listOf(child), revision.detachedIds)
        assertEquals(null, engine.taskRow(child)!!.parentId)
        assertEquals(elsewhere, engine.taskRow(child)!!.listId)
    }

    private fun local(text: String): Long =
        LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        private const val DUE = 1_800_000_000_000L
    }
}
