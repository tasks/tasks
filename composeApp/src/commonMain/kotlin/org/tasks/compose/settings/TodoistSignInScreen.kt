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
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoistSignInScreen(
    onNavigateBack: () -> Unit,
    onAccountCreated: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val viewModel = koinViewModel<TodoistAccountSettingsViewModel>()
    val state by viewModel.state.collectAsState()
    val finished by viewModel.finished.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished) {
            onAccountCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.add_account)) },
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
                isNewAccount = true,
                accountName = "",
                showDiscardDialog = showDiscardDialog,
                onTokenChange = viewModel::setToken,
                onNameChange = viewModel::setDisplayName,
                onSave = viewModel::save,
                onDelete = {},
                onNavigateBack = onNavigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
                onDismissSnackbar = viewModel::dismissSnackbar,
                onOpenTokenHelp = { openUrl(TasksUrls.TODOIST_API_TOKEN) },
            )
        }
    }
}
