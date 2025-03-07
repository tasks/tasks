package org.tasks.compose.drawer

import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.key

sealed class DrawerItem {
    data class Filter(
        val title: String,
        val icon: String?,
        val color: Int = 0,
        val count: Int = 0,
        val shareCount: Int = 0,
        val selected: Boolean = false,
        val filter: org.tasks.filters.Filter,
    ) : DrawerItem() {
        override fun key() = filter.key()
    }

    data class Header(
        val title: String,
        val collapsed: Boolean,
        val hasError: Boolean,
        val canAdd: Boolean,
        val header: NavigationDrawerSubheader,
    ) : DrawerItem() {
        override fun key() = "header_${header.subheaderType}_${header.id}"
    }

    abstract fun key(): String
}
