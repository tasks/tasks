package org.tasks.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.show_completed
import tasks.kmp.generated.resources.show_unstarted

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SettingsScreen(
    showHidden: Boolean,
    showCompleted: Boolean,
    toggleShowHidden: (Boolean) -> Unit,
    toggleShowCompleted: (Boolean) -> Unit,
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