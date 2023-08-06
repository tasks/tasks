package org.tasks.preferences

import android.content.Context
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.core.BuiltInFilterExposer.Companion.getMyTasksFilter
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.FilterDao
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.LocationDao
import org.tasks.data.TagDataDao
import org.tasks.filters.PlaceFilter
import timber.log.Timber
import javax.inject.Inject

class DefaultFilterProvider @Inject constructor(
        // TODO: don't inject context, it breaks built-in filters when overriding language
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val filterDao: FilterDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao,
        private val locationDao: LocationDao,
        private val googleTaskDao: GoogleTaskDao) {

    var dashclockFilter: Filter
        @Deprecated("use coroutines") get() = runBlocking { getFilterFromPreference(R.string.p_dashclock_filter) }
        set(filter) = setFilterPreference(filter, R.string.p_dashclock_filter)

    var lastViewedFilter: Filter
        @Deprecated("use coroutines") get() = runBlocking { getFilterFromPreference(R.string.p_last_viewed_list) }
        set(filter) = setFilterPreference(filter, R.string.p_last_viewed_list)

    var defaultList: Filter
        @Deprecated("use coroutines") get() = runBlocking { getDefaultList() }
        set(filter) = setFilterPreference(filter, R.string.p_default_list)

    @Deprecated("use coroutines")
    val startupFilter: Filter
        get() = runBlocking { getStartupFilter() }

    fun setBadgeFilter(filter: Filter) = setFilterPreference(filter, R.string.p_badge_list)

    suspend fun getBadgeFilter() = getFilterFromPreference(R.string.p_badge_list)

    suspend fun getDefaultList() =
            getFilterFromPreference(preferences.getStringValue(R.string.p_default_list), null)
                    ?.takeIf { it.isWritable }
                    ?: getAnyList()

    private suspend fun getLastViewedFilter() = getFilterFromPreference(R.string.p_last_viewed_list)

    suspend fun getDefaultOpenFilter() = getFilterFromPreference(R.string.p_default_open_filter)

    fun setDefaultOpenFilter(filter: Filter) =
            setFilterPreference(filter, R.string.p_default_open_filter)

    suspend fun getStartupFilter(): Filter =
            if (preferences.getBoolean(R.string.p_open_last_viewed_list, true)) {
                getLastViewedFilter()
            } else {
                getDefaultOpenFilter()
            }

    @Deprecated("use coroutines")
    fun getFilterFromPreferenceBlocking(prefString: String?) = runBlocking {
        getFilterFromPreference(prefString)
    }

    suspend fun getFilterFromPreference(resId: Int): Filter =
            getFilterFromPreference(preferences.getStringValue(resId))

    suspend fun getFilterFromPreference(prefString: String?): Filter =
            getFilterFromPreference(prefString, getMyTasksFilter(context.resources))!!

    private suspend fun getAnyList(): Filter {
        val filter = caldavDao.getGoogleTaskLists().getOrNull(0)?.let(::GtasksFilter)
                ?: caldavDao.getCalendars().filterNot { it.access == ACCESS_READ_ONLY }.getOrElse(0) { caldavDao.getLocalList(context) }.let(::CaldavFilter)
        defaultList = filter
        return filter
    }

    private suspend fun getFilterFromPreference(preferenceValue: String?, def: Filter?) = try {
        preferenceValue?.let { loadFilter(it) } ?: def
    } catch (e: Exception) {
        Timber.e(e)
        def
    }

    private suspend fun loadFilter(preferenceValue: String): Filter? {
        val split = preferenceValue.split(":")
        return when (split[0].toInt()) {
            TYPE_FILTER -> getBuiltInFilter(split[1].toInt())
            TYPE_CUSTOM_FILTER -> filterDao.getById(split[1].toLong())?.let(::CustomFilter)
            TYPE_TAG -> {
                val tag = tagDataDao.getByUuid(split[1])
                if (tag == null || isNullOrEmpty(tag.name)) null else TagFilter(tag)
            }
            // TODO: convert filters from old ID to uuid
            TYPE_GOOGLE_TASKS -> caldavDao.getCalendarByUuid(split[1])?.let { GtasksFilter(it) }
            TYPE_CALDAV -> caldavDao.getCalendarByUuid(split[1])?.let { CaldavFilter(it) }
            TYPE_LOCATION -> locationDao.getPlace(split[1])?.let { PlaceFilter(it) }
            else -> null
        }
    }

    private fun setFilterPreference(filter: Filter, prefId: Int) =
            getFilterPreferenceValue(filter).let { preferences.setString(prefId, it) }

    fun getFilterPreferenceValue(filter: Filter): String? = when (val filterType = getFilterType(filter)) {
        TYPE_FILTER -> getFilterPreference(filterType, getBuiltInFilterId(filter))
        TYPE_CUSTOM_FILTER -> getFilterPreference(filterType, (filter as CustomFilter).id)
        TYPE_TAG -> getFilterPreference(filterType, (filter as TagFilter).uuid)
        TYPE_GOOGLE_TASKS -> getFilterPreference(filterType, (filter as GtasksFilter).remoteId)
        TYPE_CALDAV -> getFilterPreference(filterType, (filter as CaldavFilter).uuid)
        TYPE_LOCATION -> getFilterPreference(filterType, (filter as PlaceFilter).uid)
        else -> null
    }

    private fun <T> getFilterPreference(type: Int, value: T) = "$type:$value"

    private fun getFilterType(filter: Filter) = when (filter) {
        is TagFilter -> TYPE_TAG
        is GtasksFilter -> TYPE_GOOGLE_TASKS
        is CustomFilter -> TYPE_CUSTOM_FILTER
        is CaldavFilter -> TYPE_CALDAV
        is PlaceFilter -> TYPE_LOCATION
        else -> TYPE_FILTER
    }

    private fun getBuiltInFilter(id: Int): Filter = when (id) {
        FILTER_TODAY -> BuiltInFilterExposer.getTodayFilter(context.resources)
        FILTER_RECENTLY_MODIFIED -> BuiltInFilterExposer.getRecentlyModifiedFilter(context.resources)
        FILTER_SNOOZED -> BuiltInFilterExposer.getSnoozedFilter(context.resources)
        FILTER_NOTIFICATIONS -> BuiltInFilterExposer.getNotificationsFilter(context)
        else -> getMyTasksFilter(context.resources)
    }

    private fun getBuiltInFilterId(filter: Filter) = with(filter) {
        when {
            isToday() -> FILTER_TODAY
            isRecentlyModified() -> FILTER_RECENTLY_MODIFIED
            isSnoozed() -> FILTER_SNOOZED
            isNotifications() -> FILTER_NOTIFICATIONS
            else -> FILTER_MY_TASKS
        }
    }

    suspend fun getList(task: Task): Filter {
        var originalList: Filter? = null
        if (task.isNew) {
            if (task.hasTransitory(GoogleTask.KEY)) {
                val listId = task.getTransitory<String>(GoogleTask.KEY)!!
                val googleTaskList = googleTaskListDao.getByRemoteId(listId)
                if (googleTaskList != null) {
                    originalList = GtasksFilter(googleTaskList)
                }
            } else if (task.hasTransitory(CaldavTask.KEY)) {
                val caldav = caldavDao.getCalendarByUuid(task.getTransitory(CaldavTask.KEY)!!)
                    ?.takeIf { it.access != ACCESS_READ_ONLY }
                if (caldav != null) {
                    originalList = CaldavFilter(caldav)
                }
            }
        } else {
            val googleTask = googleTaskDao.getByTaskId(task.id)
            val caldavTask = caldavDao.getTask(task.id)
            if (googleTask != null) {
                val googleTaskList = googleTaskListDao.getByRemoteId(googleTask.calendar!!)
                if (googleTaskList != null) {
                    originalList = GtasksFilter(googleTaskList)
                }
            } else if (caldavTask != null) {
                val calendarByUuid = caldavDao.getCalendarByUuid(caldavTask.calendar!!)
                if (calendarByUuid != null) {
                    originalList = CaldavFilter(calendarByUuid)
                }
            }
        }
        return originalList ?: getDefaultList()
    }

    private fun Filter.isToday() =
        BuiltInFilterExposer.isTodayFilter(context, this)

    private fun Filter.isRecentlyModified() =
        BuiltInFilterExposer.isRecentlyModifiedFilter(context, this)

    private fun Filter.isSnoozed() =
        BuiltInFilterExposer.isSnoozedFilter(context, this)

    private fun Filter.isNotifications() =
        BuiltInFilterExposer.isNotificationsFilter(context, this)

    companion object {
        private const val TYPE_FILTER = 0
        private const val TYPE_CUSTOM_FILTER = 1
        private const val TYPE_TAG = 2
        const val TYPE_GOOGLE_TASKS = 3
        private const val TYPE_CALDAV = 4
        private const val TYPE_LOCATION = 5
        private const val FILTER_MY_TASKS = 0
        private const val FILTER_TODAY = 1
        @Suppress("unused") private const val FILTER_UNCATEGORIZED = 2
        private const val FILTER_RECENTLY_MODIFIED = 3
        private const val FILTER_SNOOZED = 4
        private const val FILTER_NOTIFICATIONS = 5
    }
}