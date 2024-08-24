package org.tasks.opentasks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.ListSettings.ProgressBar
import org.tasks.compose.ListSettings.SettingsSnackBar
import org.tasks.compose.ListSettings.ListSettingsSurface
import org.tasks.compose.ListSettings.Toolbar
import org.tasks.compose.ListSettings.SelectIconRow
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TasksTheme {
                ListSettingsSurface {
                    Toolbar(
                        title = toolbarTitle,
                        save = { lifecycleScope.launch { save() } },
                        optionButton = { },
                    )
                    ProgressBar(showProgress)
                    SelectIconRow(icon = selectedIcon) {
                        showIconPicker()
                    }
                }
                SettingsSnackBar(state = snackbar)
            }
        } /* setContent */
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {}

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) {}
}