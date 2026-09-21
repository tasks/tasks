package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRecordsTest {
    @Test
    fun aRowThisCallCreatedAndCouldNotDeliverComesBackOut() {
        assertEquals(
            listOf(2L),
            undeliveredRows(attempted = listOf(1L, 2L), delivered = listOf(1L), existing = emptySet()),
        )
    }

    @Test
    fun nothingIsRemovedWhenEverythingWentOut() {
        assertEquals(
            emptyList<Long>(),
            undeliveredRows(attempted = listOf(1L, 2L), delivered = listOf(1L, 2L), existing = emptySet()),
        )
    }

    @Test
    fun aRowThatWasAlreadyThereIsNeverRemoved() {
        assertEquals(
            emptyList<Long>(),
            undeliveredRows(attempted = listOf(1L), delivered = emptyList(), existing = setOf(1L)),
        )
    }

    @Test
    fun theTwoRulesApplyIndependentlyWithinOneBatch() {
        val discarded = undeliveredRows(
            attempted = listOf(1L, 2L, 3L, 4L),
            delivered = listOf(1L),

            existing = setOf(2L),
        )

        assertEquals(listOf(3L, 4L), discarded)
    }

    @Test
    fun onlyWhatThisCallAttemptedIsConsidered() {
        assertEquals(
            emptyList<Long>(),
            undeliveredRows(attempted = emptyList(), delivered = listOf(9L), existing = emptySet()),
        )
    }
}
