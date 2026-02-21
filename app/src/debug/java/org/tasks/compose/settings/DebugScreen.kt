package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun DebugScreen(
    leakCanaryEnabled: Boolean,
    strictModeVmEnabled: Boolean,
    strictModeThreadEnabled: Boolean,
    crashOnViolationEnabled: Boolean,
    unlockProEnabled: Boolean,
    showDebugFilters: Boolean,
    iapTitle: String,
    onLeakCanary: (Boolean) -> Unit,
    onStrictModeVm: (Boolean) -> Unit,
    onStrictModeThread: (Boolean) -> Unit,
    onCrashOnViolation: (Boolean) -> Unit,
    onUnlockPro: (Boolean) -> Unit,
    onShowDebugFilters: (Boolean) -> Unit,
    onResetSsl: () -> Unit,
    onCrashApp: () -> Unit,
    onRestartApp: () -> Unit,
    onIap: () -> Unit,
    onClearHints: () -> Unit,
    onCreateTasks: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Switches island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_leakcanary),
                    checked = leakCanaryEnabled,
                    onCheckedChange = onLeakCanary,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_strict_mode_vm),
                    checked = strictModeVmEnabled,
                    onCheckedChange = onStrictModeVm,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_strict_mode_thread),
                    checked = strictModeThreadEnabled,
                    onCheckedChange = onStrictModeThread,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_main_queries),
                    checked = crashOnViolationEnabled,
                    onCheckedChange = onCrashOnViolation,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_pro),
                    checked = unlockProEnabled,
                    onCheckedChange = onUnlockPro,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.debug_show_filters),
                    checked = showDebugFilters,
                    onCheckedChange = onShowDebugFilters,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Actions island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.debug_reset_ssl),
                    onClick = onResetSsl,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.debug_crash_app),
                    onClick = onCrashApp,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.debug_force_restart),
                    onClick = onRestartApp,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = iapTitle,
                    onClick = onIap,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.debug_clear_hints),
                    onClick = onClearHints,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.debug_create_tasks),
                    onClick = onCreateTasks,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
