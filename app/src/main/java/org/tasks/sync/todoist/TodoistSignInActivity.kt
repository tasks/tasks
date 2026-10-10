package org.tasks.sync.todoist

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.compose.components.SymbolIcon
import org.tasks.compose.settings.TodoistAccountScreen
import org.tasks.extensions.Context.openUri
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.back
import javax.inject.Inject

/**
 * Collects a Todoist API token.
 *
 * Todoist publishes no OIDC discovery document, so the AppAuth-based flow the
 * Microsoft sign-in uses doesn't apply. A pasted personal token works identically on
 * every platform and needs no registered OAuth client.
 */
@AndroidEntryPoint
class TodoistSignInActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: TodoistAccountSettingsHiltViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val state by viewModel.state.collectAsState()
                val finished by viewModel.finished.collectAsState()
                var showDiscardDialog by remember { mutableStateOf(false) }

                LaunchedEffect(finished) {
                    if (finished) {
                        setResult(Activity.RESULT_OK)
                        finish()
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
                                            finish()
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
                            onNavigateBack = { finish() },
                            onDiscardDialogChange = { showDiscardDialog = it },
                            onDismissSnackbar = viewModel::dismissSnackbar,
                            onOpenTokenHelp = { openUri(TasksUrls.TODOIST_API_TOKEN) },
                        )
                    }
                }
            }
        }
    }
}
