package org.tasks.filters

import android.content.Intent
import com.todoroo.astrid.api.FilterListItem
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigationDrawerAction(
    val listingTitle: String,
    val icon: Int,
    val requestCode: Int,
    val intent: Intent? = null,
) : FilterListItem {
    @IgnoredOnParcel
    override val itemType = FilterListItem.Type.ACTION

    override fun areItemsTheSame(other: FilterListItem) = this == other

    override fun areContentsTheSame(other: FilterListItem) = true
}
