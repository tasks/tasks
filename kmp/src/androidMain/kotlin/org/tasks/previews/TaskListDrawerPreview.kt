package org.tasks.previews

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MenuPreview() {
    TasksTheme {
        TaskListDrawer(
            begForMoney = true,
            filters = persistentListOf(
                DrawerItem.Filter(
                    title = "My Tasks",
                    icon = TasksIcons.ALL_INBOX,
                    filter = FilterImpl(),
                ),
                DrawerItem.Header(
                    title = "Filters",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    header = NavigationDrawerSubheader(
                        null,
                        false,
                        false,
                        NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                        "",
                    ),
                )
            ),
            onClick = {},
            onDrawerAction = {},
            onAddClick = {},
            onErrorClick = {},
            query = "",
            onQueryChange = {},
        )
    }
}
