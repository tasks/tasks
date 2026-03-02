package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.components.imageVectorByName
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import org.tasks.themes.chipColors

@Composable
fun Chip(
    icon: String?,
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

@Composable
fun Chip(
    text: String? = null,
    icon: String? = null,
    color: Color,
    onClick: () -> Unit = {},
    clear: (() -> Unit)? = null,
) {
    val colors = chipColors(color.toArgb())
    val bgColor = Color(colors.backgroundColor)
    val onColor = Color(colors.contentColor)
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = bgColor,
        modifier = Modifier.defaultMinSize(minHeight = 26.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            ChipIcon(icon = icon, tint = onColor)
            text?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = onColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            clear?.let { onClearClick ->
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClearClick() },
                    contentDescription = stringResource(id = R.string.delete),
                    tint = onColor,
                )
            }
        }
    }
    }
}

@Composable
private fun ChipIcon(icon: String?, tint: Color = Color.Unspecified) {
    icon?.let {
        Icon(
            imageVector = imageVectorByName(it) ?: return@let,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint,
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
            icon = TasksIcons.LABEL,
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
            icon = TasksIcons.LABEL,
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
            icon = TasksIcons.LABEL,
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