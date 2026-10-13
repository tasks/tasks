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
import androidx.compose.runtime.Composable
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
import org.tasks.compose.settings.todoistAccountActions
import org.tasks.extensions.Context.openUri
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.back
import javax.inject.Inject

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
                SignInContent(
                    viewModel = viewModel,
                    onDone = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onCancel = { finish() },
                    openTokenHelp = { openUri(TasksUrls.TODOIST_API_TOKEN) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInContent(
    viewModel: TodoistAccountSettingsHiltViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    openTokenHelp: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val finished by viewModel.finished.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished) {
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.add_account)) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (state.hasChanges) showDiscardDialog = true else onCancel() }
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
                actions = todoistAccountActions(
                    viewModel = viewModel,
                    onNavigateBack = onCancel,
                    onDiscardDialogChange = { showDiscardDialog = it },
                    onOpenTokenHelp = openTokenHelp,
                ),
            )
        }
    }
}
