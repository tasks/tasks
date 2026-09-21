package org.tasks.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter

class AllowsNestingTest {
    private fun list(type: Int) = CaldavFilter(
        calendar = CaldavCalendar(uuid = "cal-$type", account = "acct-$type"),
        account = CaldavAccount(uuid = "acct-$type", accountType = type),
    )

    private val caldav = list(TYPE_CALDAV)
    private val google = list(TYPE_GOOGLE_TASKS)

    private fun allows(from: CaldavFilter?, to: CaldavFilter?) =
        TaskEditViewModel.State(isLoading = false, originalList = from, list = to).allowsNesting

    @Test
    fun aListThatNestsAllowsIt() {
        assertTrue(allows(from = caldav, to = caldav))
    }

    @Test
    fun eitherEndBeingAbleToHoldItIsEnough() {
        assertTrue(allows(from = caldav, to = google))
        assertTrue(allows(from = google, to = caldav))
    }

    @Test
    fun aTaskStayingOnASingleLevelListCannotNest() {
        assertFalse(allows(from = google, to = google))
    }

    @Test
    fun aSubtaskWithNoListOfItsOwnCannotNestOntoASingleLevelList() {
        assertFalse(allows(from = null, to = google))
    }

    @Test
    fun aSubtaskWithNoListOfItsOwnCanNestOntoAListThatNests() {
        assertTrue(allows(from = null, to = caldav))
    }

    @Test
    fun nothingPickedYetRestrictsNothing() {
        assertTrue(allows(from = null, to = null))
    }
}
