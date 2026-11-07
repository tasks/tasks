package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.EtebaseAccountSettingsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.etesync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtebaseAccountSettingsDetail(
    pane: EtebaseAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<EtebaseAccountSettingsViewModel>()
    LaunchedEffect(pane.accountId) { viewModel.setAccountId(pane.accountId) }
    val state by viewModel.state.collectAsState()
    if (!state.loaded) return
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val etesyncFallback = stringResource(Res.string.etesync)
    val accountName = state.account?.name?.takeIf { it.isNotBlank() } ?: etesyncFallback

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
            EtebaseAccountScreen(
                state = state,
                isNewAccount = false,
                accountName = accountName,
                showDiscardDialog = showDiscardDialog,
                onUrlChange = viewModel::setUrl,
                onUsernameChange = viewModel::setUsername,
                onPasswordChange = viewModel::setPassword,
                onNameChange = viewModel::setDisplayName,
                onShowUrlChange = viewModel::setShowUrl,
                onSave = { viewModel.save(onNavigateBack) },
                onDelete = { viewModel.delete(onNavigateBack) },
                onNavigateBack = onNavigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
                onDismissSnackbar = viewModel::dismissSnackbar,
            )
        }
    }
}
