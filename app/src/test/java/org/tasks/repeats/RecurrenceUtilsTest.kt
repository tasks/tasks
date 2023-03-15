package org.tasks.repeats

import junit.framework.TestCase.assertEquals
import org.junit.Test

class RecurrenceUtilsTest {
    @Test
    fun shouldRemoveZeroCount() {
        assertEquals(
            "FREQ=WEEKLY;INTERVAL=1;BYDAY=FR",
            RecurrenceUtils.newRecur("FREQ=WEEKLY;COUNT=0;INTERVAL=1;BYDAY=FR").toString()
        )
    }

    @Test
    fun shouldRemoveNegativeCount() {
        assertEquals(
            "FREQ=WEEKLY;INTERVAL=1;BYDAY=FR",
            RecurrenceUtils.newRecur("FREQ=WEEKLY;COUNT=-1;INTERVAL=1;BYDAY=FR").toString()
        )
    }
}