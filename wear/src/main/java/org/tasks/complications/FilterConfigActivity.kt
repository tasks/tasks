package org.tasks.complications

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import org.jetbrains.compose.resources.stringResource
import org.tasks.presentation.screens.MenuScreen
import org.tasks.presentation.screens.MenuViewModel
import org.tasks.presentation.theme.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.filter
import tasks.kmp.generated.resources.filter_my_tasks
import tasks.kmp.generated.resources.show_unstarted

class FilterConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val complicationId = intent.getIntExtra(
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID",
            -1
        )
        if (complicationId == -1) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResult(Activity.RESULT_OK)

        setContent {
            TasksTheme {
                AppScaffold {
                    val navController = rememberSwipeDismissableNavController()
                    var filterTitle by rememberSaveable {
                        mutableStateOf(
                            getComplicationFilterTitle(complicationId)
                        )
                    }
                    var showHidden by rememberSaveable {
                        mutableStateOf(
                            getComplicationShowHidden(complicationId)
                        )
                    }
                    SwipeDismissableNavHost(
                        startDestination = "settings",
                        navController = navController,
                    ) {
                        composable("settings") {
                            FilterSettingsScreen(
                                filterTitle = filterTitle,
                                showHidden = showHidden,
                                openFilterPicker = { navController.navigate("filter_picker") },
                                toggleShowHidden = {
                                    setComplicationShowHidden(complicationId, it)
                                    showHidden = it
                                    requestUpdate()
                                },
                            )
                        }
                        composable("filter_picker") {
                            val menuViewModel: MenuViewModel = viewModel()
                            val menuItems = menuViewModel.uiItems.collectAsLazyPagingItems()
                            MenuScreen(
                                items = menuItems,
                                selectFilter = { item ->
                                    setComplicationFilter(complicationId, item.id, item.title)
                                    filterTitle = item.title
                                    requestUpdate()
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestUpdate() {
        listOf(
            TaskCountComplicationService::class.java,
            TaskProgressComplicationService::class.java,
        ).forEach {
            ComplicationDataSourceUpdateRequester.create(this, ComponentName(this, it))
                .requestUpdateAll()
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
private fun FilterSettingsScreen(
    filterTitle: String?,
    showHidden: Boolean,
    openFilterPicker: () -> Unit,
    toggleShowHidden: (Boolean) -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = openFilterPicker,
                    label = { Text(stringResource(Res.string.filter)) },
                    secondaryLabel = {
                        Text(filterTitle ?: stringResource(Res.string.filter_my_tasks))
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                ToggleChip(
                    checked = showHidden,
                    onCheckedChanged = { toggleShowHidden(it) },
                    label = stringResource(Res.string.show_unstarted),
                    toggleControl = ToggleChipToggleControl.Switch,
                )
            }
        }
    }
}
