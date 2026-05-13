package org.tasks

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.settings.Toaster
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.failed_to_save_task
import org.tasks.compose.edit.DescriptionRow
import org.tasks.compose.edit.MarkdownEditField
import org.tasks.viewmodel.TaskEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    taskId: Long?,
    onClose: () -> Unit,
) {
    LaunchedEffect(taskId) {
        viewModel.initialize(taskId)
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
                        text = if (state.isNew) "New task" else "Edit task",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                    text = "No CalDAV list available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    TitleField(
                        title = state.task.title.orEmpty(),
                        onTitleChange = viewModel::setTitle,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DescriptionRow(
                        description = state.task.notes.orEmpty(),
                        onDescriptionChange = viewModel::setDescription,
                    )
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
        placeholder = "Task title",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}
