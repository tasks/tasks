package org.tasks.dialogs

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.core.SortHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
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

@Composable
fun SortSheetContent(
    manualSortSelected: Boolean,
    manualSortEnabled: Boolean,
    astridSortEnabled: Boolean,
    selected: Int,
    setManualSort: (Boolean) -> Unit,
    setAstridSort: (Boolean) -> Unit,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        SortPicker(
            selected = if (manualSortSelected) -1 else selected,
            options = sortOptions,
            onClick = { onSelected(it) },
        )
        if (astridSortEnabled) {
            SortOption(
                resId = R.string.astrid_sort_order,
                selected = manualSortSelected,
                onClick = { setAstridSort(true) }
            )
        }
        SortOption(
            resId = R.string.SSD_sort_my_order,
            selected = manualSortSelected && !astridSortEnabled,
            enabled = manualSortEnabled,
            onClick = { setManualSort(true) },
        )
    }
}

val sortOptions = linkedMapOf(
    R.string.SSD_sort_due to SortHelper.SORT_DUE,
    R.string.SSD_sort_start to SortHelper.SORT_START,
    R.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    R.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    R.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    R.string.SSD_sort_auto to SortHelper.SORT_AUTO,
    R.string.sort_created to SortHelper.SORT_CREATED,
)

val subtaskOptions = linkedMapOf(
    R.string.SSD_sort_my_order to SortHelper.SORT_MANUAL,
    R.string.SSD_sort_due to SortHelper.SORT_DUE,
    R.string.SSD_sort_start to SortHelper.SORT_START,
    R.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    R.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    R.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    R.string.SSD_sort_auto to SortHelper.SORT_AUTO,
    R.string.sort_created to SortHelper.SORT_CREATED,
)

val groupOptions = linkedMapOf(
    R.string.none to SortHelper.GROUP_NONE,
    R.string.SSD_sort_due to SortHelper.SORT_DUE,
    R.string.SSD_sort_start to SortHelper.SORT_START,
    R.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    R.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    R.string.sort_created to SortHelper.SORT_CREATED,
    R.string.sort_list to SortHelper.SORT_LIST,
)

private val completedOptions = linkedMapOf(
    R.string.sort_completed to SortHelper.SORT_COMPLETED,
    R.string.SSD_sort_due to SortHelper.SORT_DUE,
    R.string.SSD_sort_start to SortHelper.SORT_START,
    R.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    R.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    R.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    R.string.sort_created to SortHelper.SORT_CREATED,
)

@Composable
fun SortPicker(
    selected: Int,
    options: Map<Int, Int>,
    onClick: (Int) -> Unit,
) {
    options.forEach { (resId, sortMode) ->
        SortOption(
            resId = resId,
            selected = selected == sortMode,
            onClick = { onClick(sortMode) },
        )
    }
}

@Composable
fun SortOption(
    resId: Int,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = resId),
            style = MaterialTheme.typography.titleLarge.copy(
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                }
            ),
        )
        if (!enabled) {
            Text(
                text = stringResource(id = R.string.sort_not_available),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = FontStyle.Italic,
                ),
            )
        }
    }
}

