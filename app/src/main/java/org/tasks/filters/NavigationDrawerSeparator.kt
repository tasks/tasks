package org.tasks.filters

import com.todoroo.astrid.api.FilterListItem

class NavigationDrawerSeparator : FilterListItem {
    override val itemType = FilterListItem.Type.SEPARATOR

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NavigationDrawerSeparator
    }

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return true
    }
}
