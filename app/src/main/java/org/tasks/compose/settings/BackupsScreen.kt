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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun BackupsScreen(
    backupDirSummary: String,
    showBackupDirWarning: Boolean,
    lastBackupSummary: String,
    showLocalBackupWarning: Boolean,
    backupsEnabled: Boolean,
    driveBackupEnabled: Boolean,
    driveAccountSummary: String,
    driveAccountEnabled: Boolean,
    lastDriveBackupSummary: String,
    showDriveBackupWarning: Boolean,
    androidBackupEnabled: Boolean,
    lastAndroidBackupSummary: String,
    showAndroidBackupWarning: Boolean,
    ignoreWarnings: Boolean,
    onDocumentation: () -> Unit,
    onBackupDir: () -> Unit,
    onBackupNow: () -> Unit,
    onImportBackup: () -> Unit,
    onBackupsEnabled: (Boolean) -> Unit,
    onDriveBackup: (Boolean) -> Unit,
    onDriveAccount: () -> Unit,
    onAndroidBackup: (Boolean) -> Unit,
    onDeviceSettings: () -> Unit,
    onIgnoreWarnings: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Documentation
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            PreferenceRow(
                title = stringResource(R.string.documentation),
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = onDocumentation,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Backup directory, backup now, import
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.backup_directory),
                    summary = backupDirSummary,
                    summaryMaxLines = Int.MAX_VALUE,
                    showError = showBackupDirWarning,
                    onClick = onBackupDir,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.backup_BAc_export),
                    summary = lastBackupSummary,
                    showError = showLocalBackupWarning,
                    onClick = onBackupNow,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.backup_BAc_import),
                    onClick = onImportBackup,
                )
            }
        }

        // Automatic backups section
        SectionHeader(
            R.string.automatic_backups,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.enabled),
                checked = backupsEnabled,
                onCheckedChange = onBackupsEnabled,
            )
        }

        // Google Drive backup section
        SectionHeader(
            R.string.google_drive_backup,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                val errorColor = colorResource(R.color.overdue)
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    summary = lastDriveBackupSummary,
                    checked = driveBackupEnabled,
                    onCheckedChange = onDriveBackup,
                    icon = if (showDriveBackupWarning)
                        Icons.Outlined.ErrorOutline else null,
                    iconTint = if (showDriveBackupWarning) errorColor else null,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.account),
                    summary = driveAccountSummary,
                    enabled = driveAccountEnabled,
                    onClick = onDriveAccount,
                )
            }
        }

        // Android Backup Service section
        SectionHeader(
            R.string.android_auto_backup,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                val errorColor = colorResource(R.color.overdue)
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    summary = lastAndroidBackupSummary,
                    checked = androidBackupEnabled,
                    onCheckedChange = onAndroidBackup,
                    icon = if (showAndroidBackupWarning)
                        Icons.Outlined.ErrorOutline else null,
                    iconTint = if (showAndroidBackupWarning) errorColor else null,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.device_settings),
                    summary = stringResource(R.string.android_auto_backup_device_summary),
                    summaryMaxLines = 4,
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = onDeviceSettings,
                )
            }
        }

        // Advanced section
        SectionHeader(
            R.string.preferences_advanced,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.backups_ignore_warnings),
                summary = stringResource(R.string.backups_ignore_warnings_summary),
                checked = ignoreWarnings,
                onCheckedChange = onIgnoreWarnings,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
