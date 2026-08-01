package org.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.edit.AlarmsSection
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.DueDateRow
import org.tasks.compose.edit.ListPickerDialog
import org.tasks.compose.edit.MarkdownEditField
import org.tasks.compose.edit.PrioritySection
import org.tasks.compose.edit.RecurrencePickerDialog
import org.tasks.compose.edit.RepeatRow
import org.tasks.compose.edit.StartDateRow
import org.tasks.compose.edit.TagPickerDialog
import org.tasks.compose.edit.TagsSection
import org.tasks.compose.edit.TaskEditActionBar
import org.tasks.compose.edit.TaskEditActionBarHeight
import org.tasks.compose.edit.TaskEditCardRow
import org.tasks.compose.pickers.DueDatePickerSheet
import org.tasks.compose.pickers.StartDatePickerSheet
import org.tasks.compose.pickers.alarmFromSelection
import org.tasks.compose.pickers.alarmToSelection
import org.tasks.compose.pickers.dueDateFromSelection
import org.tasks.compose.pickers.dueDateToSelection
import org.tasks.time.is24HourFormat
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.filters.CaldavFilter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.tags.TagPickerViewModel
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.noon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.TaskEditViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.failed_to_load_task
import tasks.kmp.generated.resources.no_list_available
import tasks.kmp.generated.resources.sort_list
import tasks.kmp.generated.resources.task_title

