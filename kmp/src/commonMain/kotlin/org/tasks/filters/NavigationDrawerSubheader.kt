package org.tasks.filters

import org.tasks.data.AccountIcon

data class NavigationDrawerSubheader(
    val title: String?,
    val error: Boolean,
    val isCollapsed: Boolean,
    val subheaderType: SubheaderType,
    val id: String,
    val addIntentRc: Int = 0,
    val icon: String? = null,
    val accountIcon: AccountIcon? = null,
) : FilterListItem {
    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NavigationDrawerSubheader && subheaderType == other.subheaderType && id == other.id
    }

    override val itemType = FilterListItem.Type.SUBHEADER

    enum class SubheaderType {
        PREFERENCE,
        CALDAV,
        TASKS,
    }
}
