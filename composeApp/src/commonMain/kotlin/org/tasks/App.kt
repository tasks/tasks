package org.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.auth.OAuthProvider
import org.tasks.auth.TasksServerEnvironment
import org.tasks.compose.SignInProvider
import org.tasks.compose.SignInProviderDialog
import org.tasks.compose.WelcomeScreenLayout
import org.tasks.compose.accounts.AddAccountScreen
import org.tasks.compose.accounts.Platform
import org.tasks.themes.TasksTheme
import org.tasks.viewmodel.AddAccountViewModel
import org.tasks.viewmodel.AppViewModel

@Serializable
data object WelcomeDestination : NavKey

@Serializable
data object AddAccountDestination : NavKey

@Serializable
data object TaskListDestination : NavKey

@Composable
fun App(
    openUrl: (String) -> Unit = {},
    environments: List<TasksServerEnvironment.Environment> = emptyList(),
    currentEnvironment: String = TasksServerEnvironment.ENV_PRODUCTION,
    onSelectEnvironment: (String) -> Unit = {},
) {
    TasksTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val appViewModel = koinViewModel<AppViewModel>()
            val configuration = koinInject<PlatformConfiguration>()
            val hasAccount by appViewModel.hasAccount.collectAsState()

            if (hasAccount == null) {
                return@Surface
            }

            val backStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(WelcomeDestination::class, WelcomeDestination.serializer())
                            subclass(AddAccountDestination::class, AddAccountDestination.serializer())
                            subclass(TaskListDestination::class, TaskListDestination.serializer())
                        }
                    }
                },
                if (hasAccount == true) TaskListDestination else WelcomeDestination,
            )

            LaunchedEffect(hasAccount) {
                when (hasAccount) {
                    true -> {
                        if (backStack.lastOrNull() !is TaskListDestination) {
                            backStack.clear()
                            backStack.add(TaskListDestination)
                        }
                    }
                    false -> {
                        if (backStack.lastOrNull() !is WelcomeDestination
                            && backStack.lastOrNull() !is AddAccountDestination) {
                            backStack.clear()
                            backStack.add(WelcomeDestination)
                        }
                    }
                    null -> {}
                }
            }

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider {
                    entry<WelcomeDestination> {
                        WelcomeScreenLayout(
                            showLegalDisclosure = !configuration.isLibre,
                            showImportBackup = configuration.supportsBackupImport,
                            onSignIn = {
                                backStack.add(AddAccountDestination)
                            },
                            onContinueWithoutSync = {
                                appViewModel.continueWithoutSync()
                            },
                            openLegalUrl = openUrl,
                            environments = environments,
                            currentEnvironment = currentEnvironment,
                            onSelectEnvironment = onSelectEnvironment,
                        )
                    }
                    entry<AddAccountDestination> {
                        val addAccountViewModel = koinViewModel<AddAccountViewModel>()
                        var showProviderPicker by remember { mutableStateOf(false) }
                        AddAccountScreen(
                            configuration = configuration,
                            hasTasksAccount = false,
                            hasPro = false,
                            needsConsent = false,
                            onBack = { backStack.removeLastOrNull() },
                            signIn = { platform ->
                                when (platform) {
                                    Platform.TASKS_ORG -> showProviderPicker = true
                                    else -> addAccountViewModel.signIn(platform)
                                }
                            },
                            openUrl = { platform ->
                                // TODO: handle open URL for platform
                            },
                            openLegalUrl = openUrl,
                        )
                        if (showProviderPicker) {
                            Dialog(onDismissRequest = { showProviderPicker = false }) {
                                SignInProviderDialog(
                                    onSelected = { provider ->
                                        showProviderPicker = false
                                        val oauthProvider = when (provider) {
                                            SignInProvider.GOOGLE -> OAuthProvider.GOOGLE
                                            SignInProvider.GITHUB -> OAuthProvider.GITHUB
                                        }
                                        addAccountViewModel.signIn(
                                            platform = Platform.TASKS_ORG,
                                            provider = oauthProvider,
                                            openUrl = openUrl,
                                        )
                                    },
                                    onHelp = {
                                        showProviderPicker = false
                                        openUrl("https://tasks.org/docs/sync")
                                    },
                                    onCancel = { showProviderPicker = false },
                                )
                            }
                        }
                    }
                    entry<TaskListDestination> {
                        TaskListScreen()
                    }
                },
            )
        }
    }
}

@Composable
private fun TaskListScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Task List",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
