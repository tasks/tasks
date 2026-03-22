package org.tasks.compose.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.tasks.R


@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
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
    val defaultTint = colorResource(R.color.icon_tint_with_alpha)
    val errorColor = colorResource(R.color.overdue)
    val warningColor = colorResource(org.tasks.kmp.R.color.orange_500)

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
        // Icon slot
        if (leading != null) {
            leading()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = SettingsContentPadding)
                    .size(SettingsIconSize),
                tint = iconTint ?: defaultTint
            )
        } else if (iconRes != null && iconRes != 0) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = SettingsContentPadding)
                    .size(SettingsIconSize),
                tint = iconTint ?: defaultTint
            )
        } else {
            Spacer(modifier = Modifier.width(SettingsContentPadding + SettingsIconSize))
        }

        Spacer(modifier = Modifier.width(SettingsContentPadding))

        // Title and summary
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

        // Trailing content
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

fun cardPosition(index: Int, total: Int) = when {
    total == 1 -> CardPosition.Only
    index == 0 -> CardPosition.First
    index == total - 1 -> CardPosition.Last
    else -> CardPosition.Middle
}

@Composable
fun SectionHeader(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    SectionHeader(
        title = stringResource(id = title),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun SwitchPreferenceRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    iconTint: Color? = null,
    summary: String? = null,
) {
    PreferenceRow(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        iconRes = iconRes,
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
