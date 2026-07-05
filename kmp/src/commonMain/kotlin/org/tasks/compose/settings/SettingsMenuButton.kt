package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.app_settings
import tasks.kmp.generated.resources.list_settings
import tasks.kmp.generated.resources.settings
import tasks.kmp.generated.resources.tag_settings

@Composable
fun SettingsMenuButton(
    showListSettings: Boolean,
    showTagSettings: Boolean,
    onSettingsClick: () -> Unit,
    onListSettingsClick: () -> Unit,
    onTagSettingsClick: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.settings),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.app_settings)) },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
            )
            if (showListSettings) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.list_settings)) },
                    onClick = {
                        expanded = false
                        onListSettingsClick()
                    },
                )
            }
            if (showTagSettings) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.tag_settings)) },
                    onClick = {
                        expanded = false
                        onTagSettingsClick()
                    },
                )
            }
        }
    }
}