@Composable
fun BottomSheetContent(
    groupMode: Int,
    sortMode: Int,
    completedMode: Int,
    subtaskMode: Int,
    sortAscending: Boolean,
    groupAscending: Boolean,
    completedAscending: Boolean,
    subtaskAscending: Boolean,
    manualSort: Boolean,
    astridSort: Boolean,
    completedAtBottom: Boolean,
    setSortAscending: (Boolean) -> Unit,
    setGroupAscending: (Boolean) -> Unit,
    setCompletedAscending: (Boolean) -> Unit,
    setSubtaskAscending: (Boolean) -> Unit,
    setCompletedAtBottom: (Boolean) -> Unit,
    clickGroupMode: () -> Unit,
    clickSortMode: () -> Unit,
    clickCompletedMode: () -> Unit,
    clickSubtaskMode: () -> Unit,
) {
    SortRow(
        title = R.string.sort_grouping,
        icon = Icons.Outlined.ExpandCircleDown,
        ascending = groupAscending,
        sortMode = groupMode,
        showAscending = groupMode != SortHelper.GROUP_NONE,
        onClick = clickGroupMode,
        setAscending = setGroupAscending
    )
    SortRow(
        title = R.string.sort_sorting,
        body = remember(manualSort, astridSort, sortMode) {
            when {
                manualSort -> R.string.SSD_sort_my_order
                astridSort -> R.string.astrid_sort_order
                else -> sortMode.modeString
            }
        },
        ascending = sortAscending,
        sortMode = sortMode,
        showAscending = !(manualSort || astridSort),
        onClick = clickSortMode,
        setAscending = setSortAscending,
    )
    if (!astridSort) {
        if (!manualSort) {
            SortRow(
                title = R.string.subtasks,
                icon = Icons.Outlined.SubdirectoryArrowRight,
                ascending = subtaskAscending,
                sortMode = subtaskMode,
                onClick = clickSubtaskMode,
                setAscending = setSubtaskAscending,
                showAscending = subtaskMode != SortHelper.SORT_MANUAL
            )
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable { setCompletedAtBottom(!completedAtBottom) },
        ) {
            Text(
                text = stringResource(R.string.completed_tasks_at_bottom),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = completedAtBottom,
                onCheckedChange = { setCompletedAtBottom(it) }
            )
        }
        if (completedAtBottom) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )
            SortRow(
                title = R.string.completed,
                ascending = completedAscending,
                sortMode = completedMode,
                onClick = clickCompletedMode,
                setAscending = setCompletedAscending,
            )
        }
    }
}

@Composable
fun SortRow(
    icon: ImageVector = Icons.Outlined.SwapVert,
    title: Int,
    ascending: Boolean,
    sortMode: Int,
    body: Int = remember(sortMode) { sortMode.modeString },
    showAscending: Boolean = true,
    onClick: () -> Unit,
    setAscending: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .alpha(ContentAlpha.medium),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(id = title), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(id = body), style = MaterialTheme.typography.bodyMedium)
        }
        if (showAscending) {
            Spacer(modifier = Modifier.width(16.dp))
            val displayAscending = when (sortMode) {
                SortHelper.SORT_AUTO,
                SortHelper.SORT_IMPORTANCE -> !ascending

                else -> ascending
            }
            OrderingButton(
                ascending = displayAscending,
                onClick = { setAscending(!ascending) }
            )
        }
    }
}

@Composable
fun OrderingButton(
    ascending: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Divider(modifier = Modifier
            .width(1.dp)
            .fillMaxHeight())
        Icon(
            imageVector = if (ascending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
            contentDescription = null,
        )
        Text(
            text = stringResource(id = if (ascending) R.string.sort_ascending else R.string.sort_descending),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val Int.modeString: Int
    get() = when (this) {
        SortHelper.GROUP_NONE -> R.string.none
        SortHelper.SORT_ALPHA -> R.string.SSD_sort_alpha
        SortHelper.SORT_DUE -> R.string.SSD_sort_due
        SortHelper.SORT_IMPORTANCE -> R.string.SSD_sort_importance
        SortHelper.SORT_MODIFIED -> R.string.SSD_sort_modified
        SortHelper.SORT_CREATED -> R.string.sort_created
        SortHelper.SORT_START -> R.string.SSD_sort_start
        SortHelper.SORT_LIST -> R.string.sort_list
        SortHelper.SORT_COMPLETED -> R.string.sort_completed
        SortHelper.SORT_MANUAL -> R.string.SSD_sort_my_order
        else -> R.string.SSD_sort_auto
    }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewSortBottomSheet() {
    TasksTheme {
        Column {
            BottomSheetContent(
                groupMode = SortHelper.GROUP_NONE,
                sortMode = SortHelper.SORT_AUTO,
                completedMode = SortHelper.SORT_ALPHA,
                subtaskMode = SortHelper.SORT_MANUAL,
                sortAscending = false,
                groupAscending = false,
                completedAscending = false,
                subtaskAscending = false,
                manualSort = false,
                astridSort = false,
                completedAtBottom = true,
                clickGroupMode = {},
                clickSortMode = {},
                clickCompletedMode = {},
                clickSubtaskMode = {},
                setCompletedAtBottom = {},
                setSortAscending = {},
                setGroupAscending = {},
                setCompletedAscending = {},
                setSubtaskAscending = {},
            )
        }
    }
}
