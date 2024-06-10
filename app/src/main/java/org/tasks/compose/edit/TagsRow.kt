package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.Chip
import org.tasks.compose.ChipGroup
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.TagData
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons
import org.tasks.themes.TasksTheme

@Composable
fun TagsRow(
    tags: List<TagData>,
    colorProvider: (Int) -> Int,
    onClick: () -> Unit,
    onClear: (TagData) -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_label_24px,
        content = {
            ChipGroup(modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)) {
                if (tags.isEmpty()) {
                    DisabledText(text = stringResource(id = R.string.add_tags))
                } else {
                    tags.sortedBy(TagData::name).forEach { tag ->
                        Chip(
                            icon = CustomIcons.getIcon(
                                tag.getIcon()!!,
                                R.drawable.ic_outline_label_24px
                            ),
                            name = tag.name,
                            theme = tag.color ?: 0,
                            showText = true,
                            showIcon = true,
                            onClick = onClick,
                            colorProvider = colorProvider,
                            clear = { onClear(tag) },
                        )
                    }
                }
            }
        },
        onClick = onClick
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoTags() {
    TasksTheme {
        TagsRow(
            tags = emptyList(),
            colorProvider = { 0 },
            onClick = {},
            onClear = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SingleTag() {
    TasksTheme {
        TagsRow(
            tags = listOf(
                TagData(
                    name = "Home",
                    icon = 1062,
                    color = ColorProvider.BLUE_500
                )
            ),
            colorProvider = { it },
            onClick = {},
            onClear = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun BunchOfTags() {
    TasksTheme {
        TagsRow(
            tags = listOf(
                TagData(name = "One"),
                TagData(name = "Two"),
                TagData(name = "Three"),
                TagData(name = "Four"),
                TagData(name = "Five"),
            ),
            colorProvider = { it },
            onClick = {},
            onClear = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun TagWithReallyLongName() {
    TasksTheme {
        TagsRow(
            tags = listOf(
                TagData(
                    name = "This is a tag with a really really long name",
                    icon = 1062,
                    color = ColorProvider.BLUE_500
                )
            ),
            colorProvider = { it },
            onClick = {},
            onClear = {},
        )
    }
}
