package org.tasks

import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.themes.isDarkTheme
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.edit.AlarmsSection
import org.tasks.compose.edit.DeletePreviewRow
import org.tasks.compose.edit.DeleteTaskPanel
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.DiscardChangesDialog
import org.tasks.compose.edit.DueDateRow
import org.tasks.compose.edit.ListPickerDialog
import org.tasks.compose.edit.MakeParentTaskPanel
import org.tasks.compose.edit.MarkdownEditField
import org.tasks.compose.edit.PrioritySection
import org.tasks.compose.edit.RecurrencePickerDialog
import org.tasks.compose.edit.RepeatRow
import org.tasks.compose.edit.StartDateRow
import org.tasks.compose.edit.SubtasksSection
import org.tasks.compose.edit.TagPickerDialog
import org.tasks.compose.edit.TagsSection
import org.tasks.compose.edit.TaskEditCard
import org.tasks.compose.edit.TaskEditCardRow
import org.tasks.compose.edit.TaskEditCardRowContent
import org.tasks.compose.pickers.DueDatePickerSheet
import org.tasks.compose.pickers.StartDatePickerSheet
import org.tasks.compose.pickers.alarmFromSelection
import org.tasks.compose.pickers.alarmToSelection
import org.tasks.compose.pickers.dueDateFromSelection
import org.tasks.compose.pickers.dueDateToSelection
import org.tasks.time.is24HourFormat
import org.tasks.data.DeletePreview
import org.tasks.data.MakeParentTask
import org.tasks.data.entity.Alarm
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
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
import tasks.kmp.generated.resources.delete_task
import tasks.kmp.generated.resources.failed_to_load_task
import tasks.kmp.generated.resources.make_parent_task
import tasks.kmp.generated.resources.menu_discard_changes
import tasks.kmp.generated.resources.no_list_available
import tasks.kmp.generated.resources.save
import tasks.kmp.generated.resources.sort_list
import tasks.kmp.generated.resources.subtask_list_locked
import tasks.kmp.generated.resources.subtasks_multilevel_google_task
import tasks.kmp.generated.resources.subtasks_multilevel_microsoft
import tasks.kmp.generated.resources.subtasks_will_be_flattened
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
    filterTint: Int = 0,
    onOpenSubtask: (taskId: Long, remoteId: String, isDraft: Boolean) -> Unit = { _, _, _ -> },
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val currentOnClose by rememberUpdatedState(onClose)
    val log = remember { Logger.withTag("TaskEditScreen") }
    LaunchedEffect(viewModel) {
        viewModel.closeEvents.collect { currentOnClose() }
    }
    val loadError by viewModel.loadError.collectAsState()
    val saving by viewModel.saving.collectAsState()
    var showMakeParentPanel by remember { mutableStateOf(false) }
    var makeParentWorking by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    val makeParentScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val taskDao = koinInject<TaskDao>()
    var showDeletePanel by remember { mutableStateOf(false) }
    var deleteRows by remember { mutableStateOf<List<DeletePreviewRow>>(emptyList()) }
    var deleteWorking by remember { mutableStateOf(false) }

    // The delete preview mirrors TaskDeleter.markDeleted exactly (recursive
    // getChildren + read-only exclusion, see DeletePreview) so the panel can
    // never promise to delete something it won't. Drafts aren't in the DB yet,
    // so their staged subtree comes from the pane's state instead.
    val buildDeletePreview: suspend () -> List<DeletePreviewRow> = {
        if (state.task.id > 0) {
            DeletePreview.forTask(taskDao, state.task.id).map { entry ->
                DeletePreviewRow(
                    title = entry.task.title.orEmpty(),
                    indent = entry.depth,
                    completed = entry.task.isCompleted,
                    readOnly = entry.readOnly,
                )
            }
        } else {
            listOf(DeletePreviewRow(title = state.task.title.orEmpty())) +
                    state.subtasks.map { row ->
                        DeletePreviewRow(
                            title = row.node.title.orEmpty(),
                            indent = row.indent,
                            completed = row.completed,
                        )
                    }
        }
    }

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
                title = {
                    // "Move into new parent task", centred as the pane's hero
                    // action. The TopAppBar title slot is left-aligned by
                    // default, so the button is centred inside a full-width
                    // box. Nav (back+save) and actions (discard+delete) are
                    // two icons each, so the slot is symmetric and this is
                    // true centre. Greyed out while there are unsaved edits -
                    // the reparent operates on the saved row, and this action
                    // does not save on the user's behalf. Hidden in practice
                    // on lists that cannot nest (single-level accounts) by
                    // the same gating.
                    val canMakeParent = !state.isLoading &&
                            !state.isDraft &&
                            !saving &&
                            !state.hasChanges &&
                            state.list?.isSingleLevel != true
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = {
                                showDeletePanel = false
                                showMakeParentPanel = !showMakeParentPanel
                            },
                            enabled = canMakeParent,
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            SymbolIcon(
                                name = TasksIcons.DRIVE_FILE_MOVE,
                                contentDescription = stringResource(Res.string.make_parent_task),
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Save floats next to back on the left: the plain save and the
                    // back (save + close) are the two "leave this edit" actions, so
                    // they sit together. Matches the Android edit window (save left).
                    Row {
                        IconButton(onClick = saveAndClose, enabled = !saving) {
                            SymbolIcon(
                                name = TasksIcons.ARROW_BACK,
                                contentDescription = stringResource(Res.string.back),
                            )
                        }
                        // Explicit save (floppy). Back has always been the only save
                        // path (save and close at once); this is the plain save.
                        // Greyed out while clean.
                        IconButton(
                            onClick = { makeParentScope.launch { viewModel.saveCurrentTask() } },
                            enabled = !state.isLoading &&
                                    !state.isDraft &&
                                    !saving &&
                                    state.hasChanges,
                        ) {
                            SymbolIcon(
                                name = TasksIcons.SAVE,
                                contentDescription = stringResource(Res.string.save),
                            )
                        }
                    }
                },
                actions = {
                    // Discard: mirrors the save gating (nothing to discard while
                    // clean). Replaces the three-dot menu item on the old floater.
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            if (state.hasChanges) {
                                showDiscardConfirmation = true
                            } else {
                                viewModel.discardChanges()
                            }
                        },
                        enabled = !state.isLoading &&
                                !state.isDraft &&
                                !saving &&
                                state.hasChanges,
                    ) {
                        SymbolIcon(
                            name = TasksIcons.CLEAR,
                            contentDescription = stringResource(Res.string.menu_discard_changes),
                        )
                    }
                    // Delete: the tree preview panel is the confirmation. Available
                    // for drafts too, as the old floater's delete was; greyed only
                    // while busy. Replaces the three-dot menu item on the old floater.
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            if (deleteWorking) return@IconButton
                            deleteWorking = true
                            makeParentScope.launch {
                                deleteRows = try {
                                    buildDeletePreview()
                                } catch (e: Exception) {
                                    log.w(e) { "Failed to build delete preview" }
                                    listOf(DeletePreviewRow(title = state.task.title.orEmpty()))
                                }
                                deleteWorking = false
                                showMakeParentPanel = false
                                showDeletePanel = true
                            }
                        },
                        enabled = !state.isLoading && !saving,
                    ) {
                        SymbolIcon(
                            name = TasksIcons.DELETE,
                            contentDescription = stringResource(Res.string.delete_task),
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
                    val isDark = isDarkTheme()
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
                    val caldavDao = koinInject<CaldavDao>()
                    val taskSaver = koinInject<TaskSaver>()
                    val taskMover = koinInject<TaskMover>()
                    val taskRequests = koinInject<TaskRequests>()
                    LaunchedEffect(list) { showListPicker = false }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            // The island inset is kept on the sides and bottom, but not the
                            // top: the list pane's first row starts flush at its (overlay)
                            // app bar's bottom edge, so the edit content does the same here
                            // and the two panes' content tops line up.
                            .padding(
                                start = TaskEditIslandInset,
                                end = TaskEditIslandInset,
                                bottom = TaskEditIslandInset,
                            ),
                    ) {
                        if (showMakeParentPanel) {
                            MakeParentTaskPanel(
                                modifier = Modifier.padding(bottom = 16.dp),
                                working = makeParentWorking,
                                keyboardController = keyboardController,
                                onAccept = { title ->
                                    makeParentWorking = true
                                    makeParentScope.launch {
                                        try {
                                            val parent = MakeParentTask.make(
                                                taskDao = taskDao,
                                                caldavDao = caldavDao,
                                                taskSaver = taskSaver,
                                                taskMover = taskMover,
                                                list = list,
                                                title = title,
                                                target = state.task,
                                            )
                                            showMakeParentPanel = false
                                            makeParentWorking = false
                                            // Lands the user on the new parent, which visibly
                                            // contains the just-nested task: the confirmation.
                                            taskRequests.open(
                                                TaskEditDestination(parent.id, parent.uuid),
                                            )
                                        } catch (e: Exception) {
                                            log.e(e) { "Failed to make parent task" }
                                            makeParentWorking = false
                                        }
                                    }
                                },
                                onCancel = {
                                    makeParentWorking = false
                                    showMakeParentPanel = false
                                },
                            )
                        }
                        if (showDeletePanel) {
                            DeleteTaskPanel(
                                modifier = Modifier.padding(bottom = 16.dp),
                                rows = deleteRows,
                                working = deleteWorking,
                                keyboardController = keyboardController,
                                onDelete = {
                                    showDeletePanel = false
                                    viewModel.delete()
                                },
                                onCancel = {
                                    showDeletePanel = false
                                },
                            )
                        }
                        TitleField(
                            title = state.task.title.orEmpty(),
                            onTitleChange = viewModel::setTitle,
                            focusRequester = titleFocusRequester,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (state.isDraft) {
                            TaskEditCard {
                                TaskEditCardRowContent(
                                    value = list.title,
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    title = stringResource(Res.string.sort_list),
                                    icon = listIcon,
                                    iconTint = listTint,
                                    trailing = {
                                        SymbolIcon(
                                            name = TasksIcons.LOCK,
                                            contentDescription = stringResource(
                                                Res.string.subtask_list_locked
                                            ),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                )
                            }
                        } else {
                            TaskEditCardRow(
                                value = list.title,
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                onClick = { showListPicker = true },
                                title = stringResource(Res.string.sort_list),
                                icon = listIcon,
                                iconTint = listTint,
                            )
                        }
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
                        Spacer(modifier = Modifier.height(16.dp))
                        SubtasksSection(
                            subtasks = state.subtasks,
                            focusSubtask = state.focusSubtask,
                            unsupportedMessage = multilevelSubtaskMessage(state),
                            flattenWarning = if (flattensOnSave(state)) {
                                stringResource(Res.string.subtasks_will_be_flattened)
                            } else {
                                null
                            },
                            allowsNesting = state.allowsNesting,
                            bottomInset = TaskEditIslandInset,
                            onAddSubtask = { viewModel.addSubtask() },
                            onAddAfter = { node -> viewModel.addSubtask(after = node) },
                            onOpenSubtask = { node ->
                                onOpenSubtask(node.id, node.task.uuid, node.isNew)
                            },
                            onCompleteSubtask = viewModel::toggleSubtaskComplete,
                            onToggleCollapsed = viewModel::toggleSubtaskCollapsed,
                            onMoveSubtask = viewModel::moveSubtask,
                            onIndentSubtask = viewModel::indentSubtask,
                            onTitleChange = viewModel::setSubtaskTitle,
                            onRemoveSubtask = viewModel::removeSubtask,
                            onBackspaceSubtask = viewModel::backspaceSubtask,
                            onRestoreSubtask = viewModel::restoreSubtask,
                            onSubtaskFocused = viewModel::onSubtaskFocused,
                        )
                    }
                    if (showDiscardConfirmation) {
                        DiscardChangesDialog(
                            onDiscard = {
                                showDiscardConfirmation = false
                                viewModel.discardChanges()
                            },
                            onDismiss = { showDiscardConfirmation = false },
                        )
                    }
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

internal enum class MultilevelSubtaskLimit { GoogleTasks, Microsoft }

internal fun multilevelSubtaskLimit(state: TaskEditViewModel.State): MultilevelSubtaskLimit? {
    val list = state.list ?: return null
    if (state.task.parent <= 0 && !state.isDraft) {
        return null
    }
    if (list.isIcalendar) {
        return null
    }
    val original = state.originalList
    if (original != null && (original.isIcalendar || original.uuid != list.uuid)) {
        return null
    }
    return if (list.isGoogleTasks) {
        MultilevelSubtaskLimit.GoogleTasks
    } else {
        MultilevelSubtaskLimit.Microsoft
    }
}

internal fun flattensOnSave(state: TaskEditViewModel.State): Boolean =
    state.list?.isSingleLevel == true && state.subtasksNested

@Composable
private fun multilevelSubtaskMessage(state: TaskEditViewModel.State): String? =
    when (multilevelSubtaskLimit(state)) {
        null -> null
        MultilevelSubtaskLimit.GoogleTasks ->
            stringResource(Res.string.subtasks_multilevel_google_task)
        MultilevelSubtaskLimit.Microsoft ->
            stringResource(Res.string.subtasks_multilevel_microsoft)
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
