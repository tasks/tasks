package org.tasks.compose.edit

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.Chip
import org.tasks.compose.ChipGroup
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.TagData
import org.tasks.themes.CustomIcons

@Composable
fun TagsRow(
    tags: List<TagData>,
    colorProvider: (Int) -> Int,
    onClick: () -> Unit,
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
                            theme = tag.getColor()!!,
                            showText = true,
                            showIcon = true,
                            onClick = onClick,
                            colorProvider = colorProvider,
                        )
                    }
                }
            }
        },
        onClick = onClick
    )
}
