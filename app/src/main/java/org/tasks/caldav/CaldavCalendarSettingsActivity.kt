package org.tasks.caldav

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar

@AndroidEntryPoint
class CaldavCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {
    private val createCalendarViewModel: CreateCalendarViewModel by viewModels()
    private val deleteCalendarViewModel: DeleteCalendarViewModel by viewModels()
    private val updateCalendarViewModel: UpdateCalendarViewModel by viewModels()

    override val layout: Int
        get() = R.layout.activity_caldav_calendar_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed)
        deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed)
        updateCalendarViewModel.observe(
                this,
                { updateCalendar() },
                this::requestFailed)
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
            createCalendarViewModel.createCalendar(caldavAccount, name, color)

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendarViewModel.updateCalendar(account, calendar, name, color)

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            deleteCalendarViewModel.deleteCalendar(caldavAccount, caldavCalendar)
}