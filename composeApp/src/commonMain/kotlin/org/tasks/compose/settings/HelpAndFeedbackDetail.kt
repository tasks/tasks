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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.extensions.restartApplication
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.about
import tasks.kmp.generated.resources.back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndFeedbackDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<HelpAndFeedbackViewModel>()
    val uriHandler = LocalUriHandler.current
    val tasksPreferences = koinInject<TasksPreferences>()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about)) },
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
            HelpAndFeedbackContent(
                viewModel = viewModel,
                openUri = { uriHandler.openUri(it) },
                onRestartApplication = { restartApplication() },
                onCollectStatisticsChanged = { enabled ->
                    scope.launch {
                        tasksPreferences.set(TasksPreferences.collectStatistics, enabled)
                    }
                },
            )
        }
    }
}
