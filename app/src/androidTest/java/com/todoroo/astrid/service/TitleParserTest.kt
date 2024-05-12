/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service

import org.tasks.data.entity.Task
import com.todoroo.astrid.utility.TitleParser
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.Recur.Frequency.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.tasks.R
import org.tasks.data.dao.TagDataDao
import org.tasks.date.DateTimeUtils
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.preferences.Preferences
import org.tasks.repeats.RecurrenceUtils.newRecur
import java.util.*
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TitleParserTest : InjectingTestCase() {
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCreator: TaskCreator

    @Before
    override fun setUp() {
        super.setUp()
        preferences.setStringFromInteger(R.string.p_default_urgency_key, 0)
    }

    /**
     * test that completing a task w/ no regular expressions creates a simple task with no date, no
     * repeat, no lists
     */
    @Test
    fun testNoRegexes() = runBlocking {
        val task = taskCreator.basicQuickAddTask("Jog")
        val nothing = Task()
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        assertEquals(task.recurrence, nothing.recurrence)
    }

    /** Tests correct date is parsed  */
    @Test
    fun testMonthDate() {
        val titleMonthStrings = arrayOf(
                "Jan.", "January",
                "Feb.", "February",
                "Mar.", "March",
                "Apr.", "April",
                "May", "May",
                "Jun.", "June",
                "Jul.", "July",
                "Aug.", "August",
                "Sep.", "September",
                "Oct.", "October",
                "Nov.", "November",
                "Dec.", "December"
        )
        for (i in 0..22) {
            val testTitle = "Jog on " + titleMonthStrings[i] + " 12."
            val task = insertTitleAddTask(testTitle)
            val date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.monthOfYear, i / 2 + 1)
            assertEquals(date.dayOfMonth, 12)
        }
    }

    @Test
    fun testMonthSlashDay() {
        for (i in 1..12) {
            val testTitle = "Jog on $i/12/13"
            val task = insertTitleAddTask(testTitle)
            val date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.monthOfYear, i)
            assertEquals(date.dayOfMonth, 12)
            assertEquals(date.year, 2013)
        }
    }

    @Test
    fun testArmyTime() {
        val testTitle = "Jog on 23:21."
        val task = insertTitleAddTask(testTitle)
        val date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.hourOfDay, 23)
        assertEquals(date.minuteOfHour, 21)
    }

    @Test
    fun test_AM_PM() {
        val testTitle = "Jog at 8:33 PM."
        val task = insertTitleAddTask(testTitle)
        val date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.hourOfDay, 20)
        assertEquals(date.minuteOfHour, 33)
    }

    @Test
    fun test_at_hour() {
        val testTitle = "Jog at 8 PM."
        val task = insertTitleAddTask(testTitle)
        val date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.hourOfDay, 20)
        assertEquals(date.minuteOfHour, 0)
    }

    @Test
    fun test_oclock_AM() {
        val testTitle = "Jog at 8 o'clock AM."
        val task = insertTitleAddTask(testTitle)
        val date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.hourOfDay, 8)
        assertEquals(date.minuteOfHour, 0)
    }

    @Test
    fun test_several_forms_of_eight() {
        val testTitles = arrayOf("Jog 8 AM", "Jog 8 o'clock AM", "at 8:00 AM")
        for (testTitle in testTitles) {
            val task = insertTitleAddTask(testTitle)
            val date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.hourOfDay, 8)
            assertEquals(date.minuteOfHour, 0)
        }
    }

    @Test
    fun test_several_forms_of_1230PM() {
        val testTitles = arrayOf(
                "Jog 12:30 PM", "at 12:30 PM", "Do something on 12:30 PM", "Jog at 12:30 PM Friday"
        )
        for (testTitle in testTitles) {
            val task = insertTitleAddTask(testTitle)
            val date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.hourOfDay, 12)
            assertEquals(date.minuteOfHour, 30)
        }
    }

    private fun insertTitleAddTask(title: String): Task = runBlocking {
        taskCreator.createWithValues(title)
    }

    // ----------------Days begin----------------//
    @Test
    @Ignore("Flaky test")
    fun testDays() = runBlocking {
        val today = Calendar.getInstance()
        var title = "Jog today"
        var task = taskCreator.createWithValues(title)
        var date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.dayOfWeek, today[Calendar.DAY_OF_WEEK])
        // Calendar starts 1-6, date.getDay() starts at 0
        title = "Jog tomorrow"
        task = taskCreator.createWithValues(title)
        date = DateTimeUtils.newDateTime(task.dueDate)
        assertEquals(date.dayOfWeek % 7, (today[Calendar.DAY_OF_WEEK] + 1) % 7)
        val days = arrayOf(
                "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
        val abrevDays = arrayOf("sun.", "mon.", "tue.", "wed.", "thu.", "fri.", "sat.")
        for (i in 1..6) {
            title = "Jog " + days[i]
            task = taskCreator.createWithValues(title)
            date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.dayOfWeek, i + 1)
            title = "Jog " + abrevDays[i]
            task = taskCreator.createWithValues(title)
            date = DateTimeUtils.newDateTime(task.dueDate)
            assertEquals(date.dayOfWeek, i + 1)
        }
    }
    // ----------------Days end----------------//
    // ----------------Priority begin----------------//
    /** tests all words using priority 0  */
    @Test
    fun testPriority0() = runBlocking {
        val acceptedStrings = arrayOf("priority 0", "least priority", "lowest priority", "bang 0")
        for (acceptedString in acceptedStrings) {
            val title = "Jog $acceptedString"
            val task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.NONE)
        }
        for (acceptedString in acceptedStrings) {
            val title = "$acceptedString jog"
            val task = taskCreator.createWithValues(title)
            assertNotSame(task.priority, Task.Priority.NONE)
        }
    }

    @Test
    fun testPriority1() = runBlocking {
        val acceptedStringsAtEnd = arrayOf("priority 1", "low priority", "bang", "bang 1")
        val acceptedStringsAnywhere = arrayOf("!1", "!")
        var task: Task
        for (acceptedStringAtEnd in acceptedStringsAtEnd) {
            task = taskCreator.basicQuickAddTask(
                    "Jog $acceptedStringAtEnd") // test at end of task. should set importance.
            assertEquals(task.priority, Task.Priority.LOW)
        }
        for (acceptedStringAtEnd in acceptedStringsAtEnd) {
            task = taskCreator.basicQuickAddTask(acceptedStringAtEnd
                    + " jog") // test at beginning of task. should not set importance.
            assertEquals(task.priority, Task.Priority.LOW)
        }
        for (acceptedStringAnywhere in acceptedStringsAnywhere) {
            task = taskCreator.basicQuickAddTask(
                    "Jog $acceptedStringAnywhere") // test at end of task. should set importance.
            assertEquals(task.priority, Task.Priority.LOW)
            task = taskCreator.basicQuickAddTask(
                    "$acceptedStringAnywhere jog") // test at beginning of task. should set importance.
            assertEquals(task.priority, Task.Priority.LOW)
        }
    }

    @Test
    fun testPriority2() = runBlocking {
        val acceptedStringsAtEnd = arrayOf("priority 2", "high priority", "bang bang", "bang 2")
        val acceptedStringsAnywhere = arrayOf("!2", "!!")
        for (acceptedStringAtEnd in acceptedStringsAtEnd) {
            var title = "Jog $acceptedStringAtEnd"
            var task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.MEDIUM)
            title = "$acceptedStringAtEnd jog"
            task = taskCreator.createWithValues(title)
            assertNotSame(task.priority, Task.Priority.MEDIUM)
        }
        for (acceptedStringAnywhere in acceptedStringsAnywhere) {
            var title = "Jog $acceptedStringAnywhere"
            var task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.MEDIUM)
            title = "$acceptedStringAnywhere jog"
            task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.MEDIUM)
        }
    }

    @Test
    fun testPriority3() = runBlocking {
        val acceptedStringsAtEnd = arrayOf(
                "priority 3",
                "highest priority",
                "bang bang bang",
                "bang 3",
                "bang bang bang bang bang bang bang"
        )
        val acceptedStringsAnywhere = arrayOf("!3", "!!!", "!6", "!!!!!!!!!!!!!")
        for (acceptedStringAtEnd in acceptedStringsAtEnd) {
            var title = "Jog $acceptedStringAtEnd"
            var task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.HIGH)
            title = "$acceptedStringAtEnd jog"
            task = taskCreator.createWithValues(title)
            assertNotSame(task.priority, Task.Priority.HIGH)
        }
        for (acceptedStringAnywhere in acceptedStringsAnywhere) {
            var title = "Jog $acceptedStringAnywhere"
            var task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.HIGH)
            title = "$acceptedStringAnywhere jog"
            task = taskCreator.createWithValues(title)
            assertEquals(task.priority, Task.Priority.HIGH)
        }
    }
    // ----------------Priority end----------------//
    // ----------------Repeats begin----------------//
    /** test daily repeat from due date, but with no due date set  */
    @Test
    fun testDailyWithNoDueDate() = runBlocking {
        var title = "Jog daily"
        var task = taskCreator.createWithValues(title)
        val recur = newRecur()
        recur.setFrequency(DAILY.name)
        recur.interval = 1
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        title = "Jog every day"
        task = taskCreator.createWithValues(title)
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        for (i in 1..12) {
            title = "Jog every $i days."
            recur.interval = i
            task = taskCreator.createWithValues(title)
            assertEquals(task.recurrence, recur.toString())
            assertFalse(task.hasDueTime())
            assertFalse(task.hasDueDate())
        }
    }

    /** test weekly repeat from due date, with no due date & time set  */
    @Test
    fun testWeeklyWithNoDueDate() = runBlocking {
        var title = "Jog weekly"
        var task = taskCreator.createWithValues(title)
        val recur = newRecur()
        recur.setFrequency(WEEKLY.name)
        recur.interval = 1
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        title = "Jog every week"
        task = taskCreator.createWithValues(title)
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        for (i in 1..12) {
            title = "Jog every $i weeks"
            recur.interval = i
            task = taskCreator.createWithValues(title)
            assertEquals(task.recurrence, recur.toString())
            assertFalse(task.hasDueTime())
            assertFalse(task.hasDueDate())
        }
    }

    /** test hourly repeat from due date, with no due date but no time  */
    @Test
    fun testMonthlyFromNoDueDate() = runBlocking {
        var title = "Jog monthly"
        var task = taskCreator.createWithValues(title)
        val recur = newRecur()
        recur.setFrequency(MONTHLY.name)
        recur.interval = 1
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        title = "Jog every month"
        task = taskCreator.createWithValues(title)
        assertEquals(task.recurrence, recur.toString())
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        for (i in 1..12) {
            title = "Jog every $i months"
            recur.interval = i
            task = taskCreator.createWithValues(title)
            assertEquals(task.recurrence, recur.toString())
            assertFalse(task.hasDueTime())
            assertFalse(task.hasDueDate())
        }
    }

    @Test
    fun testDailyFromDueDate() = runBlocking {
        var title = "Jog daily starting from today"
        var task = taskCreator.createWithValues(title)
        val recur = newRecur()
        recur.setFrequency(DAILY.name)
        recur.interval = 1
        assertEquals(task.recurrence, recur.toString())
        assertTrue(task.hasDueDate())
        title = "Jog every day starting from today"
        task = taskCreator.createWithValues(title)
        assertEquals(task.recurrence, recur.toString())
        assertTrue(task.hasDueDate())
        for (i in 1..12) {
            title = "Jog every $i days starting from today"
            recur.interval = i
            task = taskCreator.createWithValues(title)
            assertEquals(task.recurrence, recur.toString())
            assertTrue(task.hasDueDate())
        }
    }

    @Test
    fun testWeeklyFromDueDate() = runBlocking {
        var title = "Jog weekly starting from today"
        var task = taskCreator.createWithValues(title)
        val recur = newRecur()
        recur.setFrequency(WEEKLY.name)
        recur.interval = 1
        assertEquals(task.recurrence, recur.toString())
        assertTrue(task.hasDueDate())
        title = "Jog every week starting from today"
        task = taskCreator.createWithValues(title)
        assertEquals(task.recurrence, recur.toString())
        assertTrue(task.hasDueDate())
        for (i in 1..12) {
            title = "Jog every $i weeks starting from today"
            recur.interval = i
            task = taskCreator.createWithValues(title)
            assertEquals(task.recurrence, recur.toString())
            assertTrue(task.hasDueDate())
        }
    }
    // ----------------Repeats end----------------//
    // ----------------Tags begin----------------//
    /** tests all words using priority 0  */
    @Test
    fun testTagsPound() = runBlocking {
        val acceptedStrings = arrayOf("#tag", "#a", "#(a cool tag)", "#(cool)")
        var task: Task
        for (acceptedString in acceptedStrings) {
            task = Task()
            task.title = "Jog $acceptedString" // test at end of task. should set importance.
            val tags = ArrayList<String>()
            TitleParser.listHelper(tagDataDao, task, tags)
            val tag = TitleParser.trimParenthesis(acceptedString)
            assertTrue(
                    "test pound at failed for string: $acceptedString for tags: $tags",
                    tags.contains(tag))
        }
    }

    /** tests all words using priority 0  */
    @Test
    fun testTagsAt() = runBlocking {
        val acceptedStrings = arrayOf("@tag", "@a", "@(a cool tag)", "@(cool)")
        var task: Task
        for (acceptedString in acceptedStrings) {
            task = Task()
            task.title = "Jog $acceptedString" // test at end of task. should set importance.
            val tags = ArrayList<String>()
            TitleParser.listHelper(tagDataDao, task, tags)
            val tag = TitleParser.trimParenthesis(acceptedString)
            assertTrue(
                    "testTagsAt failed for string: $acceptedString for tags: $tags",
                    tags.contains(tag))
        }
    }
}