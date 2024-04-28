package org.tasks.opentasks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.Constants
import org.tasks.compose.DeleteButton
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.compose.ListSettings.ListSettingsProgressBar
import org.tasks.compose.ListSettings.ListSettingsSnackBar
import org.tasks.compose.ListSettings.ListSettingsSurface
import org.tasks.compose.ListSettings.ListSettingsToolbar
import org.tasks.compose.ListSettings.SelectIconRow
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MdcTheme {
                ListSettingsSurface {
                    ListSettingsToolbar(
                        title = toolbarTitle,
                        save = { lifecycleScope.launch { save() } },
                        optionButton = { },
                    )
                    ListSettingsProgressBar(showProgress)
                    SelectIconRow(icon = iconState) {
                        showIconPicker()
                    }
                }
                ListSettingsSnackBar(state = snackbar)
            }
        } /* setContent */
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {}

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) {}
}