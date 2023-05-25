package org.tasks.dialogs

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.core.SortHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.SystemBars
import org.tasks.compose.collectAsStateLifecycleAware

@AndroidEntryPoint
class SortSettingsActivity : ComponentActivity() {

    private val viewModel: SortSettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        setContent {
            MdcTheme {
                val scrimColor = if (isSystemInDarkTheme())
                    Color(0x52454545)
                else
                    MaterialTheme.colors.onSurface.copy(.5f)
                // edge-to-edge potentially fixed in material3 v1.2.0
                SystemBars(
                    statusBarColor = scrimColor,
                    navigationBarColor = MaterialTheme.colors.surface,
                )
                val state = viewModel.state.collectAsStateLifecycleAware().value
                val mainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                var showGroupPicker by remember { mutableStateOf(false) }
                var showSortPicker by remember { mutableStateOf(false) }
                ModalBottomSheet(
                    onDismissRequest = {
                        val forceReload = viewModel.forceReload
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_FORCE_RELOAD, forceReload)
                        )
                        finish()
                        overridePendingTransition(0, 0)
                    },
                    sheetState = mainSheetState,
                    containerColor = MaterialTheme.colors.surface,
                    scrimColor = scrimColor,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    content = {
                        BottomSheetContent(
                            groupMode = state.groupMode,
                            sortMode = state.sortMode,
                            sortAscending = state.sortAscending,
                            groupAscending = state.groupAscending,
                            manualSort = state.manualSort && manualEnabled,
                            astridSort = state.astridSort && astridEnabled,
                            setSortAscending = { viewModel.setSortAscending(it) },
                            setGroupAscending = { viewModel.setGroupAscending(it) },
                            clickGroupMode = { showGroupPicker = true },
                            clickSortMode = { showSortPicker = true },
                        )
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
                        containerColor = MaterialTheme.colors.surface,
                        scrimColor = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        content = {
                            GroupSheetContent(
                                selected = state.groupMode,
                                onSelected = {
                                    viewModel.setGroupMode(it)
                                    closePicker()
                                }
                            )
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
                        containerColor = MaterialTheme.colors.surface,
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
        const val EXTRA_FORCE_RELOAD = "extra_force_reload"
        const val WIDGET_NONE = -1

        fun getIntent(
            context: Context,
            manualOrderEnabled: Boolean,
            astridOrderEnabled: Boolean,
            widgetId: Int? = null,
        ) = Intent(context, SortSettingsActivity::class.java)
            .addFlags(FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(EXTRA_MANUAL_ORDER, manualOrderEnabled)
            .putExtra(EXTRA_ASTRID_ORDER, astridOrderEnabled)
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
    if (manualSortEnabled) {
        SortOption(resId = R.string.SSD_sort_my_order, selected = manualSortSelected) {
            setManualSort(true)
        }
    }
    if (astridSortEnabled) {
        SortOption(resId = R.string.astrid_sort_order, selected = manualSortSelected) {
            setAstridSort(true)
        }
    }
    SortOption(
        resId = R.string.SSD_sort_auto,
        selected = !manualSortSelected && selected == SortHelper.SORT_AUTO,
        onClick = { onSelected(SortHelper.SORT_AUTO) }
    )
    SortOption(
        resId = R.string.SSD_sort_start,
        selected = !manualSortSelected && selected == SortHelper.SORT_START,
        onClick = { onSelected(SortHelper.SORT_START) }
    )
    SortOption(
        resId = R.string.SSD_sort_due,
        selected = !manualSortSelected && selected == SortHelper.SORT_DUE,
        onClick = { onSelected(SortHelper.SORT_DUE) }
    )
    SortOption(
        resId = R.string.SSD_sort_importance,
        selected = !manualSortSelected && selected == SortHelper.SORT_IMPORTANCE,
        onClick = { onSelected(SortHelper.SORT_IMPORTANCE) }
    )
    SortOption(
        resId = R.string.SSD_sort_alpha,
        selected = !manualSortSelected && selected == SortHelper.SORT_ALPHA,
        onClick = { onSelected(SortHelper.SORT_ALPHA) }
    )
    SortOption(
        resId = R.string.SSD_sort_modified,
        selected = !manualSortSelected && selected == SortHelper.SORT_MODIFIED,
        onClick = { onSelected(SortHelper.SORT_MODIFIED) }
    )
    SortOption(
        resId = R.string.sort_created,
        selected = !manualSortSelected && selected == SortHelper.SORT_CREATED,
        onClick = { onSelected(SortHelper.SORT_CREATED) }
    )
}

@Composable
fun GroupSheetContent(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    SortOption(
        resId = R.string.none,
        selected = selected == SortHelper.GROUP_NONE,
        onClick = { onSelected(SortHelper.GROUP_NONE) }
    )
    SortOption(
        resId = R.string.SSD_sort_due,
        selected = selected == SortHelper.SORT_DUE,
        onClick = { onSelected(SortHelper.SORT_DUE) }
    )
    SortOption(
        resId = R.string.SSD_sort_start,
        selected = selected == SortHelper.SORT_START,
        onClick = { onSelected(SortHelper.SORT_START) }
    )
    SortOption(
        resId = R.string.SSD_sort_importance,
        selected = selected == SortHelper.SORT_IMPORTANCE,
        onClick = { onSelected(SortHelper.SORT_IMPORTANCE) }
    )
    SortOption(
        resId = R.string.SSD_sort_modified,
        selected = selected == SortHelper.SORT_MODIFIED,
        onClick = { onSelected(SortHelper.SORT_MODIFIED) }
    )
    SortOption(
        resId = R.string.sort_created,
        selected = selected == SortHelper.SORT_CREATED,
        onClick = { onSelected(SortHelper.SORT_CREATED) }
    )
    SortOption(
        resId = R.string.sort_list,
        selected = selected == SortHelper.SORT_LIST,
        onClick = { onSelected(SortHelper.SORT_LIST) }
    )
}

@Composable
fun SortOption(
    resId: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = stringResource(id = resId),
        style = MaterialTheme.typography.h6.copy(
            color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
        ),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetContent(
    groupMode: Int,
    sortMode: Int,
    sortAscending: Boolean,
    groupAscending: Boolean,
    manualSort: Boolean,
    astridSort: Boolean,
    setSortAscending: (Boolean) -> Unit,
    setGroupAscending: (Boolean) -> Unit,
    clickGroupMode: () -> Unit,
    clickSortMode: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { clickGroupMode() }
        ) {
            Text(
                text = stringResource(id = R.string.sort_grouping),
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = stringResource(id = groupMode.modeString),
                style = MaterialTheme.typography.body1,
            )
        }
        if (groupMode != SortHelper.GROUP_NONE) {
            Spacer(modifier = Modifier.width(16.dp))
            val displayAscending = when (groupMode) {
                SortHelper.SORT_IMPORTANCE -> !groupAscending
                else -> groupAscending
            }
            Chip(
                onClick = { setGroupAscending(!groupAscending) },
                shape = RoundedCornerShape(4.dp),
                border = ChipDefaults.outlinedBorder,
                colors = ChipDefaults.outlinedChipColors(),
                leadingIcon = {
                    Icon(
                        imageVector = if (displayAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        modifier = Modifier.size(16.dp),
                        contentDescription = null,
                    )
                },
                content = {
                    Text(
                        text = stringResource(id = if (displayAscending) R.string.sort_ascending else R.string.sort_descending),
                        style = MaterialTheme.typography.body1,
                    )
                },
            )
        }
    }
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { clickSortMode() }
        ) {
            Text(
                text = stringResource(id = R.string.sort_sorting),
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = stringResource(
                    id = when {
                        manualSort -> R.string.SSD_sort_my_order
                        astridSort -> R.string.astrid_sort_order
                        else -> sortMode.modeString
                    }
                ),
                style = MaterialTheme.typography.body1,
            )
        }
        if (!(manualSort || astridSort)) {
            Spacer(modifier = Modifier.width(16.dp))
            val displayAscending = when (sortMode) {
                SortHelper.SORT_AUTO,
                SortHelper.SORT_IMPORTANCE -> !sortAscending
                else -> sortAscending
            }
            Chip(
                onClick = { setSortAscending(!sortAscending) },
                shape = RoundedCornerShape(4.dp),
                border = ChipDefaults.outlinedBorder,
                colors = ChipDefaults.outlinedChipColors(),
                leadingIcon = {
                    Icon(
                        imageVector = if (displayAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        modifier = Modifier.size(16.dp),
                        contentDescription = null,
                    )
                },
                content = {
                    Text(
                        text = stringResource(id = if (displayAscending) R.string.sort_ascending else R.string.sort_descending),
                        style = MaterialTheme.typography.body1,
                    )
                },
            )
        }
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
        else -> R.string.SSD_sort_auto
    }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewSortBottomSheet() {
    MdcTheme {
        BottomSheetContent(
            groupMode = SortHelper.GROUP_NONE,
            sortMode = SortHelper.SORT_AUTO,
            sortAscending = false,
            groupAscending = false,
            manualSort = false,
            astridSort = false,
            clickGroupMode = {},
            clickSortMode = {},
            setSortAscending = {},
            setGroupAscending = {},
        )
    }
}
