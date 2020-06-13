package org.tasks.preferences

import android.content.Context
import com.todoroo.astrid.api.*
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.core.BuiltInFilterExposer.getMyTasksFilter
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.*
import org.tasks.filters.PlaceFilter
import org.tasks.injection.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class DefaultFilterProvider @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val filterDao: FilterDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao,
        private val locationDao: LocationDao) {

    var dashclockFilter: Filter
        get() = getFilterFromPreference(R.string.p_dashclock_filter)
        set(filter) = setFilterPreference(filter, R.string.p_dashclock_filter)

    var badgeFilter: Filter
        get() = getFilterFromPreference(R.string.p_badge_list)
        set(filter) = setFilterPreference(filter, R.string.p_badge_list)

    var defaultOpenFilter: Filter
        get() = getFilterFromPreference(R.string.p_default_open_filter)
        set(filter) = setFilterPreference(filter, R.string.p_default_open_filter)

    var lastViewedFilter: Filter
        get() = getFilterFromPreference(R.string.p_last_viewed_list)
        set(filter) = setFilterPreference(filter, R.string.p_last_viewed_list)

    var defaultList: Filter
        get() = getFilterFromPreference(preferences.getStringValue(R.string.p_default_list), null) ?: getAnyList()
        set(filter) = setFilterPreference(filter, R.string.p_default_list)

    val startupFilter: Filter
        get() {
            return if (preferences.getBoolean(R.string.p_open_last_viewed_list, true)) {
                lastViewedFilter
            } else {
                defaultOpenFilter
            }
        }

    fun getFilterFromPreference(resId: Int): Filter =
            getFilterFromPreference(preferences.getStringValue(resId))

    fun getFilterFromPreference(prefString: String?): Filter =
            getFilterFromPreference(prefString, getMyTasksFilter(context.resources))!!

    private fun getAnyList(): Filter {
        val filter = googleTaskListDao.getAllLists().getOrNull(0)?.let(::GtasksFilter)
                ?: caldavDao.getCalendars().getOrElse(0) { caldavDao.getLocalList(context) }.let(::CaldavFilter)
        defaultList = filter
        return filter
    }

    private fun getFilterFromPreference(preferenceValue: String?, def: Filter?) = try {
        preferenceValue?.let(this::loadFilter) ?: def
    } catch (e: Exception) {
        Timber.e(e)
        def
    }

    private fun loadFilter(preferenceValue: String): Filter? {
        val split = preferenceValue.split(":")
        return when (split[0].toInt()) {
            TYPE_FILTER -> getBuiltInFilter(split[1].toInt())
            TYPE_CUSTOM_FILTER -> filterDao.getById(split[1].toLong())?.let(::CustomFilter)
            TYPE_TAG -> {
                val tag = tagDataDao.getByUuid(split[1])
                if (tag == null || isNullOrEmpty(tag.name)) null else TagFilter(tag)
            }
            TYPE_GOOGLE_TASKS -> googleTaskListDao.getById(split[1].toLong())?.let { GtasksFilter(it) }
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
        TYPE_GOOGLE_TASKS -> getFilterPreference(filterType, (filter as GtasksFilter).storeId)
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
        else -> getMyTasksFilter(context.resources)
    }

    private fun getBuiltInFilterId(filter: Filter): Int {
        if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
            return FILTER_TODAY
        } else if (BuiltInFilterExposer.isRecentlyModifiedFilter(context, filter)) {
            return FILTER_RECENTLY_MODIFIED
        }
        return FILTER_MY_TASKS
    }

    companion object {
        private const val TYPE_FILTER = 0
        private const val TYPE_CUSTOM_FILTER = 1
        private const val TYPE_TAG = 2
        private const val TYPE_GOOGLE_TASKS = 3
        private const val TYPE_CALDAV = 4
        private const val TYPE_LOCATION = 5
        private const val FILTER_MY_TASKS = 0
        private const val FILTER_TODAY = 1
        @Suppress("unused") private const val FILTER_UNCATEGORIZED = 2
        private const val FILTER_RECENTLY_MODIFIED = 3
    }
}