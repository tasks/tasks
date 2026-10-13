package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.TasksUrls
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.TodoistAccountSettingsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.todoist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoistAccountSettingsDetail(
    pane: TodoistAccountSettingsPane,
    onNavigateBack: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val viewModel = koinViewModel<TodoistAccountSettingsViewModel>()
    LaunchedEffect(pane.account.id) { viewModel.setAccount(pane.account) }
    val state by viewModel.state.collectAsState()
    val finished by viewModel.finished.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val fallback = stringResource(Res.string.todoist)
    val accountName = state.account?.name?.takeIf { it.isNotBlank() } ?: fallback

    LaunchedEffect(finished) {
        if (finished) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.hasChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
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
            TodoistAccountScreen(
                state = state,
                isNewAccount = false,
                accountName = accountName,
                showDiscardDialog = showDiscardDialog,
                actions = todoistAccountActions(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onDiscardDialogChange = { showDiscardDialog = it },
                    onOpenTokenHelp = { openUrl(TasksUrls.TODOIST_API_TOKEN) },
                    onDelete = { viewModel.delete(onNavigateBack) },
                ),
            )
        }
    }
}
