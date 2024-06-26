package org.tasks.compose.drawer

import org.tasks.filters.NavigationDrawerSubheader

sealed interface DrawerItem {
    data class Filter(
        val title: String,
        val icon: String,
        val color: Int = 0,
        val count: Int = 0,
        val shareCount: Int = 0,
        val selected: Boolean = false,
        val type: () -> org.tasks.filters.Filter,
    ) : DrawerItem
    data class Header(
        val title: String,
        val collapsed: Boolean,
        val hasError: Boolean,
        val canAdd: Boolean,
        val type: () -> NavigationDrawerSubheader,
    ) : DrawerItem
}
