package org.tasks.caldav

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.settings.CaldavAccountScreen
import org.tasks.preferences.fragments.CaldavAccountSettingsHiltViewModel
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.back
import javax.inject.Inject

@AndroidEntryPoint
class CaldavSignInActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: CaldavAccountSettingsHiltViewModel by viewModels()

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
                var showDiscardDialog by remember { mutableStateOf(false) }

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
                            isNewAccount = true,
                            accountName = "",
                            showDiscardDialog = showDiscardDialog,
                            onUrlChange = viewModel::setUrl,
                            onUsernameChange = viewModel::setUsername,
                            onPasswordChange = viewModel::setPassword,
                            onNameChange = {},
                            onServerTypeChange = viewModel::setServerType,
                            onSave = {
                                viewModel.save {
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                            },
                            onDelete = {},
                            onNavigateBack = { finish() },
                            onDiscardDialogChange = { showDiscardDialog = it },
                            onDismissSnackbar = viewModel::dismissSnackbar,
                        )
                    }
                }
            }
        }
    }
}
