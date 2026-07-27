package org.tasks.compose.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.touchlab.kermit.Logger
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.RepeatRuleSummary
import org.tasks.compose.pickers.BasicRecurrence
import org.tasks.compose.pickers.BasicRecurrenceOption
import org.tasks.compose.pickers.CustomRecurrence
import org.tasks.compose.pickers.isCustomRecurrence
import org.tasks.compose.rememberRepeatRuleSummary
import org.tasks.repeats.CustomRecurrenceViewModel
import org.tasks.repeats.RecurrenceUtils.newRecur

private val CustomRecurrenceMaxWidth = 420.dp
private val CustomRecurrenceMaxHeight = 640.dp
private val BasicOptionsVerticalPadding = 16.dp
private val BasicOptionsContentPadding = PaddingValues(start = 6.dp, end = 16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePickerDialog(
    recurrence: String?,
    dueDate: Long,
    accountType: Int,
    calendarInputMode: Boolean,
    onCalendarInputModeChange: (Boolean) -> Unit,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCustom by remember(recurrence) { mutableStateOf(false) }
    var customToken by remember { mutableStateOf(0) }
    if (showCustom) {
        CustomRecurrenceDialog(
            viewModelKey = "custom-recurrence-$customToken",
            recurrence = recurrence,
            dueDate = dueDate,
            accountType = accountType,
            calendarInputMode = calendarInputMode,
            onCalendarInputModeChange = onCalendarInputModeChange,
            onSelected = onSelected,
            onDismiss = { showCustom = false },
        )
        return
    }
    PlatformBackHandler(enabled = true, onBack = onDismiss)
    val recur = remember(recurrence) {
        recurrence
            ?.takeIf { it.isNotBlank() }
            ?.let {
                try {
                    newRecur(it)
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to parse $it" }
                    null
                }
            }
    }
    val customPicked = recur?.isCustomRecurrence() == true
    val summary = rememberRepeatRuleSummary(recurrence.takeIf { customPicked })
    if (summary is RepeatRuleSummary.Loading) {
        return
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(modifier = Modifier.padding(vertical = BasicOptionsVerticalPadding)) {
                BasicRecurrence(
                    customLabel = (summary as? RepeatRuleSummary.Repeats)?.text,
                    selectedFrequency = if (customPicked) null else recur?.frequency,
                    contentPadding = BasicOptionsContentPadding,
                    onSelected = { option ->
                        when (option) {
                            BasicRecurrenceOption.KeepCustom -> onDismiss()
                            BasicRecurrenceOption.DoesNotRepeat -> onSelected(null)
                            is BasicRecurrenceOption.Frequency -> onSelected(
                                newRecur()
                                    .apply {
                                        interval = 1
                                        setFrequency(option.frequency.name)
                                    }
                                    .toString()
                            )
                            BasicRecurrenceOption.Custom -> {
                                customToken++
                                showCustom = true
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CustomRecurrenceDialog(
    viewModelKey: String,
    recurrence: String?,
    dueDate: Long,
    accountType: Int,
    calendarInputMode: Boolean,
    onCalendarInputModeChange: (Boolean) -> Unit,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = koinViewModel<CustomRecurrenceViewModel>(
        key = viewModelKey,
        parameters = { parametersOf(recurrence.orEmpty(), dueDate, accountType) },
    )
    val state by viewModel.state.collectAsState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(24.dp)
                .sizeIn(
                    maxWidth = CustomRecurrenceMaxWidth,
                    maxHeight = CustomRecurrenceMaxHeight,
                ),
        ) {
            CustomRecurrence(
                state = state,
                save = { onSelected(viewModel.getRecur()) },
                discard = onDismiss,
                setInterval = viewModel::setInterval,
                setSelectedFrequency = viewModel::setFrequency,
                setEndDate = viewModel::setEndDate,
                setSelectedEndType = viewModel::setEndType,
                setOccurrences = viewModel::setOccurrences,
                toggleDay = viewModel::toggleDay,
                setMonthSelection = viewModel::setMonthSelection,
                calendarDisplayMode = if (calendarInputMode) {
                    DisplayMode.Input
                } else {
                    DisplayMode.Picker
                },
                setDisplayMode = { onCalendarInputModeChange(it == DisplayMode.Input) },
            )
        }
    }
}
