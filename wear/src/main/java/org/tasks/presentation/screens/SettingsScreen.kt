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

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SettingsScreen(

    showHidden: Boolean,
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
                ToggleChip(
                    checked = showHidden,
                    onCheckedChanged = { toggleShowHidden(it) },
                    label = "Show unstarted",
                    toggleControl = ToggleChipToggleControl.Switch,
                )
            }
        }
    }
}