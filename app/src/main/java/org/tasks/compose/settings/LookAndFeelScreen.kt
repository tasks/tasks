package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tasks.R

@Composable
fun LookAndFeelScreen(
    themeName: String,
    dynamicColorAvailable: Boolean,
    dynamicColorEnabled: Boolean,
    dynamicColorProOnly: Boolean,
    themeColor: Int,
    launcherColor: Int,
    markdownEnabled: Boolean,
    openLastViewedList: Boolean,
    defaultFilterName: String,
    localeName: String,
    onTheme: () -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onColor: () -> Unit,
    onLauncher: () -> Unit,
    onMarkdown: (Boolean) -> Unit,
    onOpenLastViewedList: (Boolean) -> Unit,
    onDefaultFilter: () -> Unit,
    onLanguage: () -> Unit,
    onTranslations: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Appearance island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val showColor = !dynamicColorEnabled
            val total = 1 + (if (dynamicColorAvailable) 1 else 0) +
                    (if (showColor) 1 else 0) + 1
            var i = 0

            SettingsItemCard(position = cardPosition(i++, total)) {
                PreferenceRow(
                    title = stringResource(R.string.theme),
                    summary = themeName,
                    onClick = onTheme,
                )
            }
            if (dynamicColorAvailable) {
                SettingsItemCard(position = cardPosition(i++, total)) {
                    SwitchPreferenceRow(
                        title = stringResource(R.string.theme_dynamic),
                        checked = dynamicColorEnabled,
                        enabled = !dynamicColorProOnly,
                        onCheckedChange = onDynamicColor,
                        summary = if (dynamicColorProOnly)
                            stringResource(R.string.requires_pro_subscription)
                        else
                            null,
                    )
                }
            }
            if (showColor) {
                SettingsItemCard(position = cardPosition(i++, total)) {
                    PreferenceRow(
                        title = stringResource(R.string.color),
                        leading = { ColorIcon(Color(themeColor)) },
                        onClick = onColor,
                    )
                }
            }
            SettingsItemCard(position = cardPosition(i, total)) {
                PreferenceRow(
                    title = stringResource(R.string.launcher_icon),
                    leading = { ColorIcon(Color(launcherColor)) },
                    onClick = onLauncher,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Markdown island
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.markdown),
                summary = stringResource(R.string.markdown_description),
                checked = markdownEnabled,
                onCheckedChange = onMarkdown,
            )
        }

        // On launch section
        SectionHeader(
            R.string.on_launch,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.open_last_viewed_list),
                    checked = openLastViewedList,
                    onCheckedChange = onOpenLastViewedList,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.widget_open_list),
                    summary = defaultFilterName,
                    enabled = !openLastViewedList,
                    onClick = onDefaultFilter,
                )
            }
        }

        // Localization section
        SectionHeader(
            R.string.settings_localization,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.language),
                    summary = localeName,
                    onClick = onLanguage,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.translations),
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = onTranslations,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun ColorIcon(color: Color) {
    Box(
        modifier = Modifier
            .padding(start = SettingsContentPadding)
            .size(SettingsIconSize)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = colorResource(R.color.text_tertiary),
                shape = CircleShape,
            )
    )
}
