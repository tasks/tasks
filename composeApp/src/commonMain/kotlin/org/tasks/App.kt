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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.tasks.auth.TasksServerEnvironment
import org.tasks.compose.WelcomeScreenLayout
import org.tasks.data.dao.CaldavDao
import org.tasks.themes.TasksTheme

@Serializable
data object WelcomeDestination : NavKey

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
            val caldavDao = koinInject<CaldavDao>()
            val configuration = koinInject<PlatformConfiguration>()
            val hasAccount by caldavDao.watchAccountExists().collectAsState(initial = null)
            val backStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(WelcomeDestination::class, WelcomeDestination.serializer())
                            subclass(TaskListDestination::class, TaskListDestination.serializer())
                        }
                    }
                },
                WelcomeDestination,
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
                        if (backStack.lastOrNull() !is WelcomeDestination) {
                            backStack.clear()
                            backStack.add(WelcomeDestination)
                        }
                    }
                    null -> {} // loading
                }
            }

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider {
                    entry<WelcomeDestination> {
                        WelcomeScreenLayout(
                            showLegalDisclosure = !configuration.isLibre,
                            onSignIn = {
                                // TODO: sign in flow
                            },
                            onContinueWithoutSync = {
                                // TODO: create local account and navigate
                            },
                            onImportBackup = {
                                // TODO: import backup
                            },
                            openLegalUrl = openUrl,
                            environments = environments,
                            currentEnvironment = currentEnvironment,
                            onSelectEnvironment = onSelectEnvironment,
                        )
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
