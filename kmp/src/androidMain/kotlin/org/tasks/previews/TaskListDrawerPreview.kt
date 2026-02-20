package org.tasks.previews

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.MenuSearchBar
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.AccountIcon
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_round_icon

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MenuPreview() {
    TasksTheme {
        TaskListDrawer(
            arrangement = Arrangement.Top,
            searchBar = {
                MenuSearchBar(
                    begForMoney = true,
                    onDrawerAction = {},
                    query = "",
                    onQueryChange = {}
                )
            },
            filters = persistentListOf(
                DrawerItem.Filter(
                    title = "My Tasks",
                    icon = TasksIcons.ALL_INBOX,
                    filter = FilterImpl(),
                    count = 100
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
                        "filters",
                        icon = TasksIcons.FILTER_LIST,
                    ),
                ),
                DrawerItem.Header(
                    title = "Tags",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    header = NavigationDrawerSubheader(
                        null,
                        false,
                        false,
                        NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                        "tags",
                        icon = TasksIcons.LABEL,
                    ),
                ),
                DrawerItem.Header(
                    title = "Places",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    header = NavigationDrawerSubheader(
                        null,
                        false,
                        false,
                        NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                        "places",
                        icon = TasksIcons.PLACE,
                    ),
                ),
                DrawerItem.Filter(
                    title = "Home",
                    icon = TasksIcons.PLACE,
                    filter = FilterImpl(),
                    count = 1,
                ),
                DrawerItem.Filter(
                    title = "Work",
                    icon = TasksIcons.PLACE,
                    filter = FilterImpl(),
                    count = 17,
                ),
                DrawerItem.Header(
                    title = "user@gmail.com",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    header = NavigationDrawerSubheader(
                        "user@gmail.com",
                        false,
                        false,
                        NavigationDrawerSubheader.SubheaderType.TASKS,
                        "1",
                        accountIcon = AccountIcon(Res.drawable.ic_round_icon, false),
                    ),
                ),
                DrawerItem.Filter(
                    title = "A really really really really really really really really really really really really really long title",
                    icon = TasksIcons.LIST,
                    filter = FilterImpl(),
                    count = 123456,
                ),
                DrawerItem.Header(
                    title = "user@gmail.com",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    header = NavigationDrawerSubheader(
                        "user@gmail.com",
                        false,
                        false,
                        NavigationDrawerSubheader.SubheaderType.CALDAV,
                        "2",
                        accountIcon = AccountIcon(Res.drawable.ic_google, false),
                    ),
                )
            ),
            onClick = {},
            onAddClick = {},
            onErrorClick = {},
        )
    }
}
