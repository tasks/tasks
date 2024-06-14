package org.tasks.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.themes.CustomIcons
import org.tasks.themes.TasksTheme

@Composable
fun Chip(
    @DrawableRes icon: Int?,
    name: String?,
    theme: Int,
    showText: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    colorProvider: (Int) -> Int,
    clear: (() -> Unit)? = null,
) {
    Chip(
        color = remember(theme) { Color(colorProvider(theme)) },
        text = if (showText) name else null,
        icon = if (showIcon && icon != null) icon else null,
        onClick = onClick,
        clear = clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(
    text: String? = null,
    icon: Int? = null,
    color: Color,
    onClick: () -> Unit = {},
    clear: (() -> Unit)? = null,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false
    ) {
        FilterChip(
            selected = false,
            onClick = onClick,
            border = BorderStroke(1.dp, color = color),
            leadingIcon = {
                if (text != null) {
                    ChipIcon(iconRes = icon)
                }
            },
            trailingIcon = {
                clear?.let { onClearClick ->
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClearClick() },
                        contentDescription = stringResource(id = R.string.delete),
                    )
                }
            },
            modifier = Modifier.defaultMinSize(minHeight = 26.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = color.copy(alpha = .1f),
                iconColor = MaterialTheme.colorScheme.onSurface,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
            label = {
                if (text == null) {
                    ChipIcon(iconRes = icon)
                }
                text?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        )
    }
}

@Composable
private fun ChipIcon(iconRes: Int?) {
    iconRes?.let {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipIconAndTextPreview() {
    TasksTheme {
        Chip(
            text = "Home",
            icon = CustomIcons.getIconResId(CustomIcons.LABEL),
            color = Color.Red,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipIconTextAndClearPreview() {
    TasksTheme {
        Chip(
            text = "Home",
            icon = CustomIcons.getIconResId(CustomIcons.LABEL),
            color = Color.Red,
            clear = {},
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipIconPreview() {
    TasksTheme {
        Chip(
            text = null,
            icon = CustomIcons.getIconResId(CustomIcons.LABEL),
            color = Color.Red,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipTextPreview() {
    TasksTheme {
        Chip(
            text = "Home",
            icon = null,
            color = Color.Red,
        )
    }
}