package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.preferences.TasksPreferences
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.debug
import tasks.kmp.generated.resources.debug_unlock_pro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsDetail(
    onNavigateBack: () -> Unit,
) {
    val tasksPreferences = koinInject<TasksPreferences>()
    val scope = rememberCoroutineScope()
    val unlockPro by tasksPreferences
        .flow(TasksPreferences.debugPro, false)
        .collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.debug)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                SettingsItemCard(position = CardPosition.Only) {
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.debug_unlock_pro),
                        checked = unlockPro,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                tasksPreferences.set(TasksPreferences.debugPro, enabled)
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }
    }
}
