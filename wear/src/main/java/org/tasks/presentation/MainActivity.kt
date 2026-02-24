/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package org.tasks.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.AppScaffold
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.tasks.presentation.screens.MenuScreen
import org.tasks.presentation.screens.MenuViewModel
import org.tasks.presentation.screens.SettingsScreen
import org.tasks.presentation.screens.SettingsViewModel
import org.tasks.presentation.screens.SortPickerScreen
import org.tasks.presentation.screens.TaskEditScreen
import org.tasks.presentation.screens.TaskEditViewModel
import org.tasks.presentation.screens.TaskEditViewModelFactory
import org.tasks.presentation.screens.TaskListScreen
import org.tasks.presentation.screens.TaskListViewModel
import org.tasks.presentation.theme.TasksTheme
import org.tasks.complications.EXTRA_ADD_TASK
import org.tasks.complications.EXTRA_COMPLICATION_FILTER
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.wear_install_app
import tasks.kmp.generated.resources.wear_phone_update_required
import tasks.kmp.generated.resources.wear_connecting_to_phone
import tasks.kmp.generated.resources.wear_unknown_error

class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

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
                    val menuViewModel: MenuViewModel = viewModel()
                    val menuItems = menuViewModel.uiItems.collectAsLazyPagingItems()
                    val settingsViewModel: SettingsViewModel = viewModel()
                    val connected = viewModel.uiState.collectAsStateWithLifecycle().value
                    LifecycleResumeEffect(Unit) {
                        taskListViewModel.invalidate()
                        onPauseOrDispose {}
                    }
                    LaunchedEffect(connected) {
                        when (connected) {
                            NodesActionScreenState.ApiNotAvailable -> {
                                navController.popBackStack()
                                navController.navigate("error")
                            }

                            is NodesActionScreenState.Loaded -> {
                                navController.popBackStack()
                                if (connected.nodeList.any { it.type == NodeTypeUiModel.PHONE && it.appInstalled }) {
                                    if (connected.phoneUpdateRequired) {
                                        val phoneNodeId = connected.nodeList
                                            .first { it.type == NodeTypeUiModel.PHONE && it.appInstalled }
                                            .id
                                        navController.navigate("error?type=${Errors.PHONE_UPDATE_REQUIRED},nodeId=$phoneNodeId")
                                    } else {
                                        intent.getStringExtra(EXTRA_COMPLICATION_FILTER)?.let { complicationFilter ->
                                            settingsViewModel.setFilter(complicationFilter)
                                            intent.removeExtra(EXTRA_COMPLICATION_FILTER)
                                        }
                                        val addTask = intent.getBooleanExtra(EXTRA_ADD_TASK, false)
                                        if (addTask) {
                                            intent.removeExtra(EXTRA_ADD_TASK)
                                        }
                                        navController.navigate("task_list")
                                        if (addTask) {
                                            navController.navigate("task_edit?taskId=0")
                                        }
                                    }
                                } else {
                                    connected
                                        .nodeList
                                        .firstOrNull { it.type == NodeTypeUiModel.UNKNOWN }
                                        ?.let {
                                            navController.navigate("error?type=${Errors.APP_NOT_INSTALLED},nodeId=${it.id}")
                                        }
                                        ?: navController.navigate("error")
                                }
                            }

                            else -> {}
                        }
                    }
                    SwipeDismissableNavHost(
                        startDestination = "loading",
                        navController = navController,
                    ) {
                        composable("loading") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        composable(
                            route = "error?type={type},nodeId={nodeId}",
                            arguments = listOf(
                                navArgument("type") {
                                    type = NavType.EnumType(Errors::class.java)
                                    defaultValue = Errors.UNKNOWN
                                },
                                navArgument("nodeId") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) {
                            val type = it.arguments?.getSerializable("type") as? Errors
                            val nodeId = it.arguments?.getString("nodeId")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (type) {
                                    Errors.APP_NOT_INSTALLED -> {
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                delay(5000)
                                                viewModel.loadNodes()
                                            }
                                        }
                                        Chip(
                                            onClick = { viewModel.installOnNode(nodeId) },
                                            label = {
                                                Text(
                                                    text = stringResource(Res.string.wear_install_app),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                        )
                                    }

                                    Errors.PHONE_UPDATE_REQUIRED -> {
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                delay(5000)
                                                viewModel.loadNodes()
                                            }
                                        }
                                        Chip(
                                            onClick = { viewModel.installOnNode(nodeId) },
                                            label = {
                                                Text(
                                                    text = stringResource(Res.string.wear_phone_update_required),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                        )
                                    }

                                    else -> {
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                delay(5000)
                                                viewModel.loadNodes()
                                            }
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(Res.string.wear_connecting_to_phone),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        composable("task_list") {
                            TaskListScreen(
                                uiItems = taskListItems,
                                toggleGroup = { value, collapsed ->
                                    taskListViewModel.toggleGroup(value, collapsed)
                                },
                                onComplete = { id, completed ->
                                    taskListViewModel.completeTask(id, completed)
                                },
                                openTask = { navController.navigate("task_edit?taskId=$it") },
                                addTask = { navController.navigate("task_edit?taskId=0")},
                                openMenu = { navController.navigate("menu") },
                                openSettings = { navController.navigate("settings") },
                                toggleSubtasks = { id, collapsed ->
                                    taskListViewModel.toggleSubtasks(id, collapsed)
                                }
                            )
                        }
                        composable(
                            route = "task_edit?taskId={taskId}",
                            arguments = listOf(
                                navArgument("taskId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { navBackStackEntry ->
                            val taskId = navBackStackEntry.arguments?.getLong("taskId") ?: 0
                            val context = LocalContext.current
                            val viewModel: TaskEditViewModel = viewModel(
                                viewModelStoreOwner = navBackStackEntry,
                                factory = TaskEditViewModelFactory(
                                    applicationContext = context.applicationContext,
                                    taskId = taskId,
                                )
                            )
                            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                            TaskEditScreen(
                                uiState = uiState,
                                setTitle = { viewModel.setTitle(it) },
                                toggleCompleted = { viewModel.setCompleted(!uiState.completed) { navController.popBackStack() } },
                                save = { viewModel.save { navController.popBackStack() } },
                                back = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = "menu",
                        ) {
                            MenuScreen(
                                items = menuItems,
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
                            SettingsScreen(
                                showHidden = viewState.showHidden,
                                showCompleted = viewState.showCompleted,
                                sortMode = viewState.sortMode,
                                groupMode = viewState.groupMode,
                                toggleShowHidden = { settingsViewModel.setShowHidden(it) },
                                toggleShowCompleted = { settingsViewModel.setShowCompleted(it) },
                                openSortPicker = { navController.navigate("sort_picker") },
                                openGroupPicker = { navController.navigate("group_picker") },
                            )
                        }
                        composable(
                            route = "sort_picker",
                        ) {
                            val viewState =
                                settingsViewModel.viewState.collectAsStateWithLifecycle().value
                            SortPickerScreen(
                                selected = viewState.sortMode,
                                includeNone = false,
                                onSelect = {
                                    settingsViewModel.setSortMode(it)
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(
                            route = "group_picker",
                        ) {
                            val viewState =
                                settingsViewModel.viewState.collectAsStateWithLifecycle().value
                            SortPickerScreen(
                                selected = viewState.groupMode,
                                includeNone = true,
                                onSelect = {
                                    settingsViewModel.setGroupMode(it)
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
