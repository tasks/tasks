package org.tasks.caldav.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.icalendar.VTodo

class XPropTests {
    @Test
    fun setSortOrder() {
        val task = VTodo()
        task.order = 12345

        assertEquals(12345L, task.order)
    }

    @Test
    fun removeSortOrder() {
        val task = VTodo()
        task.order = 12345
        task.order = null

        assertNull(task.order)
        assertEquals(0, task.unknownProperties.count { it.name == "X-APPLE-SORT-ORDER" })
    }

    @Test
    fun overwriteSortOrder() {
        val task = VTodo()
        task.order = 12345
        task.order = 67890

        assertEquals(67890L, task.order)
        assertEquals(1, task.unknownProperties.count { it.name == "X-APPLE-SORT-ORDER" })
    }
}