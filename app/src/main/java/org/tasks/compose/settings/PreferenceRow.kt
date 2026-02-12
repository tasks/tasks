package org.tasks.compose.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

val SettingsCardRadius = 20.dp
val SettingsCardInnerRadius = 4.dp
val SettingsCardGap = 2.dp
val SettingsRowPadding = 20.dp
val SettingsContentPadding = 16.dp
val SettingsIconSize = 24.dp
val SettingsSectionGap = 8.dp

enum class CardPosition { Only, First, Middle, Last }

@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    iconTint: Color? = null,
    summary: String? = null,
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
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(vertical = SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon slot
        if (icon != null) {
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
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Trailing content
        if (trailing != null) {
            trailing()
        } else {
            val trailingIcon = when {
                showError -> R.drawable.ic_outline_error_outline_24px
                showWarning -> R.drawable.ic_outline_error_outline_24px
                showChevron -> R.drawable.ic_keyboard_arrow_right_24px
                else -> null
            }
            if (trailingIcon != null) {
                val trailingTint = when {
                    showError -> errorColor
                    showWarning -> warningColor
                    else -> defaultTint
                }
                Icon(
                    painter = painterResource(trailingIcon),
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

@Composable
fun SectionHeader(
    @StringRes title: Int,
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
            text = stringResource(id = title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.help),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(SettingsIconSize),
            )
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
fun DangerCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
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
                .clickable(onClick = onClick)
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
                    painter = painterResource(trailingIcon),
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
