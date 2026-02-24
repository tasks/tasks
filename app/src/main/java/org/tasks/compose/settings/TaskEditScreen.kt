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
fun TaskEditScreen(
    showWithoutUnlockVisible: Boolean,
    showLinks: Boolean,
    backButtonSaves: Boolean,
    multilineTitle: Boolean,
    showComments: Boolean,
    showWithoutUnlock: Boolean,
    onCustomizeEditScreen: () -> Unit,
    onShowLinks: (Boolean) -> Unit,
    onBackButtonSaves: (Boolean) -> Unit,
    onMultilineTitle: (Boolean) -> Unit,
    onShowComments: (Boolean) -> Unit,
    onShowWithoutUnlock: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Customize edit screen
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            PreferenceRow(
                title = stringResource(R.string.customize_edit_screen),
                summary = stringResource(R.string.customize_edit_screen_summary),
                showChevron = true,
                onClick = onCustomizeEditScreen,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Options island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val total = if (showWithoutUnlockVisible) 5 else 4
            var i = 0

            SettingsItemCard(position = cardPosition(i++, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.linkify),
                    summary = stringResource(R.string.linkify_description),
                    checked = showLinks,
                    onCheckedChange = onShowLinks,
                )
            }
            SettingsItemCard(position = cardPosition(i++, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.back_button_saves_task),
                    checked = backButtonSaves,
                    onCheckedChange = onBackButtonSaves,
                )
            }
            SettingsItemCard(position = cardPosition(i++, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.multiline_title),
                    summary = if (multilineTitle)
                        stringResource(R.string.multiline_title_on)
                    else
                        stringResource(R.string.multiline_title_off),
                    checked = multilineTitle,
                    onCheckedChange = onMultilineTitle,
                )
            }
            SettingsItemCard(position = cardPosition(i++, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.EPr_show_task_edit_comments),
                    checked = showComments,
                    onCheckedChange = onShowComments,
                )
            }
            if (showWithoutUnlockVisible) {
                SettingsItemCard(position = cardPosition(i, total)) {
                    SwitchPreferenceRow(
                        title = stringResource(R.string.show_edit_screen_without_unlock),
                        summary = stringResource(R.string.show_edit_screen_without_unlock_summary),
                        checked = showWithoutUnlock,
                        onCheckedChange = onShowWithoutUnlock,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
