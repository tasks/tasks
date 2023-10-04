package org.tasks.filters

import com.todoroo.astrid.api.FilterListItem
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class NavigationDrawerSeparator : FilterListItem {
    @IgnoredOnParcel
    override val itemType = FilterListItem.Type.SEPARATOR

    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NavigationDrawerSeparator
    }

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return true
    }
}
