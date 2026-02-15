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
fun DateAndTimeScreen(
    fullDateEnabled: Boolean,
    morningSummary: String,
    afternoonSummary: String,
    eveningSummary: String,
    nightSummary: String,
    autoDismissListEnabled: Boolean,
    autoDismissEditEnabled: Boolean,
    autoDismissWidgetEnabled: Boolean,
    onFullDate: (Boolean) -> Unit,
    onMorning: () -> Unit,
    onAfternoon: () -> Unit,
    onEvening: () -> Unit,
    onNight: () -> Unit,
    onAutoDismissInfo: () -> Unit,
    onAutoDismissList: (Boolean) -> Unit,
    onAutoDismissEdit: (Boolean) -> Unit,
    onAutoDismissWidget: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Full date island
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.always_display_full_date),
                checked = fullDateEnabled,
                onCheckedChange = onFullDate,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Time shortcuts island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.date_shortcut_morning),
                    summary = morningSummary,
                    onClick = onMorning,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.date_shortcut_afternoon),
                    summary = afternoonSummary,
                    onClick = onAfternoon,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.date_shortcut_evening),
                    summary = eveningSummary,
                    onClick = onEvening,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.date_shortcut_night),
                    summary = nightSummary,
                    onClick = onNight,
                )
            }
        }

        // Autoclose date time picker section
        SectionHeader(
            R.string.auto_dismiss_datetime,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            onClick = onAutoDismissInfo,
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.auto_dismiss_datetime_list),
                    summary = stringResource(R.string.auto_dismiss_datetime_list_summary),
                    checked = autoDismissListEnabled,
                    onCheckedChange = onAutoDismissList,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.auto_dismiss_datetime_edit),
                    summary = stringResource(R.string.auto_dismiss_datetime_edit_summary),
                    checked = autoDismissEditEnabled,
                    onCheckedChange = onAutoDismissEdit,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.auto_dismiss_datetime_widget),
                    summary = stringResource(R.string.auto_dismiss_datetime_widget_summary),
                    checked = autoDismissWidgetEnabled,
                    onCheckedChange = onAutoDismissWidget,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
