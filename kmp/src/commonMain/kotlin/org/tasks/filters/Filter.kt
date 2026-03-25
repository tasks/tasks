package org.tasks.filters

import androidx.compose.runtime.Stable
import co.touchlab.kermit.Logger
import org.tasks.CommonParcelable
import org.tasks.data.NO_COUNT
import org.tasks.data.NO_ORDER
import org.tasks.data.entity.CaldavCalendar


@Stable
abstract class Filter : FilterListItem, CommonParcelable {
    open val valuesForNewTasks: String?
        get() = null
    abstract val sql: String?
    open val icon: String?
        get() = null
    abstract val title: String
    open val tint: Int
        get() = 0
    @Deprecated("Remove this")
    open val count: Int
        get() = NO_COUNT
    open val order: Int
        get() = NO_ORDER
    override val itemType: FilterListItem.Type
        get() = FilterListItem.Type.ITEM
    open val isReadOnly: Boolean
        get() = false
    val isWritable: Boolean
        get() = !isReadOnly
    val listId: String?
        get() = when(this) {
            is CaldavFilter -> calendar.uuid
            is TagFilter -> tagData.remoteId
            else -> null
        }

    open fun supportsManualSort(): Boolean = false
    open fun supportsHiddenTasks(): Boolean = true
    open fun supportsSubtasks(): Boolean = true
    open fun supportsSorting(): Boolean = true
    open fun disableHeaders(): Boolean = !supportsSorting()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Filter) return false
        return areItemsTheSame(other)
    }

    override fun hashCode(): Int = key().hashCode()
}

fun Filter.key(): String = when (this) {
    is CustomFilter -> "custom_${id}"
    is CaldavFilter -> calendar.filterPreferencesKey()
    is PlaceFilter -> "place_${place.uid}"
    is TagFilter -> "tag_${tagData.remoteId}"
    is MyTasksFilter -> "builtin_my_tasks"
    is TodayFilter -> "builtin_today"
    is RecentlyModifiedFilter -> "builtin_recently_modified"
    is TimerFilter -> "builtin_timer"
    is SnoozedFilter -> "builtin_snoozed"
    is NotificationsFilter -> "builtin_notifications"
    is SearchFilter -> "builtin_search"
    is EmptyFilter -> "builtin_empty"
    is DebugFilter -> title
    else -> {
        Logger.w { "Unexpected filter type: ${javaClass.name}" }
        "unknown_${sql.hashCode()}"
    }
}

fun CaldavCalendar.filterPreferencesKey(): String = "list_${account}_${uuid}"
