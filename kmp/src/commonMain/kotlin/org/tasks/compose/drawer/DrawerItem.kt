package org.tasks.compose.drawer

import androidx.compose.runtime.Stable
import co.touchlab.kermit.Logger
import org.tasks.data.UUIDHelper
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TimerFilter
import org.tasks.filters.TodayFilter

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
        override fun key() = when (filter) {
            is CustomFilter -> "custom_${filter.id}"
            is CaldavFilter -> "list_${filter.account.id}_${filter.calendar.id}"
            is PlaceFilter -> "place_${filter.place.id}"
            is TagFilter -> "tag_${filter.tagData.id}"
            is MyTasksFilter -> "builtin_my_tasks"
            is TodayFilter -> "builtin_today"
            is RecentlyModifiedFilter -> "builtin_recently_modified"
            is TimerFilter -> "builtin_timer"
            is SnoozedFilter -> "builtin_snoozed"
            is NotificationsFilter -> "builtin_notifications"
            else -> {
                Logger.w { "Unexpected filter type: ${filter.javaClass.name}" }
                UUIDHelper.newUUID()
            }
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
        override fun key() = "header_${header.subheaderType}_${header.id}"
    }

    abstract fun key(): String
}
