package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.components.TasksIcon
import org.tasks.kmp.org.tasks.compose.settings.SettingRow
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme

@Composable
fun SelectIconRow(icon: String, selectIcon: () -> Unit) =
    SettingRow(
        modifier = Modifier.clickable(onClick =  selectIcon),
        left = {
            IconButton(onClick = selectIcon) {
                TasksIcon(
                    label = icon,
                    tint = colorResource(R.color.icon_tint_with_alpha)
                )
            }
        },
        center = {
            Text(
                text = LocalContext.current.getString(R.string.icon),
                modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
            )
        }
    )

@Composable
@Preview(showBackground = true)
private fun IconSelectPreview () {
    TasksTheme {
        SelectIconRow(
            icon = TasksIcons.FILTER_LIST,
            selectIcon = {}
        )
    }
}
