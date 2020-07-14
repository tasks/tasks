package org.tasks.caldav

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao

@AndroidEntryPoint
class LocalListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override val layout: Int
        get() = R.layout.activity_caldav_calendar_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.menu.findItem(R.id.delete)?.isVisible =
                caldavDao.getCalendarsByAccount(CaldavDao.LOCAL).size > 1
    }

    override fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
            createSuccessful(null)

    override fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            onDeleted(true)
}