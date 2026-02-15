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
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun AdvancedScreen(
    astridSortEnabled: Boolean,
    attachmentDirSummary: String,
    calendarEndAtDueTime: Boolean,
    onAstridSort: (Boolean) -> Unit,
    onAttachmentDir: () -> Unit,
    onCalendarEndAtDueTime: (Boolean) -> Unit,
    onDeleteCompletedEvents: () -> Unit,
    onDeleteAllEvents: () -> Unit,
    onResetPreferences: () -> Unit,
    onDeleteTaskData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Astrid sort & attachment dir island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.astrid_sort_order),
                    summary = stringResource(R.string.astrid_sort_order_summary),
                    checked = astridSortEnabled,
                    onCheckedChange = onAstridSort,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.attachment_directory),
                    icon = Icons.Outlined.Attachment,
                    summary = attachmentDirSummary,
                    onClick = onAttachmentDir,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Calendar island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.EPr_cal_end_or_start_at_due_time),
                    icon = Icons.Outlined.Event,
                    summary = if (calendarEndAtDueTime)
                        stringResource(R.string.EPr_cal_start_at_due_time)
                    else
                        stringResource(R.string.EPr_cal_end_at_due_time),
                    checked = calendarEndAtDueTime,
                    onCheckedChange = onCalendarEndAtDueTime,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_manage_delete_completed_gcal),
                    onClick = onDeleteCompletedEvents,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_manage_delete_all_gcal),
                    onClick = onDeleteAllEvents,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Danger zone island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_reset_preferences),
                    onClick = onResetPreferences,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_delete_task_data),
                    icon = Icons.Outlined.Delete,
                    onClick = onDeleteTaskData,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
