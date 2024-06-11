package org.tasks.filters

import android.content.Intent

data class NavigationDrawerSubheader(
    val title: String?,
    val error: Boolean,
    val isCollapsed: Boolean,
    val subheaderType: SubheaderType,
    val id: Long,
    val addIntentRc: Int,
    val addIntent: Intent?,
) : FilterListItem {
    override fun areItemsTheSame(other: FilterListItem): Boolean {
        return other is NavigationDrawerSubheader && subheaderType == other.subheaderType && id == other.id
    }

    override val itemType = FilterListItem.Type.SUBHEADER

    enum class SubheaderType {
        PREFERENCE,
        GOOGLE_TASKS,
        CALDAV,
        TASKS,
        @Deprecated("")
        ETESYNC
    }
}
