/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package org.tasks.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.compose.layout.AppScaffold
import org.tasks.presentation.screens.MenuScreen
import org.tasks.presentation.screens.MenuViewModel
import org.tasks.presentation.screens.SettingsScreen
import org.tasks.presentation.screens.SettingsViewModel
import org.tasks.presentation.screens.TaskListScreen
import org.tasks.presentation.screens.TaskListViewModel
import org.tasks.presentation.theme.TasksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            TasksTheme {
                AppScaffold(
                    timeText = { TimeText() },
                    modifier = Modifier.background(MaterialTheme.colors.background),
                ) {
                    val navController = rememberSwipeDismissableNavController()
                    val taskListViewModel: TaskListViewModel = viewModel()
                    val taskListItems = taskListViewModel.uiItems.collectAsLazyPagingItems()
                    val settingsViewModel: SettingsViewModel = viewModel()
                    SwipeDismissableNavHost(
                        startDestination = "task_list",
                        navController = navController,
                    ) {
                        composable("task_list") {
                            TaskListScreen(
                                uiItems = taskListItems,
                                toggleGroup = { value, collapsed ->
                                    taskListViewModel.toggleGroup(value, collapsed)
                                },
                                onComplete = { id, completed ->
                                    taskListViewModel.completeTask(id, completed)
                                },
                                openTask = { navController.navigate("task_edit/$it") },
                                addTask = {},
                                openMenu = { navController.navigate("menu") },
                                openSettings = { navController.navigate("settings") },
                                toggleSubtasks = { id, collapsed ->
                                    taskListViewModel.toggleSubtasks(id, collapsed)
                                }
                            )
                        }
                        composable(
                            route = "task_edit/{taskId}",
                            arguments = listOf(
                                navArgument("taskId") { type = NavType.StringType }
                            )
                        ) {
                            val taskId = it.arguments?.getString("taskId")
                            WearApp(taskId ?: "invalid id")
                        }
                        composable(
                            route = "menu",
                        ) { navBackStackEntry ->
                            val menuViewModel: MenuViewModel = viewModel(navBackStackEntry)
                            MenuScreen(
                                items = menuViewModel.uiItems.collectAsLazyPagingItems(),
                                selectFilter = {
                                    settingsViewModel.setFilter(it.id)
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(
                            route = "settings",
                        ) {
                            val viewState =
                                settingsViewModel.viewState.collectAsStateWithLifecycle().value
                            if (viewState.initialized) {
                                SettingsScreen(
                                    showHidden = viewState.settings.showHidden,
                                    showCompleted = viewState.settings.showCompleted,
                                    toggleShowHidden = { settingsViewModel.setShowHidden(it) },
                                    toggleShowCompleted = { settingsViewModel.setShowCompleted(it) },
                                )
                            } else {
                                // TODO: show spinner
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    TasksTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = "id=$greetingName"
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}
