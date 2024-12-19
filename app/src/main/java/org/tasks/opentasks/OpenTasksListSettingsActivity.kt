package org.tasks.opentasks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.settings.ProgressBar
import org.tasks.compose.settings.SelectIconRow
import org.tasks.compose.settings.SettingsSurface
import org.tasks.compose.settings.Toaster
import org.tasks.compose.settings.Toolbar
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TasksTheme {
                SettingsSurface {
                    Toolbar(
                        title = toolbarTitle,
                        save = { lifecycleScope.launch { save() } },
                        optionButton = { },
                    )
                    ProgressBar(showProgress.value)
                    SelectIconRow(icon = selectedIcon.value?: defaultIcon) { showIconPicker() }
                }
                Toaster(state = snackbar)
            }
        } /* setContent */
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {}

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) {}
}