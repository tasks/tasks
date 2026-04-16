package org.tasks.compose.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun PreferenceRow(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconTint: Color? = null,
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
    PreferenceRow(
        title = title,
        modifier = modifier,
        enabled = enabled,
        iconTint = iconTint,
        summary = summary,
        titleMaxLines = titleMaxLines,
        summaryMaxLines = summaryMaxLines,
        showWarning = showWarning,
        showError = showError,
        showChevron = showChevron,
        trailing = trailing,
        onClick = onClick,
        leading = if (iconRes != 0) {
            {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = SettingsContentPadding)
                        .size(SettingsIconSize),
                    tint = iconTint ?: defaultTint
                )
            }
        } else {
            null
        },
    )
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
