package org.tasks.db

import org.tasks.data.sql.Functions
import org.tasks.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryUtilsTest {
    @Test
    fun replaceHiddenLT() {
        assertEquals("(1)", QueryUtils.showHidden(Task.HIDE_UNTIL.lt(Functions.now()).toString()))
    }

    @Test
    fun replaceHiddenLTE() {
        assertEquals("(1)", QueryUtils.showHidden(Task.HIDE_UNTIL.lte(Functions.now()).toString()))
    }

    @Test
    fun replaceUncompletedEQ() {
        assertEquals("(1)", QueryUtils.showCompleted(Task.COMPLETION_DATE.eq(0).toString()))
    }

    @Test
    fun replaceUncompletedLTE() {
        assertEquals("(1)", QueryUtils.showCompleted(Task.COMPLETION_DATE.lte(0).toString()))
    }
}