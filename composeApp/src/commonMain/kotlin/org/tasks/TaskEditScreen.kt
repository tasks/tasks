package org.tasks

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
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.ListPickerDialog
import org.tasks.compose.edit.ListPickerRow
import org.tasks.compose.edit.MarkdownEditField
import org.tasks.compose.settings.Toaster
import org.tasks.filters.CaldavFilter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.TaskEditViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.edit_task
import tasks.kmp.generated.resources.failed_to_save_task
import tasks.kmp.generated.resources.new_task
import tasks.kmp.generated.resources.no_list_available
import tasks.kmp.generated.resources.task_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    filterPickerViewModel: FilterPickerViewModel,
    taskId: Long?,
    remoteId: String,
    currentFilter: CaldavFilter? = null,
    onCreateList: (accountId: Long) -> Unit = {},
    onClose: () -> Unit,
) {
    LaunchedEffect(taskId, remoteId) {
        viewModel.initialize(taskId, currentFilter)
    }
    val state by viewModel.state.collectAsState()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.closeEvents.collect { currentOnClose() }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveError by viewModel.saveError.collectAsState()
    val saveErrorMessage = stringResource(Res.string.failed_to_save_task)
    LaunchedEffect(saveError) {
        if (saveError) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            viewModel.clearSaveError()
        }
    }

    val saveAndClose = { viewModel.save() }

    PlatformBackHandler(enabled = !state.isLoading) { saveAndClose() }

    Scaffold(
        snackbarHost = { Toaster(state = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (state.isNew) Res.string.new_task else Res.string.edit_task),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndClose) {
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
                    var showListPicker by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        TitleField(
                            title = state.task.title.orEmpty(),
                            onTitleChange = viewModel::setTitle,
                            focusRequester = titleFocusRequester,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ListPickerRow(
                            listName = list.title,
                            icon = listIcon,
                            tint = listTint,
                            onClick = { showListPicker = true },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DescriptionRow(
                            description = state.task.notes.orEmpty(),
                            onDescriptionChange = viewModel::setDescription,
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
                                    showListPicker = false
                                    filterPickerViewModel.onQueryChange("")
                                    onCreateList(accountId)
                                }
                            },
                            onDismiss = {
                                showListPicker = false
                                filterPickerViewModel.onQueryChange("")
                            },
                        )
                    }
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
