package org.tasks.filters

import co.touchlab.kermit.Logger
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao

class FilterPreferenceCodec(
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val caldavDao: CaldavDao,
    private val locationDao: LocationDao,
) {
    suspend fun decode(preferenceValue: String?, default: Filter? = null): Filter? = try {
        preferenceValue?.let { load(it) } ?: default
    } catch (e: Exception) {
        Logger.e(e, tag = TAG) { "Failed to load filter from '$preferenceValue'" }
        default
    }

    fun encode(filter: Filter): String? = when (filter) {
        is TagFilter -> "$TYPE_TAG:${filter.uuid}"
        is CustomFilter -> "$TYPE_CUSTOM_FILTER:${filter.id}"
        is CaldavFilter -> "$TYPE_CALDAV:${filter.uuid}"
        is PlaceFilter -> "$TYPE_LOCATION:${filter.uid}"
        else -> "$TYPE_FILTER:${builtInId(filter)}"
    }

    private suspend fun load(preferenceValue: String): Filter? {
        val split = preferenceValue.split(":")
        if (split.size < 2) return null
        return when (split[0].toIntOrNull()) {
            TYPE_FILTER -> builtIn(split[1].toIntOrNull() ?: FILTER_MY_TASKS)
            TYPE_CUSTOM_FILTER ->
                split[1].toLongOrNull()?.let { filterDao.getById(it) }?.let(::CustomFilter)
            TYPE_TAG ->
                tagDataDao.getByUuid(split[1])
                    ?.takeIf { !it.name.isNullOrBlank() }
                    ?.let(::TagFilter)
            // TODO: convert filters from old ID to uuid?
            TYPE_GOOGLE_TASKS,
            TYPE_CALDAV ->
                caldavDao.getCalendarByUuid(split[1])
                    ?.let { CaldavFilter(it, caldavDao.getAccountByUuid(it.account!!)!!) }
            TYPE_LOCATION -> locationDao.getPlace(split[1])?.let { PlaceFilter(it) }
            else -> null
        }
    }

    private suspend fun builtIn(id: Int): Filter = when (id) {
        FILTER_TODAY -> TodayFilter.create()
        FILTER_RECENTLY_MODIFIED -> RecentlyModifiedFilter.create()
        FILTER_SNOOZED -> SnoozedFilter.create()
        FILTER_NOTIFICATIONS -> NotificationsFilter.create()
        else -> MyTasksFilter.create()
    }

    private fun builtInId(filter: Filter) = when (filter) {
        is TodayFilter -> FILTER_TODAY
        is RecentlyModifiedFilter -> FILTER_RECENTLY_MODIFIED
        is SnoozedFilter -> FILTER_SNOOZED
        is NotificationsFilter -> FILTER_NOTIFICATIONS
        else -> FILTER_MY_TASKS
    }

    companion object {
        const val TYPE_FILTER = 0
        const val TYPE_CUSTOM_FILTER = 1
        const val TYPE_TAG = 2

        @Deprecated("use TYPE_CALDAV")
        const val TYPE_GOOGLE_TASKS = 3
        const val TYPE_CALDAV = 4
        const val TYPE_LOCATION = 5

        const val FILTER_MY_TASKS = 0
        const val FILTER_TODAY = 1

        @Suppress("unused")
        const val FILTER_UNCATEGORIZED = 2
        const val FILTER_RECENTLY_MODIFIED = 3
        const val FILTER_SNOOZED = 4
        const val FILTER_NOTIFICATIONS = 5

        private const val TAG = "FilterPreferenceCodec"
    }
}
