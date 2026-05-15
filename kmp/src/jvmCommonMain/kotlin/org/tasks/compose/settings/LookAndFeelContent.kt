package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.viewmodel.LookAndFeelViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.language
import tasks.kmp.generated.resources.restart_later
import tasks.kmp.generated.resources.restart_now
import tasks.kmp.generated.resources.restart_required
import tasks.kmp.generated.resources.theme
import tasks.kmp.generated.resources.theme_black
import tasks.kmp.generated.resources.theme_dark
import tasks.kmp.generated.resources.theme_day_night
import tasks.kmp.generated.resources.theme_light
import tasks.kmp.generated.resources.theme_system_default
import tasks.kmp.generated.resources.theme_wallpaper
import java.util.Locale

@Composable
fun LookAndFeelContent(
    viewModel: LookAndFeelViewModel,
    onColor: () -> Unit,
    onDefaultFilter: () -> Unit,
    onTranslations: () -> Unit,
    onRestartApplication: () -> Unit = {},
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val themeLight = stringResource(Res.string.theme_light)
    val themeBlack = stringResource(Res.string.theme_black)
    val themeDark = stringResource(Res.string.theme_dark)
    val themeWallpaper = stringResource(Res.string.theme_wallpaper)
    val themeDayNight = stringResource(Res.string.theme_day_night)
    val themeSystemDefault = stringResource(Res.string.theme_system_default)

    LookAndFeelScreen(
        themeName = when (viewModel.themeIndex) {
            0 -> themeLight
            1 -> themeBlack
            2 -> themeDark
            3 -> themeWallpaper
            4 -> themeDayNight
            else -> themeSystemDefault
        },
        dynamicColorAvailable = false,
        dynamicColorEnabled = false,
        dynamicColorProOnly = false,
        themeColor = viewModel.themeColor,
        launcherColor = viewModel.themeColor,
        markdownEnabled = viewModel.markdownEnabled,
        openLastViewedList = viewModel.openLastViewedList,
        defaultFilterName = "",
        localeName = viewModel.localeName,
        onTheme = { showThemeDialog = true },
        onDynamicColor = {},
        onColor = onColor,
        onLauncher = {},
        onMarkdown = { viewModel.updateMarkdownEnabled(it) },
        onOpenLastViewedList = { viewModel.updateOpenLastViewedList(it) },
        onDefaultFilter = onDefaultFilter,
        onLanguage = { showLanguageDialog = true },
        onTranslations = onTranslations,
    )

    if (showThemeDialog) {
        ThemeDialog(
            selectedIndex = viewModel.themeIndex,
            onSelect = { index ->
                viewModel.setTheme(index)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentTag = viewModel.languageTag,
            onSelect = { tag ->
                viewModel.updateLanguageTag(tag)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (viewModel.showRestartDialog) {
        ConfirmDialog(
            text = stringResource(Res.string.restart_required),
            confirmText = stringResource(Res.string.restart_now),
            dismissText = stringResource(Res.string.restart_later),
            onConfirm = onRestartApplication,
            onDismiss = { viewModel.dismissRestartDialog() },
        )
    }
}

@Composable
private fun ThemeDialog(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val names = listOf(
        stringResource(Res.string.theme_light),
        stringResource(Res.string.theme_black),
        stringResource(Res.string.theme_dark),
        stringResource(Res.string.theme_wallpaper),
        stringResource(Res.string.theme_day_night),
        stringResource(Res.string.theme_system_default),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.theme)) },
        text = {
            Column {
                names.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { onSelect(index) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = { onSelect(index) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguageDialog(
    currentTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val locales = remember {
        LookAndFeelViewModel.LOCALE_TAGS.mapNotNull { tag ->
            try {
                val locale = Locale.forLanguageTag(tag)
                val displayName = locale.getDisplayName(locale)
                if (displayName.isNotBlank()) LocaleItem(tag, displayName) else null
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.displayName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.language)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                locales.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { onSelect(item.tag) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentTag == item.tag,
                            onClick = { onSelect(item.tag) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

private data class LocaleItem(
    val tag: String,
    val displayName: String,
)
