package org.tasks.previews

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import org.tasks.compose.drawer.DrawerItem
import org.tasks.compose.drawer.TaskListDrawer
import org.tasks.data.AccountIcon
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import org.tasks.viewmodel.DrawerViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_round_icon

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MenuPreview() {
    TasksTheme {
        TaskListDrawer(
            drawerOpen = true,
            drawerState = DrawerViewModel.State(
                drawerItems = persistentListOf(
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
                        hasChildren = true,
                        header = NavigationDrawerSubheader(
                            null,
                            false,
                            false,
                            NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                            "0",
                        ),
                    ),
                    DrawerItem.Filter(
                        title = "Recently Modified",
                        icon = TasksIcons.HISTORY,
                        filter = FilterImpl(),
                    ),
                    DrawerItem.Header(
                        title = "Tags",
                        collapsed = true,
                        canAdd = true,
                        hasChildren = true,
                        header = NavigationDrawerSubheader(
                            null,
                            false,
                            true,
                            NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                            "1",
                        ),
                    ),
                    DrawerItem.Header(
                        title = "Places",
                        collapsed = false,
                        canAdd = true,
                        hasChildren = true,
                        header = NavigationDrawerSubheader(
                            null,
                            false,
                            false,
                            NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                            "1",
                        ),
                    ),
                    DrawerItem.Filter(
                        title = "Home",
                        icon = TasksIcons.PLACE,
                        filter = FilterImpl(),
                        count = 5,
                    ),
                    DrawerItem.Header(
                        title = "tasks.org",
                        collapsed = false,
                        canAdd = true,
                        hasError = true,
                        hasChildren = true,
                        header = NavigationDrawerSubheader(
                            null,
                            true,
                            false,
                            NavigationDrawerSubheader.SubheaderType.TASKS,
                            "1",
                            accountIcon = AccountIcon(Res.drawable.ic_round_icon, true),
                        ),
                    ),
                    DrawerItem.Filter(
                        title = "Personal",
                        icon = TasksIcons.LIST,
                        filter = FilterImpl(),
                        count = 42,
                        selected = true,
                    ),
                    DrawerItem.Filter(
                        title = "Shared list",
                        icon = TasksIcons.LIST,
                        filter = FilterImpl(),
                        count = 3,
                        shareCount = 2,
                    ),
                    DrawerItem.Header(
                        title = "Google Tasks",
                        collapsed = false,
                        canAdd = true,
                        hasChildren = true,
                        header = NavigationDrawerSubheader(
                            null,
                            false,
                            false,
                            NavigationDrawerSubheader.SubheaderType.CALDAV,
                            "2",
                            accountIcon = AccountIcon(Res.drawable.ic_google, false),
                        ),
                    )
                ),
            ),
            onQueryChange = {},
            onClick = {},
            onAddClick = {},
            onErrorClick = {},
        )
    }
}
