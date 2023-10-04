package org.tasks.filters

import android.content.Intent
import com.todoroo.astrid.api.FilterListItem
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigationDrawerSubheader(
    val listingTitle: String?,
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

    override fun areContentsTheSame(other: FilterListItem): Boolean {
        return this == other
    }

    @IgnoredOnParcel
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
