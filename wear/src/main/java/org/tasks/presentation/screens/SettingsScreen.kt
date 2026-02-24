package org.tasks.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.group_mode
import tasks.kmp.generated.resources.show_completed
import tasks.kmp.generated.resources.show_unstarted
import tasks.kmp.generated.resources.sort_mode

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SettingsScreen(
    showHidden: Boolean,
    showCompleted: Boolean,
    sortMode: Int,
    groupMode: Int,
    toggleShowHidden: (Boolean) -> Unit,
    toggleShowCompleted: (Boolean) -> Unit,
    openSortPicker: () -> Unit,
    openGroupPicker: () -> Unit,
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
                    onClick = openGroupPicker,
                    label = { Text(stringResource(Res.string.group_mode)) },
                    secondaryLabel = { Text(groupModeLabel(groupMode)) },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = openSortPicker,
                    label = { Text(stringResource(Res.string.sort_mode)) },
                    secondaryLabel = { Text(sortModeLabel(sortMode)) },
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
            item {
                ToggleChip(
                    checked = showCompleted,
                    onCheckedChanged = { toggleShowCompleted(it) },
                    label = stringResource(Res.string.show_completed),
                    toggleControl = ToggleChipToggleControl.Switch,
                )
            }
        }
    }
}
