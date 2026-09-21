package com.todoroo.astrid.service

import com.todoroo.astrid.repeats.RepeatTaskHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.createDueDate
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.time.DateTime

class Upgrade_15_11_Test : DatabaseTest() {
    private val taskDao = db.taskDao()
    private val caldavDao = db.caldavDao()
    private val dirtyDao = db.dirtyDao()
    private val upgrade = Upgrade_15_11(db.upgraderDao(), dirtyDao)

    private suspend fun task(
        recurrence: String,
        due: DateTime,
        repeatFrom: Int = Task.RepeatFrom.DUE_DATE,
        completed: Boolean = false,
        deleted: Boolean = false,
    ): Long {
        val task = Task(
            title = recurrence,
            recurrence = recurrence,
            dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, due.millis),
            repeatFrom = repeatFrom,
            completionDate = if (completed) due.millis else 0,
            deletionDate = if (deleted) due.millis else 0,
        )
        taskDao.createNew(task)
        return task.id
    }

    private suspend fun recurrence(id: Long) = taskDao.fetch(id)!!.recurrence

    private suspend fun modified(id: Long) = taskDao.fetch(id)!!.modificationDate

    @Test
    fun the31stBecomesLastDayOfMonth() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 1, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(id))
    }

    @Test
    fun intervalIsPreserved() = runBlocking {
        val id = task("FREQ=MONTHLY;INTERVAL=6", DateTime(2026, 8, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;INTERVAL=6;BYMONTHDAY=-1", recurrence(id))
    }

    @Test
    fun the30thOfA30DayMonthMigrates() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 4, 30))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(id))
    }

    @Test
    fun februaryMigrates() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 2, 28))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(id))
    }

    @Test
    fun leapDayMigrates() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2028, 2, 29))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(id))
    }

    @Test
    fun midMonthDueDatesAreLeftAlone() = runBlocking {
        val id = task("FREQ=MONTHLY;INTERVAL=6", DateTime(2026, 8, 30))
        val before = modified(id)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;INTERVAL=6", recurrence(id))
        assertEquals(before, modified(id))
    }

    @Test
    fun rulesThatAlreadySayWhatTheyMeanAreLeftAlone() = runBlocking {
        val byMonthDay = task("FREQ=MONTHLY;BYMONTHDAY=-1", DateTime(2026, 1, 31))
        val byDay = task("FREQ=MONTHLY;BYDAY=-1SA", DateTime(2026, 1, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(byMonthDay))
        assertEquals("FREQ=MONTHLY;BYDAY=-1SA", recurrence(byDay))
    }

    @Test
    fun nonMonthlyRulesAreLeftAlone() = runBlocking {
        val id = task("FREQ=YEARLY", DateTime(2026, 1, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=YEARLY", recurrence(id))
    }

    @Test
    fun repeatFromCompletionIsLeftAlone() = runBlocking {
        val id = task(
            "FREQ=MONTHLY",
            DateTime(2026, 8, 31),
            repeatFrom = Task.RepeatFrom.COMPLETION_DATE,
        )

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY", recurrence(id))
    }

    @Test
    fun finishedSeriesIsLeftAlone() = runBlocking {
        val id = task("FREQ=MONTHLY;COUNT=3", DateTime(2026, 1, 31), completed = true)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;COUNT=3", recurrence(id))
    }

    @Test
    fun deletedTaskIsLeftAlone() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 1, 31), deleted = true)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY", recurrence(id))
    }

    @Test
    fun taskWithoutDueDateIsLeftAlone() = runBlocking {
        val task = Task(title = "no due date", recurrence = "FREQ=MONTHLY")
        taskDao.createNew(task)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY", recurrence(task.id))
    }

    @Test
    fun microsoftTasksAreLeftAlone() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 1, 31))
        addToList(id, TYPE_MICROSOFT)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY", recurrence(id))
    }

    @Test
    fun caldavTasksAreMarkedDirty() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 1, 31))
        addToList(id, TYPE_CALDAV)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals(listOf(id), dirtyDao.getDirtyTaskIds().first())
    }

    @Test
    fun malformedRecurrenceDoesNotBlockTheUpgrade() = runBlocking {
        val badInterval = task("FREQ=MONTHLY;INTERVAL=x", DateTime(2026, 1, 31))
        val badMonthDay = task("FREQ=MONTHLY;BYMONTHDAY=32", DateTime(2026, 1, 31))
        val badWkst = task("FREQ=MONTHLY;WKST=XX", DateTime(2026, 1, 31))
        val good = task("FREQ=MONTHLY", DateTime(2026, 1, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        assertEquals("FREQ=MONTHLY;INTERVAL=x", recurrence(badInterval))
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=32", recurrence(badMonthDay))
        assertEquals("FREQ=MONTHLY;WKST=XX", recurrence(badWkst))
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(good))
    }

    @Test
    fun migrationBumpsModified() = runBlocking {
        val id = task("FREQ=MONTHLY", DateTime(2026, 1, 31))
        val before = modified(id)

        upgrade.migrateLastDayOfMonthRecurrence()

        assertTrue(modified(id) >= before)
    }

    @Test
    fun migratedRuleStillRepeatsEverySixMonths() = runBlocking {
        val id = task("FREQ=MONTHLY;INTERVAL=6", DateTime(2026, 8, 31))

        upgrade.migrateLastDayOfMonthRecurrence()

        val migrated = taskDao.fetch(id)!!
        assertEquals(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2027, 2, 28).millis),
            RepeatTaskHelper.computeNextDueDate(migrated, migrated.recurrence!!, false),
        )
    }

    private suspend fun addToList(task: Long, accountType: Int) {
        val account = "account-$accountType"
        val calendar = "calendar-$accountType"
        caldavDao.insert(CaldavAccount(uuid = account, accountType = accountType))
        caldavDao.insert(CaldavCalendar(uuid = calendar, account = account))
        caldavDao.insert(
            CaldavTask(task = task, calendar = calendar, remoteId = "remote-$task")
        )
    }
}
