package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.TasksIcon
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_shortcut_to_home_screen
import tasks.kmp.generated.resources.add_widget_to_home_screen
import tasks.kmp.generated.resources.color
import tasks.kmp.generated.resources.help
import tasks.kmp.generated.resources.icon

val SettingsCardRadius = 20.dp
val SettingsCardInnerRadius = 4.dp
val SettingsCardGap = 2.dp
val SettingsRowPadding = 20.dp
val SettingsContentPadding = 16.dp
val SettingsIconSize = 24.dp
val SettingsSectionGap = 8.dp

enum class CardPosition {
    Only, First, Middle, Last;

    companion object {
        fun forIndex(index: Int, size: Int): CardPosition = when {
            size == 1 -> Only
            index == 0 -> First
            index == size - 1 -> Last
            else -> Middle
        }
    }
}

@Composable
fun SettingsItemCard(
    position: CardPosition = CardPosition.Only,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = when (position) {
        CardPosition.Only -> RoundedCornerShape(SettingsCardRadius)
        CardPosition.First -> RoundedCornerShape(topStart = SettingsCardRadius, topEnd = SettingsCardRadius, bottomStart = SettingsCardInnerRadius, bottomEnd = SettingsCardInnerRadius)
        CardPosition.Middle -> RoundedCornerShape(SettingsCardInnerRadius)
        CardPosition.Last -> RoundedCornerShape(topStart = SettingsCardInnerRadius, topEnd = SettingsCardInnerRadius, bottomStart = SettingsCardRadius, bottomEnd = SettingsCardRadius)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        content()
    }
}

/**
 * The "Add shortcut to home screen" / "Add widget to home screen" cards, shared between the list
 * and tag settings screens. Renders nothing (and no leading spacer) when both callbacks are null.
 */
@Composable
fun ShortcutWidgetCards(
    onAddShortcut: (() -> Unit)?,
    onAddWidget: (() -> Unit)?,
    enabled: Boolean,
) {
    if (onAddShortcut == null && onAddWidget == null) return
    Spacer(modifier = Modifier.height(SettingsContentPadding))
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        val items = listOfNotNull(
            onAddShortcut?.let {
                Triple(Res.string.add_shortcut_to_home_screen, Icons.Outlined.Home, it)
            },
            onAddWidget?.let {
                Triple(Res.string.add_widget_to_home_screen, Icons.Outlined.Widgets, it)
            },
        )
        items.forEachIndexed { index, (label, icon, onClick) ->
            SettingsItemCard(position = CardPosition.forIndex(index, items.size)) {
                PreferenceRow(
                    title = stringResource(label),
                    icon = icon,
                    enabled = enabled,
                    onClick = onClick,
                )
            }
        }
    }
}

/**
 * The color + icon picker cards, shared between the list and tag settings screens. Renders a color
 * swatch (or a "no color" icon) with a clear button, followed by the icon row.
 */
@Composable
fun ColorIconCards(
    color: Int,
    icon: String,
    pickerColors: List<PickerColor>,
    onColorClick: () -> Unit,
    onClearColor: () -> Unit,
    onIconClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            PreferenceRow(
                title = stringResource(Res.string.color),
                showChevron = color == 0,
                onClick = onColorClick,
                leading = {
                    if (color != 0) {
                        val bgColor = Color(
                            pickerColors
                                .firstOrNull { it.originalColor == color }
                                ?.primaryColor
                                ?: color
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = SettingsContentPadding)
                                .size(SettingsIconSize)
                                .clip(CircleShape)
                                .background(bgColor),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.NotInterested,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = SettingsContentPadding)
                                .size(SettingsIconSize),
                        )
                    }
                },
                trailing = if (color != 0) {
                    {
                        IconButton(
                            onClick = onClearColor,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                            )
                        }
                    }
                } else null,
            )
        }
        SettingsItemCard(position = CardPosition.Last) {
            PreferenceRow(
                title = stringResource(Res.string.icon),
                showChevron = true,
                onClick = onIconClick,
                leading = {
                    TasksIcon(
                        label = icon,
                        modifier = Modifier
                            .padding(start = SettingsContentPadding)
                            .size(SettingsIconSize),
                    )
                },
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .then(
                if (onClick != null)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier
            ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(Res.string.help),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(SettingsIconSize),
            )
        }
    }
}
