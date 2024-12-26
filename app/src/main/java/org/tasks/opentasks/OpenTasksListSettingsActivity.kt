package org.tasks.opentasks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.compose.settings.AddShortcutToHomeRow
import org.tasks.compose.settings.AddWidgetToHomeRow
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
                val color = if (viewState.color == 0) MaterialTheme.colorScheme.primary else Color(viewState.color)
                ListSettingsScaffold(
                    title = toolbarTitle,
                    color = color,
                    promptDiscard = viewState.promptDiscard,
                    showProgress = viewState.showProgress,
                    dismissDiscardPrompt = { baseViewModel.promptDiscard(false) },
                    save = { lifecycleScope.launch { save() } },
                    discard = { finish() },
                ) {
                    SelectIconRow(icon = viewState.icon ?: defaultIcon) { showIconPicker() }
                    AddShortcutToHomeRow(onClick = { createShortcut(color) })
                    AddWidgetToHomeRow(onClick = { createWidget() })
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