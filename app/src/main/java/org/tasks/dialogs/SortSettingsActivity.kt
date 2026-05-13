package org.tasks.dialogs

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.compose.sort.BottomSheetContent
import org.tasks.compose.sort.SortPicker
import org.tasks.compose.sort.SortSheetContent
import org.tasks.compose.sort.completedOptions
import org.tasks.compose.sort.groupOptions
import org.tasks.compose.sort.subtaskOptions
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class SortSettingsActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: SortSettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val scrimColor = if (isSystemInDarkTheme())
                    Color(0x52454545)
                else
                    MaterialTheme.colorScheme.onSurface.copy(.5f)
                val state = viewModel.state.collectAsStateWithLifecycle().value
                val mainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                var showGroupPicker by remember { mutableStateOf(false) }
                var showSortPicker by remember { mutableStateOf(false) }
                var showCompletedPicker by remember { mutableStateOf(false) }
                var showSubtaskPicker by remember { mutableStateOf(false) }
                ModalBottomSheet(
                    modifier = Modifier.statusBarsPadding(),
                    onDismissRequest = {
                        val forceReload = viewModel.forceReload
                        val changedGroup = viewModel.changedGroup
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_FORCE_RELOAD, forceReload)
                                .putExtra(EXTRA_CHANGED_GROUP, changedGroup)
                        )
                        finish()
                        overridePendingTransition(0, 0)
                    },
                    sheetState = mainSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = scrimColor,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    content = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            BottomSheetContent(
                                groupMode = state.groupMode,
                                sortMode = state.sortMode,
                                completedMode = state.completedMode,
                                subtaskMode = state.subtaskMode,
                                sortAscending = state.sortAscending,
                                groupAscending = state.groupAscending,
                                completedAscending = state.completedAscending,
                                subtaskAscending = state.subtaskAscending,
                                manualSort = state.manualSort && manualEnabled,
                                astridSort = state.astridSort && astridEnabled,
                                completedAtBottom = state.completedAtBottom,
                                setSortAscending = { viewModel.setSortAscending(it) },
                                setGroupAscending = { viewModel.setGroupAscending(it) },
                                setCompletedAscending = { viewModel.setCompletedAscending(it) },
                                setSubtaskAscending = { viewModel.setSubtaskAscending(it) },
                                setCompletedAtBottom = { viewModel.setCompletedAtBottom(it) },
                                clickGroupMode = { showGroupPicker = true },
                                clickSortMode = { showSortPicker = true },
                                clickCompletedMode = { showCompletedPicker = true },
                                clickSubtaskMode = { showSubtaskPicker = true },
                            )
                        }
                    }
                )
                if (showGroupPicker) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val closePicker: () -> Unit = {
                        scope.launch {
                            sheetState.hide()
                            showGroupPicker = false
                        }
                    }
                    ModalBottomSheet(
                        onDismissRequest = closePicker,
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        content = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .fillMaxWidth()
                            ) {
                                SortPicker(
                                    selected = state.groupMode,
                                    options = groupOptions,
                                    onClick = {
                                        viewModel.setGroupMode(it)
                                        closePicker()
                                    }
                                )
                            }
                        }
                    )
                    LaunchedEffect(Unit) {
                        sheetState.show()
                    }
                }
                if (showSortPicker) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val closePicker: () -> Unit = {
                        scope.launch {
                            sheetState.hide()
                            showSortPicker = false
                        }
                    }
                    ModalBottomSheet(
                        onDismissRequest = closePicker,
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        content = {
                            SortSheetContent(
                                manualSortEnabled = manualEnabled,
                                astridSortEnabled = astridEnabled,
                                setManualSort = {
                                    viewModel.setManual(true)
                                    closePicker()
                                },
                                setAstridSort = {
                                    viewModel.setAstrid(true)
                                    closePicker()
                                },
                                manualSortSelected = (manualEnabled && state.manualSort) || (astridEnabled && state.astridSort),
                                selected = state.sortMode,
                                onSelected = {
                                    viewModel.setSortMode(it)
                                    closePicker()
                                }
                            )
                        }
                    )
                    LaunchedEffect(Unit) {
                        sheetState.show()
                    }
                }
                if (showCompletedPicker) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val closePicker: () -> Unit = {
                        scope.launch {
                            sheetState.hide()
                            showCompletedPicker = false
                        }
                    }
                    ModalBottomSheet(
                        onDismissRequest = closePicker,
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        content = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .fillMaxWidth()
                            ) {
                                SortPicker(
                                    selected = state.completedMode,
                                    options = completedOptions,
                                    onClick = {
                                        viewModel.setCompletedMode(it)
                                        closePicker()
                                    }
                                )
                            }
                        }
                    )
                    LaunchedEffect(Unit) {
                        sheetState.show()
                    }
                }
                if (showSubtaskPicker) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val closePicker: () -> Unit = {
                        scope.launch {
                            sheetState.hide()
                            showSubtaskPicker = false
                        }
                    }
                    ModalBottomSheet(
                        onDismissRequest = closePicker,
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        content = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .fillMaxWidth()
                            ) {
                                SortPicker(
                                    selected = state.subtaskMode,
                                    options = subtaskOptions,
                                    onClick = {
                                        viewModel.setSubtaskMode(it)
                                        closePicker()
                                    }
                                )
                            }
                        }
                    )
                    LaunchedEffect(Unit) {
                        sheetState.show()
                    }
                }
                LaunchedEffect(Unit) {
                    mainSheetState.show()
                }
            }
        }
    }

    private val manualEnabled: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_MANUAL_ORDER, false)
    }

    private val astridEnabled: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_ASTRID_ORDER, false)
    }

    companion object {
        const val EXTRA_MANUAL_ORDER = "extra_manual_order"
        const val EXTRA_ASTRID_ORDER = "extra_astrid_order"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        const val EXTRA_FILTER_KEY = "extra_filter_key"
        const val EXTRA_FORCE_RELOAD = "extra_force_reload"
        const val EXTRA_CHANGED_GROUP = "extra_changed_group"
        const val WIDGET_NONE = -1

        fun getIntent(
            context: Context,
            manualOrderEnabled: Boolean,
            astridOrderEnabled: Boolean,
            widgetId: Int? = null,
            filterKey: String? = null,
        ) = Intent(context, SortSettingsActivity::class.java)
            .addFlags(FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(EXTRA_MANUAL_ORDER, manualOrderEnabled)
            .putExtra(EXTRA_ASTRID_ORDER, astridOrderEnabled)
            .putExtra(EXTRA_FILTER_KEY, filterKey)
            .apply {
                widgetId?.let {
                    putExtra(EXTRA_WIDGET_ID, it)
                }
            }
    }
}
