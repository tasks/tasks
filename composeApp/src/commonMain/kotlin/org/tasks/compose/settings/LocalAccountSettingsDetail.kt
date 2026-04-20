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
import org.tasks.viewmodel.LocalAccountViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.local_lists

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAccountSettingsDetail(
    pane: LocalAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<LocalAccountViewModel>()
    remember(pane.account.id) { viewModel.setAccount(pane.account) }
    val displayName by viewModel.displayName.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val taskCount by viewModel.taskCount.collectAsState()
    val account by viewModel.account.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        account?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.local_lists)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
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
            LocalAccountScreen(
                displayName = displayName,
                nameError = nameError,
                taskCount = taskCount,
                accountName = account?.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.local_lists),
                hasChanges = hasChanges,
                showDiscardDialog = showDiscardDialog,
                onNameChange = viewModel::setDisplayName,
                onSave = { errorMessage ->
                    viewModel.save(errorMessage, onNavigateBack)
                },
                onDelete = {
                    viewModel.delete(onNavigateBack)
                },
                onNavigateBack = onNavigateBack,
                onDiscardDialogChange = { showDiscardDialog = it },
            )
        }
    }
}
