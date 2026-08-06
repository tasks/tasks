package org.tasks.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.tasks.NewListDialogHost
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.compose.edit.ListPickerDialog
import org.tasks.compose.edit.RecurrencePickerDialog
import org.tasks.compose.edit.TagPickerDialog
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.tags.TagPickerViewModel
import org.tasks.themes.TasksIcons
import org.tasks.time.is24HourFormat
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.TaskDefaultsViewModel
import org.tasks.viewmodel.resolveColor
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.task_defaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDefaultsDetail(
    onNavigateBack: () -> Unit,
    onSignIn: () -> Unit = {},
    onSubscribe: () -> Unit = {},
    onAddAccount: () -> Unit = {},
) {
    val viewModel = koinViewModel<TaskDefaultsViewModel>()
    var showListPicker by rememberSaveable { mutableStateOf(false) }
    var showTagPicker by rememberSaveable { mutableStateOf(false) }
    var showRecurrencePicker by rememberSaveable { mutableStateOf(false) }
    var showDefaultReminders by rememberSaveable { mutableStateOf(false) }
    var newListAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    val isDark = isSystemInDarkTheme()
    val is24HourFormat = is24HourFormat()
    val refreshBroadcaster = koinInject<ComposeRefreshBroadcaster>()

    LaunchedEffect(Unit) {
        viewModel.refreshState()
        refreshBroadcaster.refreshes.collect { viewModel.refreshState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.task_defaults)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TaskDefaultsContent(
                viewModel = viewModel,
                is24HourFormat = is24HourFormat,
                onDefaultList = { showListPicker = true },
                onDefaultTags = { showTagPicker = true },
                onCalendar = {},
                onRecurrence = { showRecurrencePicker = true },
                onReminders = { showDefaultReminders = true },
                onLocation = {},
            )
        }
    }

    if (showListPicker && viewModel.loaded) {
        val filterPickerViewModel = rememberFilterPickerViewModel()
        val pickerState by filterPickerViewModel.viewState.collectAsState()
        val searching = pickerState.query.isNotBlank()
        val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
        val dismiss = {
            showListPicker = false
            filterPickerViewModel.onQueryChange("")
        }
        ListPickerDialog(
            filters = if (searching) pickerState.searchResults else pickerState.filters,
            query = pickerState.query,
            onQueryChange = filterPickerViewModel::onQueryChange,
            selected = viewModel.defaultListFilter,
            onClick = { filter ->
                when (filter) {
                    is NavigationDrawerSubheader -> filterPickerViewModel.onClick(filter)
                    is CaldavFilter -> {
                        viewModel.setDefaultList(filter)
                        dismiss()
                    }
                }
            },
            getIcon = { filterPickerViewModel.getIcon(it) },
            getColor = { filterPickerViewModel.getColor(it.tint, isDark) ?: onSurfaceArgb },
            onAddClick = { header ->
                header.id.toLongOrNull()?.let { accountId ->
                    dismiss()
                    newListAccountId = accountId
                }
            },
            onSignIn = {
                dismiss()
                onSignIn()
            },
            onDismiss = dismiss,
        )
    }

    NewListDialogHost(
        accountId = newListAccountId,
        isDark = isDark,
        onDismiss = { created ->
            newListAccountId = null
            created?.let { viewModel.setDefaultList(it) }
        },
        onSubscribe = onSubscribe,
        onAddAccount = onAddAccount,
    )

    if (showTagPicker && viewModel.loaded) {
        val tagPickerViewModel = koinViewModel<TagPickerViewModel>(
            key = "task_defaults_tag_picker",
        )
        val purchaseState = koinInject<PurchaseState>()
        val surfaceVariantArgb = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        val initialTags by produceState<List<TagData>?>(null, viewModel.settings.defaultTags) {
            value = viewModel.defaultTags()
        }
        initialTags?.let { tags ->
            TagPickerDialog(
                viewModel = tagPickerViewModel,
                initialTags = tags,
                getTagIcon = { it.icon ?: TasksIcons.LABEL },
                getTagColor = { tag ->
                    Color(
                        resolveColor(tag.color ?: 0, isDark, purchaseState) ?: surfaceVariantArgb
                    )
                },
                onDismiss = { selected ->
                    viewModel.setDefaultTags(selected)
                    showTagPicker = false
                },
            )
        }
    }

    if (showRecurrencePicker && viewModel.loaded) {
        RecurrencePickerDialog(
            recurrence = viewModel.settings.defaultRecurrence,
            dueDate = 0,
            accountType = CaldavAccount.TYPE_LOCAL,
            calendarInputMode = false,
            onCalendarInputModeChange = {},
            onSelected = {
                viewModel.setRecurrence(it)
                showRecurrencePicker = false
            },
            onDismiss = { showRecurrencePicker = false },
        )
    }

    if (showDefaultReminders && viewModel.loaded) {
        DefaultRemindersDialog(
            vm = koinViewModel<ReminderControlSetViewModel>(key = "task_defaults_reminders"),
            initialAlarms = viewModel.settings.defaultAlarms,
            is24HourFormat = is24HourFormat,
            onAlarmsChanged = { viewModel.setDefaultAlarms(it) },
            onDismiss = { showDefaultReminders = false },
        )
    }
}

@Composable
private fun rememberFilterPickerViewModel(): FilterPickerViewModel =
    koinViewModel(
        key = "task_defaults_list_picker",
        parameters = { parametersOf(true) },
    )
