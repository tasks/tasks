package org.tasks.todoist

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class TodoistCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {
    private val createCalendarViewModel: CreateCalendarViewModel by viewModels()
    private val deleteCalendarViewModel: DeleteCalendarViewModel by viewModels()
    private val updateCalendarViewModel: UpdateCalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed)
        deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed)
        updateCalendarViewModel.observe(this, { updateCalendar() }, this::requestFailed)

        setContent {
            TasksTheme {
                BaseCaldavSettingsContent()
            }
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
            createCalendarViewModel.createCalendar(caldavAccount, name, color)

    override suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendarViewModel.updateCalendar(account, calendar, name, color)

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            deleteCalendarViewModel.deleteCalendar(caldavAccount, caldavCalendar)
}
