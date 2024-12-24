package org.tasks.opentasks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.settings.AddToHomeRow
import org.tasks.compose.settings.ListSettingsScaffold
import org.tasks.compose.settings.SelectIconRow
import org.tasks.compose.settings.Toaster
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class OpenTasksListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TasksTheme {
                val viewState = baseViewModel.viewState.collectAsStateWithLifecycle().value
                ListSettingsScaffold(
                    title = toolbarTitle,
                    theme = if (viewState.colorState == Color.Unspecified)
                        Color(tasksTheme.themeColor.primaryColor)
                    else
                        viewState.colorState,
                    promptDiscard = viewState.promptDiscard,
                    showProgress = viewState.showProgress,
                    dismissDiscardPrompt = { baseViewModel.promptDiscard(false) },
                    save = { lifecycleScope.launch { save() } },
                    discard = { finish() },
                ) {
                    SelectIconRow(icon = viewState.icon ?: defaultIcon) { showIconPicker() }
                    AddToHomeRow(onClick = { createShortcut() })
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