val TaskEditIslandInset = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    filterPickerViewModel: FilterPickerViewModel,
    onCreateList: (accountId: Long) -> Unit = {},
    onSignIn: () -> Unit = {},
    backHandlerEnabled: Boolean = true,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.closeEvents.collect { currentOnClose() }
    }
    val loadError by viewModel.loadError.collectAsState()
    val saving by viewModel.saving.collectAsState()

    val saveAndClose = { viewModel.save() }

    // Armed unless the caller says something is on top of this screen. In a list-detail scene
    // neither NavDisplay nor the task list chrome has a back handler enabled, so leaving this one
    // off while the row loads would let back exit the app instead of closing the editor. save() is
    // a no-op while loading and still emits the close.
    PlatformBackHandler(enabled = backHandlerEnabled) { saveAndClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = saveAndClose, enabled = !saving) {
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
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
                loadError -> Text(
                    text = stringResource(Res.string.failed_to_load_task),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
                state.list == null -> Text(
                    text = stringResource(Res.string.no_list_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
                else -> {
                    val titleFocusRequester = remember { FocusRequester() }
                    if (state.isNew) {
                        LaunchedEffect(Unit) {
                            titleFocusRequester.requestFocus()
                        }
                    }
                    val list = state.list!!
                    val isDark = isSystemInDarkTheme()
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    val listTint = remember(list, isDark) {
                        val color = filterPickerViewModel.getColor(list.tint, isDark)
                        if (color != null) Color(color) else onSurface
                    }
                    val listIcon = remember(list) { filterPickerViewModel.getIcon(list) }
                    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                    val surfaceVariantArgb = remember(surfaceVariant) { surfaceVariant.toArgb() }
                    val tagChipColorProvider = remember(isDark, surfaceVariantArgb) {
                        { tint: Int ->
                            filterPickerViewModel.getColor(tint, isDark) ?: surfaceVariantArgb
                        }
                    }
                    var showListPicker by remember { mutableStateOf(false) }
                    var showTagPicker by remember { mutableStateOf(false) }
                    var showDueDatePicker by remember { mutableStateOf(false) }
                    var showStartDatePicker by remember { mutableStateOf(false) }
                    var showRecurrencePicker by remember { mutableStateOf(false) }
                    var showAlarmDateTimePicker by remember { mutableStateOf(false) }
                    var alarmToReplace by remember { mutableStateOf<Alarm?>(null) }
                    var pickerToken by remember { mutableStateOf(0) }
                    val reminderViewModel = koinViewModel<ReminderControlSetViewModel>()
                    val is24Hour = is24HourFormat()
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(list) { showListPicker = false }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(TaskEditIslandInset),
                    ) {
                        TitleField(
                            title = state.task.title.orEmpty(),
                            onTitleChange = viewModel::setTitle,
                            focusRequester = titleFocusRequester,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TaskEditCardRow(
                            value = list.title,
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { showListPicker = true },
                            title = stringResource(Res.string.sort_list),
                            icon = listIcon,
                            iconTint = listTint,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        StartDateRow(
                            startDate = state.task.hideUntil,
                            selectedDay = state.startDay,
                            selectedTime = state.startTime,
                            hasDueDate = state.task.dueDate > 0,
                            hasStartAlarm = remember(state.alarms) {
                                state.alarms.any { it.type == Alarm.TYPE_REL_START }
                            },
                            is24Hour = is24Hour,
                            alwaysDisplayFullDate = state.datePickerPreferences.alwaysDisplayFullDate,
                            onClick = {
                                keyboardController?.hide()
                                showStartDatePicker = true
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DueDateRow(
                            dueDate = state.task.dueDate,
                            hasDueDateAlarm = remember(state.alarms) {
                                state.alarms.any { it.type == Alarm.TYPE_REL_END }
                            },
                            is24Hour = is24Hour,
                            alwaysDisplayFullDate = state.datePickerPreferences.alwaysDisplayFullDate,
                            onClick = {
                                keyboardController?.hide()
                                showDueDatePicker = true
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AlarmsSection(
                            vm = reminderViewModel,
                            alarms = state.alarms,
                            isNew = state.isNew,
                            hasStartDate = state.task.hasStartDate(),
                            hasDueDate = state.task.hasDueDate(),
                            is24HourFormat = is24Hour,
                            addAlarm = viewModel::addAlarm,
                            deleteAlarm = viewModel::removeAlarm,
                            pickDateAndTime = { replace ->
                                alarmToReplace = replace
                                keyboardController?.hide()
                                showAlarmDateTimePicker = true
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        RepeatRow(
                            recurrence = state.task.recurrence,
                            repeatFrom = state.task.repeatFrom,
                            onClick = {
                                keyboardController?.hide()
                                showRecurrencePicker = true
                            },
                            onRepeatFromChanged = viewModel::setRepeatFrom,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TagsSection(
                            tags = state.tags,
                            colorProvider = tagChipColorProvider,
                            onClick = {
                                pickerToken++
                                showTagPicker = true
                            },
                            onClear = { tag -> viewModel.setTags(state.tags - tag) },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrioritySection(
                            priority = state.task.priority,
                            onPriorityChange = viewModel::setPriority,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DescriptionRow(
                            description = state.task.notes.orEmpty(),
                            onDescriptionChange = viewModel::setDescription,
                        )
                        Spacer(
                            modifier = Modifier.height(
                                TaskEditActionBarHeight + FloatingToolbarBottomMargin
                            )
                        )
                    }
                    TaskEditActionBar(
                        onMarkCompleted = {
                            keyboardController?.hide()
                            viewModel.markComplete()
                        },
                        onDiscardChanges = viewModel::discardChanges,
                        onDeleteTask = viewModel::delete,
                        enabled = !saving,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                horizontal = TaskEditIslandInset,
                                vertical = FloatingToolbarBottomMargin,
                            ),
                    )
                    if (showDueDatePicker) {
                        val (initialDay, initialTime) = dueDateToSelection(state.task.dueDate)
                        // TODO: both date pickers hard-code autoClose
                        DueDatePickerSheet(
                            initialDay = initialDay,
                            initialTime = initialTime,
                            is24Hour = is24Hour,
                            showNoDate = !state.task.isRecurring,
                            times = state.datePickerPreferences.quickPickTimes,
                            initialDateInputMode = state.datePickerPreferences.datePickerInputMode,
                            onDateInputModeChange = viewModel::setDatePickerInputMode,
                            initialTimeInputMode = state.datePickerPreferences.timePickerInputMode,
                            onTimeInputModeChange = viewModel::setTimePickerInputMode,
                            onSelected = { day, time ->
                                viewModel.setDueDate(dueDateFromSelection(day, time))
                                showDueDatePicker = false
                            },
                            onDismiss = { showDueDatePicker = false },
                        )
                    }
                    if (showStartDatePicker) {
                        StartDatePickerSheet(
                            initialDay = state.startDay,
                            initialTime = state.startTime,
                            is24Hour = is24Hour,
                            autoClose = false,
                            showDueDate = state.list?.account?.isOpenTasks != true,
                            times = state.datePickerPreferences.quickPickTimes,
                            initialDateInputMode = state.datePickerPreferences.datePickerInputMode,
                            onDateInputModeChange = viewModel::setDatePickerInputMode,
                            initialTimeInputMode = state.datePickerPreferences.timePickerInputMode,
                            onTimeInputModeChange = viewModel::setTimePickerInputMode,
                            onSelected = { day, time ->
                                viewModel.setStartDate(day, time)
                                showStartDatePicker = false
                            },
                            onDismiss = { showStartDatePicker = false },
                        )
                    }
                    if (showRecurrencePicker) {
                        RecurrencePickerDialog(
                            recurrence = state.task.recurrence,
                            dueDate = state.task.dueDate,
                            accountType = list.account.accountType,
                            calendarInputMode = state.datePickerPreferences.datePickerInputMode,
                            onCalendarInputModeChange = viewModel::setDatePickerInputMode,
                            onSelected = { recurrence ->
                                viewModel.setRecurrence(recurrence)
                                showRecurrencePicker = false
                            },
                            onDismiss = { showRecurrencePicker = false },
                        )
                    }
                    if (showAlarmDateTimePicker) {
                        val existing = alarmToReplace
                            ?.takeIf { it.type == Alarm.TYPE_DATE_TIME }
                            ?.time
                            ?.takeIf { it > 0 }
                        val (initialDay, initialTime) = alarmToSelection(existing ?: currentTimeMillis().noon())
                        DueDatePickerSheet(
                            initialDay = initialDay,
                            initialTime = initialTime,
                            is24Hour = is24Hour,
                            showNoDate = false,
                            showNoTime = false,
                            times = state.datePickerPreferences.quickPickTimes,
                            initialDateInputMode = state.datePickerPreferences.datePickerInputMode,
                            onDateInputModeChange = viewModel::setDatePickerInputMode,
                            initialTimeInputMode = state.datePickerPreferences.timePickerInputMode,
                            onTimeInputModeChange = viewModel::setTimePickerInputMode,
                            onSelected = { day, time ->
                                val timestamp = alarmFromSelection(day, time)
                                if (timestamp > 0) {
                                    alarmToReplace?.let(viewModel::removeAlarm)
                                    viewModel.addAlarm(
                                        Alarm(time = timestamp, type = Alarm.TYPE_DATE_TIME)
                                    )
                                }
                                alarmToReplace = null
                                showAlarmDateTimePicker = false
                            },
                            onDismiss = {
                                alarmToReplace = null
                                showAlarmDateTimePicker = false
                            },
                        )
                    }
                    if (showTagPicker) {
                        val tagPickerViewModel = koinViewModel<TagPickerViewModel>(
                            key = "tag-picker-$pickerToken",
                        )
                        TagPickerDialog(
                            viewModel = tagPickerViewModel,
                            initialTags = state.tags,
                            getTagIcon = { it.icon ?: TasksIcons.LABEL },
                            getTagColor = { tag ->
                                filterPickerViewModel.getColor(tag.color ?: 0, isDark)
                                    ?.let { Color(it) } ?: Color.Unspecified
                            },
                            onDismiss = { selected: List<TagData> ->
                                viewModel.setTags(selected)
                                showTagPicker = false
                            },
                        )
                    }
                    if (showListPicker) {
                        val pickerState by filterPickerViewModel.viewState.collectAsState()
                        val searching = pickerState.query.isNotBlank()
                        val onSurfaceArgb = remember(onSurface) { onSurface.toArgb() }
                        ListPickerDialog(
                            filters = if (searching) pickerState.searchResults else pickerState.filters,
                            query = pickerState.query,
                            onQueryChange = filterPickerViewModel::onQueryChange,
                            selected = list,
                            onClick = { filter ->
                                when (filter) {
                                    is NavigationDrawerSubheader ->
                                        filterPickerViewModel.onClick(filter)
                                    is CaldavFilter -> {
                                        viewModel.setList(filter)
                                        showListPicker = false
                                        filterPickerViewModel.onQueryChange("")
                                    }
                                }
                            },
                            getIcon = { filterPickerViewModel.getIcon(it) },
                            getColor = { filter ->
                                filterPickerViewModel.getColor(filter.tint, isDark)
                                    ?: onSurfaceArgb
                            },
                            onAddClick = { header ->
                                header.id.toLongOrNull()?.let { accountId ->
                                    onCreateList(accountId)
                                }
                            },
                            onSignIn = onSignIn,
                            onDismiss = {
                                showListPicker = false
                                filterPickerViewModel.onQueryChange("")
                            },
                        )
                    }
                }
            }
            // Saving and closing can wait: a save already in flight for this task holds its lock for
            // as long as the calendar provider and sync adapters take, and the wait is
            // uncancellable. Without something on screen, back and escape were consumed and the
            // editor just sat there looking untouched. Takes the pointer input with it, because a
            // screen that is on its way out should not still look editable.
            if (saving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun TitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).sp,
    )
    MarkdownEditField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = titleStyle,
        placeholder = stringResource(Res.string.task_title),
        focusRequester = focusRequester,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}
