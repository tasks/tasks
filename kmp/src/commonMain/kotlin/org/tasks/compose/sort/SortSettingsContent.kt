package org.tasks.compose.sort

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.todoroo.astrid.core.SortHelper
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.SSD_sort_alpha
import tasks.kmp.generated.resources.SSD_sort_auto
import tasks.kmp.generated.resources.SSD_sort_due
import tasks.kmp.generated.resources.SSD_sort_importance
import tasks.kmp.generated.resources.SSD_sort_modified
import tasks.kmp.generated.resources.SSD_sort_my_order
import tasks.kmp.generated.resources.SSD_sort_start
import tasks.kmp.generated.resources.astrid_sort_order
import tasks.kmp.generated.resources.completed
import tasks.kmp.generated.resources.completed_tasks_at_bottom
import tasks.kmp.generated.resources.none
import tasks.kmp.generated.resources.sort_ascending
import tasks.kmp.generated.resources.sort_completed
import tasks.kmp.generated.resources.sort_created
import tasks.kmp.generated.resources.sort_descending
import tasks.kmp.generated.resources.sort_grouping
import tasks.kmp.generated.resources.sort_list
import tasks.kmp.generated.resources.sort_not_available
import tasks.kmp.generated.resources.sort_sorting
import tasks.kmp.generated.resources.subtasks

val sortOptions = linkedMapOf(
    Res.string.SSD_sort_due to SortHelper.SORT_DUE,
    Res.string.SSD_sort_start to SortHelper.SORT_START,
    Res.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    Res.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    Res.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    Res.string.SSD_sort_auto to SortHelper.SORT_AUTO,
    Res.string.sort_created to SortHelper.SORT_CREATED,
)

val subtaskOptions = linkedMapOf(
    Res.string.SSD_sort_my_order to SortHelper.SORT_MANUAL,
    Res.string.SSD_sort_due to SortHelper.SORT_DUE,
    Res.string.SSD_sort_start to SortHelper.SORT_START,
    Res.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    Res.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    Res.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    Res.string.SSD_sort_auto to SortHelper.SORT_AUTO,
    Res.string.sort_created to SortHelper.SORT_CREATED,
)

val groupOptions = linkedMapOf(
    Res.string.none to SortHelper.GROUP_NONE,
    Res.string.SSD_sort_due to SortHelper.SORT_DUE,
    Res.string.SSD_sort_start to SortHelper.SORT_START,
    Res.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    Res.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    Res.string.sort_created to SortHelper.SORT_CREATED,
    Res.string.sort_list to SortHelper.SORT_LIST,
)

val completedOptions = linkedMapOf(
    Res.string.sort_completed to SortHelper.SORT_COMPLETED,
    Res.string.SSD_sort_due to SortHelper.SORT_DUE,
    Res.string.SSD_sort_start to SortHelper.SORT_START,
    Res.string.SSD_sort_importance to SortHelper.SORT_IMPORTANCE,
    Res.string.SSD_sort_alpha to SortHelper.SORT_ALPHA,
    Res.string.SSD_sort_modified to SortHelper.SORT_MODIFIED,
    Res.string.sort_created to SortHelper.SORT_CREATED,
)

val Int.modeString: StringResource
    get() = when (this) {
        SortHelper.GROUP_NONE -> Res.string.none
        SortHelper.SORT_ALPHA -> Res.string.SSD_sort_alpha
        SortHelper.SORT_DUE -> Res.string.SSD_sort_due
        SortHelper.SORT_IMPORTANCE -> Res.string.SSD_sort_importance
        SortHelper.SORT_MODIFIED -> Res.string.SSD_sort_modified
        SortHelper.SORT_CREATED -> Res.string.sort_created
        SortHelper.SORT_START -> Res.string.SSD_sort_start
        SortHelper.SORT_LIST -> Res.string.sort_list
        SortHelper.SORT_COMPLETED -> Res.string.sort_completed
        SortHelper.SORT_MANUAL -> Res.string.SSD_sort_my_order
        else -> Res.string.SSD_sort_auto
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
                label = Res.string.astrid_sort_order,
                selected = manualSortSelected,
                onClick = { setAstridSort(true) }
            )
        }
        SortOption(
            label = Res.string.SSD_sort_my_order,
            selected = manualSortSelected && !astridSortEnabled,
            enabled = manualSortEnabled,
            onClick = { setManualSort(true) },
        )
    }
}

@Composable
fun SortPicker(
    selected: Int,
    options: Map<StringResource, Int>,
    onClick: (Int) -> Unit,
) {
    options.forEach { (label, sortMode) ->
        SortOption(
            label = label,
            selected = selected == sortMode,
            onClick = { onClick(sortMode) },
        )
    }
}

@Composable
fun SortOption(
    label: StringResource,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleLarge.copy(
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            ),
        )
        if (!enabled) {
            Text(
                text = stringResource(Res.string.sort_not_available),
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
        title = Res.string.sort_grouping,
        icon = Icons.Outlined.ExpandCircleDown,
        ascending = groupAscending,
        sortMode = groupMode,
        showAscending = groupMode != SortHelper.GROUP_NONE,
        onClick = clickGroupMode,
        setAscending = setGroupAscending
    )
    SortRow(
        title = Res.string.sort_sorting,
        body = remember(manualSort, astridSort, sortMode) {
            when {
                manualSort -> Res.string.SSD_sort_my_order
                astridSort -> Res.string.astrid_sort_order
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
                title = Res.string.subtasks,
                icon = Icons.Outlined.SubdirectoryArrowRight,
                ascending = subtaskAscending,
                sortMode = subtaskMode,
                onClick = clickSubtaskMode,
                setAscending = setSubtaskAscending,
                showAscending = subtaskMode != SortHelper.SORT_MANUAL
            )
        }
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable { setCompletedAtBottom(!completedAtBottom) },
        ) {
            Text(
                text = stringResource(Res.string.completed_tasks_at_bottom),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = completedAtBottom,
                onCheckedChange = { setCompletedAtBottom(it) }
            )
        }
        if (completedAtBottom) {
            HorizontalDivider()
            SortRow(
                title = Res.string.completed,
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
    title: StringResource,
    ascending: Boolean,
    sortMode: Int,
    body: StringResource = remember(sortMode) { sortMode.modeString },
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
                .alpha(0.6f),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(body), style = MaterialTheme.typography.bodyMedium)
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
        VerticalDivider()
        Icon(
            imageVector = if (ascending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
            contentDescription = null,
        )
        Text(
            text = stringResource(if (ascending) Res.string.sort_ascending else Res.string.sort_descending),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
