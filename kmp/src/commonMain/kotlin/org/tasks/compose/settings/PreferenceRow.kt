package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.tasks.themes.WarningColor

@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconDrawable: DrawableResource? = null,
    iconTint: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    summary: String? = null,
    titleMaxLines: Int = 1,
    summaryMaxLines: Int = 2,
    showWarning: Boolean = false,
    showError: Boolean = false,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val defaultTint = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = WarningColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled && onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            leading != null -> {
                leading()
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = iconTint ?: defaultTint
                )
            }
            iconDrawable != null -> {
                Icon(
                    painter = painterResource(iconDrawable),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = iconTint ?: defaultTint
                )
            }
            else -> {
                Spacer(modifier = Modifier.width(SettingsContentPadding + SettingsIconSize))
            }
        }

        Spacer(modifier = Modifier.width(SettingsContentPadding))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = SettingsContentPadding)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = summaryMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            val trailingIcon = when {
                showError -> Icons.Outlined.ErrorOutline
                showWarning -> Icons.Outlined.ErrorOutline
                showChevron -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                else -> null
            }
            if (trailingIcon != null) {
                val trailingTint = when {
                    showError -> errorColor
                    showWarning -> warningColor
                    else -> defaultTint
                }
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = trailingTint
                )
            }
        }
    }
}

@Composable
fun SwitchPreferenceRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    summary: String? = null,
) {
    PreferenceRow(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        iconTint = iconTint,
        summary = summary,
        summaryMaxLines = Int.MAX_VALUE,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.padding(end = SettingsContentPadding),
            )
        },
    )
}

@Composable
fun DangerCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsCardRadius),
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = SettingsRowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = SettingsContentPadding)
                    .size(SettingsIconSize),
                tint = tint,
            )
            Spacer(modifier = Modifier.width(SettingsContentPadding))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = tint.copy(alpha = 0.6f),
                )
            }
        }
    }
}

