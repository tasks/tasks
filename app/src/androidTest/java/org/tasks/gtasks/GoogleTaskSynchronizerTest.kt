package org.tasks.gtasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime

@RunWith(AndroidJUnit4::class)
class GoogleTaskSynchronizerTest {
    @Test
    fun testMergeDate() {
        val local = newTask(with(DUE_DATE, DateTime(2016, 3, 12)))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 11))).dueDate, local)
        assertEquals(DateTime(2016, 3, 11, 12, 0).millis, local.dueDate)
    }

    @Test
    fun testMergeTime() {
        val local = newTask(with(DUE_TIME, DateTime(2016, 3, 11, 13, 30)))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 11))).dueDate, local)
        assertEquals(DateTime(2016, 3, 11, 13, 30, 1).millis, local.dueDate)
    }

    @Test
    fun testDueDateAdjustHideBackwards() {
        val local = newTask(with(DUE_DATE, DateTime(2016, 3, 12)), with(HIDE_TYPE, Task.HIDE_UNTIL_DUE))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 11))).dueDate, local)
        assertEquals(DateTime(2016, 3, 11).millis, local.hideUntil)
    }

    @Test
    fun testDueDateAdjustHideForwards() {
        val local = newTask(with(DUE_DATE, DateTime(2016, 3, 12)), with(HIDE_TYPE, Task.HIDE_UNTIL_DUE))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 14))).dueDate, local)
        assertEquals(DateTime(2016, 3, 14).millis, local.hideUntil)
    }

    @Test
    fun testDueTimeAdjustHideBackwards() {
        val local = newTask(
                with(DUE_TIME, DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 11))).dueDate, local)
        assertEquals(
                DateTime(2016, 3, 11, 13, 30, 1).millis, local.hideUntil)
    }

    @Test
    fun testDueTimeAdjustTimeForwards() {
        val local = newTask(
                with(DUE_TIME, DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME))
        GoogleTaskSynchronizer.mergeDates(newTask(with(DUE_DATE, DateTime(2016, 3, 14))).dueDate, local)
        assertEquals(
                DateTime(2016, 3, 14, 13, 30, 1).millis, local.hideUntil)
    }

    @Test
    fun testDueDateClearHide() {
        val local = newTask(with(DUE_DATE, DateTime(2016, 3, 12)), with(HIDE_TYPE, Task.HIDE_UNTIL_DUE))
        GoogleTaskSynchronizer.mergeDates(newTask().dueDate, local)
        assertEquals(0L, local.hideUntil)
    }

    @Test
    fun testDueTimeClearHide() {
        val local = newTask(
                with(DUE_TIME, DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME))
        GoogleTaskSynchronizer.mergeDates(newTask().dueDate, local)
        assertEquals(0L, local.hideUntil)
    }

    @Test
    fun truncateValue() {
        assertEquals("1234567", GoogleTaskSynchronizer.truncate("12345678", 7))
    }

    @Test
    fun dontTruncateMax() {
        assertEquals("1234567", GoogleTaskSynchronizer.truncate("1234567", 7))
    }

    @Test
    fun dontTruncateShortValue() {
        assertEquals("12345", GoogleTaskSynchronizer.truncate("12345", 7))
    }

    @Test
    fun dontTruncateNull() {
        assertNull(GoogleTaskSynchronizer.truncate(null, 7))
    }

    @Test
    fun dontOverwriteTruncatedValue() {
        assertEquals("123456789", GoogleTaskSynchronizer.getTruncatedValue("123456789", "1234567", 7))
    }

    @Test
    fun overwriteTruncatedValueWithShortenedValue() {
        assertEquals("12345", GoogleTaskSynchronizer.getTruncatedValue("123456789", "12345", 7))
    }

    @Test
    fun overwriteTruncatedValueWithNullValue() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue("123456789", null, 7))
    }

    @Test
    fun overwriteNullValueWithTruncatedValue() {
        assertEquals("1234567", GoogleTaskSynchronizer.getTruncatedValue(null, "1234567", 7))
    }
}