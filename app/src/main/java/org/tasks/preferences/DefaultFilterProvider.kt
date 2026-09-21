package org.tasks.preferences

import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.data.GoogleTask
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.getOrCreateDefaultListFilter
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.MyTasksFilter
import timber.log.Timber
import javax.inject.Inject

class DefaultFilterProvider @Inject constructor(
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
    private val codec: FilterPreferenceCodec,
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

    suspend fun getLastViewedFilter() = getFilterFromPreference(R.string.p_last_viewed_list)

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

    private suspend fun getAnyList(): CaldavFilter =
        caldavDao.getOrCreateDefaultListFilter(null).also { defaultList = it }

    private suspend fun getFilterFromPreference(preferenceValue: String?, def: Filter?) = try {
        preferenceValue?.let { loadFilter(it) } ?: def
    } catch (e: Exception) {
        Timber.e(e)
        def
    }

    private suspend fun loadFilter(preferenceValue: String): Filter? =
            codec.decode(preferenceValue)

    private fun setFilterPreference(filter: Filter, prefId: Int) =
            getFilterPreferenceValue(filter).let { preferences.setString(prefId, it) }

    fun getFilterPreferenceValue(filter: Filter): String? = codec.encode(filter)

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

    companion object {
        const val TYPE_CALDAV = FilterPreferenceCodec.TYPE_CALDAV

        @Suppress("DEPRECATION")
        @Deprecated("use TYPE_CALDAV")
        const val TYPE_GOOGLE_TASKS = FilterPreferenceCodec.TYPE_GOOGLE_TASKS
    }
}