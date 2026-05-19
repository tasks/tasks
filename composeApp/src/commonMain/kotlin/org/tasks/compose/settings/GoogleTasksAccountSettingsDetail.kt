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
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.GoogleTasksAccountViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.gtasks_GPr_header

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleTasksAccountSettingsDetail(
    pane: GoogleTasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<GoogleTasksAccountViewModel>()
    LaunchedEffect(pane.account.id) { viewModel.setAccount(pane.account) }
    val state by viewModel.state.collectAsState()
    val accountName = state.account?.name ?: stringResource(Res.string.gtasks_GPr_header)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            GoogleTasksAccountScreen(
                error = state.error,
                isUnauthorized = state.isUnauthorized,
                accountName = accountName,
                onSignIn = { /* not supported on desktop */ },
                onDelete = { viewModel.delete(onNavigateBack) },
            )
        }
    }
}
