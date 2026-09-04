package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.locale.SUPPORTED_LANGUAGE_TAGS
import org.tasks.viewmodel.LookAndFeelViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.language
import tasks.kmp.generated.resources.restart_later
import tasks.kmp.generated.resources.restart_now
import tasks.kmp.generated.resources.restart_required
import tasks.kmp.generated.resources.theme
import tasks.kmp.generated.resources.theme_system_default
import java.util.Locale

@Composable
fun LookAndFeelContent(
    viewModel: LookAndFeelViewModel,
    onColor: () -> Unit,
    onLauncher: () -> Unit = {},
    onDefaultFilter: () -> Unit,
    onTranslations: () -> Unit,
    onRestartApplication: () -> Unit = {},
) {
    if (!viewModel.loaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    LookAndFeelScreen(
        themeName = baseThemeName(viewModel.themeIndex),
        dynamicColorAvailable = viewModel.dynamicColorAvailable,
        dynamicColorEnabled = viewModel.dynamicColorEnabled,
        dynamicColorProOnly = viewModel.dynamicColorProOnly,
        themeColor = viewModel.themeColor,
        launcherColor = viewModel.launcherColor,
        showLauncherIcon = viewModel.showLauncherIcon,
        showLanguage = viewModel.showLanguage,
        showMarkdown = viewModel.showMarkdown,
        markdownEnabled = viewModel.settings.markdown,
        openLastViewedList = viewModel.settings.openLastViewedList,
        defaultFilterName = viewModel.defaultFilterName,
        localeName = viewModel.localeName,
        onTheme = { showThemeDialog = true },
        onDynamicColor = { viewModel.setDynamicColor(it) },
        onColor = onColor,
        onLauncher = onLauncher,
        onMarkdown = { viewModel.setMarkdown(it) },
        onOpenLastViewedList = { viewModel.setOpenLastViewedList(it) },
        onDefaultFilter = onDefaultFilter,
        onLanguage = { showLanguageDialog = true },
        onTranslations = onTranslations,
    )

    if (showThemeDialog) {
        ThemeDialog(
            options = viewModel.themeOptions,
            selectedIndex = viewModel.themeIndex,
            onSelect = {
                viewModel.setTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedTag = viewModel.settings.languageTag,
            onSelect = {
                viewModel.setLanguage(it)
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
    options: List<Int>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ChoiceDialog(
        title = stringResource(Res.string.theme),
        onDismiss = onDismiss,
    ) {
        options.forEach { index ->
            ChoiceRow(
                label = baseThemeName(index),
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val languages = remember {
        SUPPORTED_LANGUAGE_TAGS
            .mapNotNull { tag ->
                val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull()
                locale
                    ?.getDisplayName(locale)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { tag to it }
            }
            .sortedBy { it.second.lowercase() }
    }
    ChoiceDialog(
        title = stringResource(Res.string.language),
        onDismiss = onDismiss,
    ) {
        ChoiceRow(
            label = stringResource(Res.string.theme_system_default),
            selected = selectedTag.isNullOrBlank(),
            onClick = { onSelect(null) },
        )
        languages.forEach { (tag, displayName) ->
            ChoiceRow(
                label = displayName,
                selected = selectedTag == tag,
                onClick = { onSelect(tag) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DialogHorizontalPadding),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = DialogHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private val DialogHorizontalPadding = 24.dp
