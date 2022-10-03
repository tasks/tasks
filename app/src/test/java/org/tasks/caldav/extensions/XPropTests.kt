package org.tasks.caldav.extensions

import at.bitfire.ical4android.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.IS_APPLE_SORT_ORDER
import org.tasks.caldav.iCalendar.Companion.order

class XPropTests {
    @Test
    fun setSortOrder() {
        val task = Task()
        task.order = 12345

        assertEquals(12345L, task.order)
    }

    @Test
    fun removeSortOrder() {
        val task = Task()
        task.order = 12345
        task.order = null

        assertNull(task.order)
        assertEquals(0, task.unknownProperties.count(IS_APPLE_SORT_ORDER))
    }

    @Test
    fun overwriteSortOrder() {
        val task = Task()
        task.order = 12345
        task.order = 67890

        assertEquals(67890L, task.order)
        assertEquals(1, task.unknownProperties.count(IS_APPLE_SORT_ORDER))
    }
}