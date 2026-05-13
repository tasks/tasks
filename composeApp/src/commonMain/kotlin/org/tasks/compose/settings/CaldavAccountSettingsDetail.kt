package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.CaldavAccountSettingsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.caldav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaldavAccountSettingsDetail(
    pane: CaldavAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<CaldavAccountSettingsViewModel>()
    LaunchedEffect(pane.account.id) { viewModel.setAccount(pane.account) }
    val state by viewModel.state.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val caldavFallback = stringResource(Res.string.caldav)
    val accountName = state.account?.name?.takeIf { it.isNotBlank() } ?: caldavFallback

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
            CaldavAccountScreen(
                state = state,
                isNewAccount = false,
                accountName = accountName,
                showDiscardDialog = showDiscardDialog,
                onUrlChange = viewModel::setUrl,
                onUsernameChange = viewModel::setUsername,
                onPasswordChange = viewModel::setPassword,
                onNameChange = viewModel::setDisplayName,
                onServerTypeChange = viewModel::setServerType,
                onSave = { viewModel.save(onNavigateBack) },
                onDelete = { viewModel.delete(onNavigateBack) },
                onNavigateBack = onNavigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
                onDismissSnackbar = viewModel::dismissSnackbar,
            )
        }
    }
}
