package org.tasks.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.components.SymbolIcon
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.kmp.formatTime
import org.tasks.resources.getString
import org.tasks.themes.TasksIcons
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.is24HourFormat
import org.tasks.time.withMillisOfDay
import org.tasks.viewmodel.DateAndTimeViewModel
import org.tasks.viewmodel.DateAndTimeViewModel.QuickPickSlot
import org.tasks.viewmodel.time
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.always_display_full_date
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.date_and_time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAndTimeDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<DateAndTimeViewModel>()
    val is24Hour = is24HourFormat()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferences = viewModel.preferences

    viewModel.orderError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                getString(error.message, getString(error.slot), getString(error.other))
            )
            viewModel.dismissOrderError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.date_and_time)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.always_display_full_date),
                    checked = preferences.alwaysDisplayFullDate,
                    onCheckedChange = { viewModel.updateFullDate(it) },
                )
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                QuickPickSlot.entries.forEach { slot ->
                    SettingsItemCard(
                        position = when (slot) {
                            QuickPickSlot.entries.first() -> CardPosition.First
                            QuickPickSlot.entries.last() -> CardPosition.Last
                            else -> CardPosition.Middle
                        },
                    ) {
                        PreferenceRow(
                            title = stringResource(slot.label),
                            summary = formatTime(
                                currentTimeMillis().withMillisOfDay(preferences.time(slot)),
                                is24Hour,
                            ),
                            onClick = { viewModel.openTimePicker(slot) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }
    }

    viewModel.timePickerSlot?.let { slot ->
        key(slot) {
            val initial = preferences.time(slot)
            TimePickerDialog(
                state = rememberTimePickerState(
                    initialHour = initial / (60 * 60_000),
                    initialMinute = (initial / 60_000) % 60,
                    is24Hour = is24Hour,
                ),
                initialDisplayMode = if (preferences.timePickerInputMode) {
                    DisplayMode.Input
                } else {
                    DisplayMode.Picker
                },
                setDisplayMode = { viewModel.updateTimePickerInputMode(it == DisplayMode.Input) },
                selected = { viewModel.setTime(slot, it) },
                dismiss = { viewModel.dismissTimePicker() },
            )
        }
    }
}
