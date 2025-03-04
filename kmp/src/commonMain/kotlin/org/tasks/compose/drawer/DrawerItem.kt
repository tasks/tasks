package org.tasks.compose.drawer

import androidx.compose.runtime.Stable
import org.tasks.filters.NavigationDrawerSubheader

@Stable
sealed class DrawerItem {
    @Stable
    data class Filter(
        val title: String,
        val icon: String?,
        val color: Int = 0,
        val count: Int = 0,
        val shareCount: Int = 0,
        val selected: Boolean = false,
        val filter: org.tasks.filters.Filter,
    ) : DrawerItem() {
        override fun key(): String {
            return "filter_${hashCode()}"
        }
    }

    @Stable
    data class Header(
        val title: String,
        val collapsed: Boolean,
        val hasError: Boolean,
        val canAdd: Boolean,
        val header: NavigationDrawerSubheader,
    ) : DrawerItem() {
        override fun key(): String {
            return "header_${header.subheaderType}_${header.id}"
        }
    }

    abstract fun key(): String
}
