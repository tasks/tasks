package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.TaskEditPreferencesViewModel
import org.tasks.viewmodel.TaskEditPreferencesViewModel.State
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_edit_screen_options
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.back_button_saves_task
import tasks.kmp.generated.resources.back_button_saves_task_summary

/**
 * The desktop "Edit screen options" settings pane - a mirror of the native
 * Android TaskEditPreferences screen, storing values in the shared
 * TasksPreferences DataStore.
 *
 * Only the "Back button saves task" row ships in this milestone. The other
 * native rows are omitted deliberately (multiline title is a no-op on
 * desktop, the comments row has no desktop comments UI, the "customize"
 * row is the legacy BeastMode screen, and "without unlock" is Android-only).
 * See SPEC-settings-panes.md §3.5 for the omission table.
 */
@Composable
fun TaskEditOptionsDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<TaskEditPreferencesViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.EPr_edit_screen_options)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // Hold off rendering until the persisted value has loaded, so the
        // switch doesn't flash its default (and animate) before the real
        // state arrives.
        if (state.loaded) {
            TaskEditOptionsScreen(
                state = state,
                onBackButtonSavesChange = viewModel::updateBackButtonSaves,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
fun TaskEditOptionsScreen(
    state: State,
    onBackButtonSavesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsContentPadding),
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        SettingsItemCard(position = CardPosition.Only) {
            SwitchPreferenceRow(
                title = stringResource(Res.string.back_button_saves_task),
                summary = stringResource(Res.string.back_button_saves_task_summary),
                checked = state.backButtonSaves,
                onCheckedChange = onBackButtonSavesChange,
            )
        }
        Spacer(modifier = Modifier.height(SettingsContentPadding))
    }
}
