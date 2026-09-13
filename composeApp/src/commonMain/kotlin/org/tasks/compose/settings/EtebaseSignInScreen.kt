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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.EtebaseAccountSettingsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.silentsuite
import tasks.kmp.generated.resources.etesync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtebaseSignInScreen(
    silentSuite: Boolean = false,
    onNavigateBack: () -> Unit,
    onAccountCreated: () -> Unit,
) {
    val viewModel = koinViewModel<EtebaseAccountSettingsViewModel>()
    androidx.compose.runtime.LaunchedEffect(silentSuite) {
        viewModel.setService(if (silentSuite) org.tasks.data.entity.EtebaseService.SILENTSUITE else org.tasks.data.entity.EtebaseService.ETESYNC)
    }
    val state by viewModel.state.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (silentSuite) Res.string.silentsuite else Res.string.etesync)) },
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
            EtebaseAccountScreen(
                state = state,
                isNewAccount = true,
                accountName = "",
                showDiscardDialog = showDiscardDialog,
                onUrlChange = viewModel::setUrl,
                onUsernameChange = viewModel::setUsername,
                onPasswordChange = viewModel::setPassword,
                onNameChange = {},
                onShowUrlChange = viewModel::setShowUrl,
                onSave = { viewModel.save(onAccountCreated) },
                onDelete = {},
                onNavigateBack = onNavigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
                onDismissSnackbar = viewModel::dismissSnackbar,
            )
        }
    }
}
