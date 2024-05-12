package org.tasks.opentasks

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.menu.findItem(R.id.delete).isVisible = false
        nameLayout.visibility = View.GONE
        colorRow.visibility = View.GONE
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {}

    override suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) {}
}