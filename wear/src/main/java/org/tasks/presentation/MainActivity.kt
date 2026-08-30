/**
 * MainActivity.kt — Single Activity host for the Wear OS Tasks app.
 *
 * ## Architecture
 * Uses Jetpack Compose for Wear OS with a [SwipeDismissableNavHost] that
 * provides swipe-to-dismiss navigation between three screens:
 *
 * | Route            | Screen                      | ViewModel                       |
 * |------------------|-----------------------------|----------------------------------|
 * | `task_list`      | [OfflineTaskListScreen]      | [OfflineTaskListViewModel]       |
 * | `task_edit`      | [OfflineTaskEditScreen]      | [OfflineTaskEditViewModel]       |
 * | `settings`       | [OfflineSettingsScreen]      | [OfflineSettingsViewModel]       |
 *
 * ## Offline-first design
 * All data is stored in a local Room database ([WearDatabase]).
 * When the paired phone is reachable, the app syncs via the
 * Wearable Data Layer API (see [DataLayerSyncManager]).
 *
 * ## Permissions
 * On Android 13+ (Tiramisu) the app requests `POST_NOTIFICATIONS` at launch
 * so that task reminders can be shown.
 */

package org.tasks.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.AppScaffold
import org.tasks.presentation.screens.OfflineSettingsScreen
import org.tasks.presentation.screens.OfflineSettingsViewModel
import org.tasks.presentation.screens.OfflineTaskEditScreen
import org.tasks.presentation.screens.OfflineTaskEditViewModel
import org.tasks.presentation.screens.OfflineTaskEditViewModelFactory
import org.tasks.presentation.screens.OfflineTaskListScreen
import org.tasks.presentation.screens.OfflineTaskListViewModel
import org.tasks.presentation.theme.TasksTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("POST_NOTIFICATIONS permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        setContent {
            TasksTheme {
                AppScaffold(
                    timeText = { TimeText() },
                    modifier = Modifier.background(MaterialTheme.colors.background),
                ) {
                    val navController = rememberSwipeDismissableNavController()

                    // Offline-first: use local database and sync when connected
                    val taskListViewModel: OfflineTaskListViewModel = viewModel()
                    val settingsViewModel: OfflineSettingsViewModel = viewModel()

                    SwipeDismissableNavHost(
                        startDestination = "task_list",
                        navController = navController,
                    ) {
                        // Main task list screen
                        composable("task_list") {
                            val viewState = taskListViewModel.viewState.collectAsStateWithLifecycle().value
                            OfflineTaskListScreen(
                                viewState = viewState,
                                onTaskClick = { stringId ->
                                    navController.navigate("task_edit?taskId=$stringId")
                                },
                                onToggleComplete = { stringId, completed ->
                                    taskListViewModel.toggleComplete(stringId, completed)
                                },
                                onAddTask = {
                                    navController.navigate("task_edit?taskId=")
                                },
                                openSettings = { navController.navigate("settings") },
                                onRefresh = { taskListViewModel.refresh() },
                            )
                        }

                        // Task edit screen
                        composable(
                            route = "task_edit?taskId={taskId}",
                            arguments = listOf(
                                navArgument("taskId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { navBackStackEntry ->
                            val taskId = navBackStackEntry.arguments?.getString("taskId") ?: ""
                            val context = LocalContext.current
                            val editViewModel: OfflineTaskEditViewModel = viewModel(
                                viewModelStoreOwner = navBackStackEntry,
                                factory = OfflineTaskEditViewModelFactory(
                                    application = context.applicationContext as android.app.Application,
                                    taskId = taskId.ifEmpty { null },
                                )
                            )
                            val uiState = editViewModel.uiState.collectAsStateWithLifecycle().value
                            OfflineTaskEditScreen(
                                uiState = uiState,
                                onTitleChange = { editViewModel.setTitle(it) },
                                onNotesChange = { editViewModel.setNotes(it) },
                                onToggleCompleted = { editViewModel.toggleCompleted() },
                                onSave = { editViewModel.save { navController.popBackStack() } },
                                onDelete = { editViewModel.delete { navController.popBackStack() } },
                                onBack = { navController.popBackStack() },
                                onSetDueDate = { editViewModel.setDueDate(it) },
                                onSetDueTime = { editViewModel.setDueTime(it) },
                                onToggleReminder = { editViewModel.toggleReminder() },
                                onShowDatePicker = { editViewModel.showDatePicker(true) },
                                onShowTimePicker = { editViewModel.showTimePicker(true) },
                                onClearDueDate = { editViewModel.clearDueDate() },
                                onDismissDatePicker = { editViewModel.showDatePicker(false) },
                                onDismissTimePicker = { editViewModel.showTimePicker(false) },
                            )
                        }

                        // Settings screen
                        composable("settings") {
                            val viewState = settingsViewModel.viewState.collectAsStateWithLifecycle().value
                            OfflineSettingsScreen(
                                viewState = viewState,
                                toggleShowHidden = { settingsViewModel.setShowHidden(it) },
                                toggleShowCompleted = { settingsViewModel.setShowCompleted(it) },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
