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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.extensions.openSystemNotificationSettings
import org.tasks.time.is24HourFormat
import org.tasks.viewmodel.NotificationsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NotificationsViewModel>()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.notifications)) },
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
            NotificationsContent(
                viewModel = viewModel,
                is24HourFormat = is24HourFormat(),
                openUri = { uriHandler.openUri(it) },
                onMoreSettings = { openSystemNotificationSettings() },
            )
        }
    }
}
