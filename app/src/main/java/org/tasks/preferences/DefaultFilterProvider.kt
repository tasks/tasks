package org.tasks.preferences

import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.GoogleTask
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.getLocalList
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.Filter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.NotificationsFilter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.filters.SnoozedFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TodayFilter
import timber.log.Timber
import javax.inject.Inject

class DefaultFilterProvider @Inject constructor(
    private val preferences: Preferences,
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val caldavDao: CaldavDao,
    private val locationDao: LocationDao,
) {
    var dashclockFilter: Filter
        @Deprecated("use coroutines") get() = runBlocking { getFilterFromPreference(R.string.p_dashclock_filter) }
        set(filter) = setFilterPreference(filter, R.string.p_dashclock_filter)

    var defaultList: CaldavFilter
        @Deprecated("use coroutines") get() = runBlocking { getDefaultList() }
        set(filter) = setFilterPreference(filter, R.string.p_default_list)

    @Deprecated("use coroutines")
    val startupFilter: Filter
        get() = runBlocking { getStartupFilter() }

    fun setBadgeFilter(filter: Filter) = setFilterPreference(filter, R.string.p_badge_list)

    suspend fun getBadgeFilter() = getFilterFromPreference(R.string.p_badge_list)

    suspend fun getDefaultList(): CaldavFilter =
            getFilterFromPreference(preferences.getStringValue(R.string.p_default_list), null)
                ?.let { it as? CaldavFilter }
                ?.takeIf { it.isWritable }
                ?: getAnyList()

    fun setLastViewedFilter(filter: Filter) = setFilterPreference(filter, R.string.p_last_viewed_list)

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
        getFilterFromPreference(prefString, MyTasksFilter.create())!!

    private suspend fun getAnyList(): CaldavFilter {
        val filter = caldavDao
            .getCalendars()
            .filterNot { it.access == ACCESS_READ_ONLY }
            .getOrNull(0)
            ?.let { list ->
                list.account
                    ?.let { caldavDao.getAccountByUuid(it) }
                    ?.let { account -> CaldavFilter(calendar = list, account = account) }
            }
            ?: caldavDao.getLocalList().let { list ->
                CaldavFilter(
                    calendar = list,
                    account = caldavDao.getAccountByUuid(list.account!!)!!
                )
            }
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
            TYPE_GOOGLE_TASKS, // TODO: convert filters from old ID to uuid?
            TYPE_CALDAV ->
                caldavDao.getCalendarByUuid(split[1])
                    ?.let { CaldavFilter(it, caldavDao.getAccountByUuid(it.account!!)!!) }
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
        TYPE_GOOGLE_TASKS,
        TYPE_CALDAV -> getFilterPreference(filterType, (filter as CaldavFilter).uuid)
        TYPE_LOCATION -> getFilterPreference(filterType, (filter as PlaceFilter).uid)
        else -> null
    }

    private fun <T> getFilterPreference(type: Int, value: T) = "$type:$value"

    private fun getFilterType(filter: Filter) = when (filter) {
        is TagFilter -> TYPE_TAG
        is CustomFilter -> TYPE_CUSTOM_FILTER
        is CaldavFilter -> TYPE_CALDAV
        is PlaceFilter -> TYPE_LOCATION
        else -> TYPE_FILTER
    }

    private suspend fun getBuiltInFilter(id: Int): Filter = when (id) {
        FILTER_TODAY -> TodayFilter.create()
        FILTER_RECENTLY_MODIFIED -> RecentlyModifiedFilter.create()
        FILTER_SNOOZED -> SnoozedFilter.create()
        FILTER_NOTIFICATIONS -> NotificationsFilter.create()
        else -> MyTasksFilter.create()
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

    suspend fun getList(task: Task): CaldavFilter {
        var originalList: CaldavFilter? = null
        if (task.isNew) {
            if (task.hasTransitory(GoogleTask.KEY)) {
                val listId = task.getTransitory<String>(GoogleTask.KEY)!!
                val googleTaskList = caldavDao.getCalendarByUuid(listId)
                if (googleTaskList != null) {
                    val account = caldavDao.getAccountByUuid(googleTaskList.account!!)!!
                    originalList = CaldavFilter(calendar = googleTaskList, account = account)
                }
            } else if (task.hasTransitory(CaldavTask.KEY)) {
                val caldav = caldavDao.getCalendarByUuid(task.getTransitory(CaldavTask.KEY)!!)
                    ?.takeIf { it.access != ACCESS_READ_ONLY }
                if (caldav != null) {
                    val account = caldavDao.getAccountByUuid(caldav.account!!)!!
                    originalList = CaldavFilter(calendar = caldav, account = account)
                }
            }
        } else {
            val caldavTask = caldavDao.getTask(task.id)
            val calendar = caldavTask?.calendar?.let { caldavDao.getCalendarByUuid(it) }
            originalList = calendar
                ?.account
                ?.let { caldavDao.getAccountByUuid(it) }
                ?.let { CaldavFilter(calendar = calendar, account = it) }
        }
        return originalList ?: getDefaultList()
    }

    private fun Filter.isToday() = this is TodayFilter

    private fun Filter.isRecentlyModified() = this is RecentlyModifiedFilter

    private fun Filter.isSnoozed() = this is SnoozedFilter

    private fun Filter.isNotifications() = this is NotificationsFilter

    companion object {
        private const val TYPE_FILTER = 0
        private const val TYPE_CUSTOM_FILTER = 1
        private const val TYPE_TAG = 2
        @Deprecated("use TYPE_CALDAV") const val TYPE_GOOGLE_TASKS = 3
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