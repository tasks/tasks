package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.todoroo.astrid.core.SortHelper
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.group_none
import tasks.kmp.generated.resources.sort_alphabetical
import tasks.kmp.generated.resources.sort_created
import tasks.kmp.generated.resources.sort_due_date
import tasks.kmp.generated.resources.sort_modified
import tasks.kmp.generated.resources.sort_priority
import tasks.kmp.generated.resources.sort_start_date

@Composable
fun sortModeLabel(sortMode: Int): String = when (sortMode) {
    SortHelper.SORT_DUE -> stringResource(Res.string.sort_due_date)
    SortHelper.SORT_IMPORTANCE -> stringResource(Res.string.sort_priority)
    SortHelper.SORT_ALPHA -> stringResource(Res.string.sort_alphabetical)
    SortHelper.SORT_CREATED -> stringResource(Res.string.sort_created)
    SortHelper.SORT_MODIFIED -> stringResource(Res.string.sort_modified)
    SortHelper.SORT_START -> stringResource(Res.string.sort_start_date)
    else -> stringResource(Res.string.sort_due_date)
}

@Composable
fun groupModeLabel(groupMode: Int): String = when (groupMode) {
    SortHelper.GROUP_NONE -> stringResource(Res.string.group_none)
    else -> sortModeLabel(groupMode)
}

data class SortOption(
    val value: Int,
    val label: @Composable () -> String,
)

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SortPickerScreen(
    selected: Int,
    includeNone: Boolean,
    onSelect: (Int) -> Unit,
) {
    val options = buildList {
        if (includeNone) {
            add(SortOption(SortHelper.GROUP_NONE) { stringResource(Res.string.group_none) })
        }
        add(SortOption(SortHelper.SORT_DUE) { stringResource(Res.string.sort_due_date) })
        add(SortOption(SortHelper.SORT_IMPORTANCE) { stringResource(Res.string.sort_priority) })
        add(SortOption(SortHelper.SORT_ALPHA) { stringResource(Res.string.sort_alphabetical) })
        add(SortOption(SortHelper.SORT_CREATED) { stringResource(Res.string.sort_created) })
        add(SortOption(SortHelper.SORT_MODIFIED) { stringResource(Res.string.sort_modified) })
        add(SortOption(SortHelper.SORT_START) { stringResource(Res.string.sort_start_date) })
    }

    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            items(options.size) { index ->
                val option = options[index]
                val isSelected = option.value == selected
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(option.value) },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option.label(),
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    colors = if (isSelected) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                )
            }
        }
    }
}
