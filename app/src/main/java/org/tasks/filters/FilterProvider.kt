package org.tasks.filters

import android.content.Context
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.core.BuiltInFilterExposer
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.GoogleTaskFilters
import org.tasks.data.LocationFilters
import org.tasks.data.NO_ORDER
import org.tasks.data.TagFilters
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.setupLocalAccount
import org.tasks.data.toGtasksFilter
import org.tasks.data.toLocationFilter
import org.tasks.data.toTagFilter
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType
import org.tasks.preferences.Preferences
import javax.inject.Inject

class FilterProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val builtInFilterExposer: BuiltInFilterExposer,
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val googleTaskListDao: GoogleTaskListDao,
    private val caldavDao: CaldavDao,
    private val preferences: Preferences,
    private val locationDao: LocationDao
) {
    suspend fun listPickerItems(): List<FilterListItem> =
            caldavFilters(showCreate = false, forceExpand = false)

    suspend fun drawerItems(): List<FilterListItem> =
        getAllFilters(showCreate = true, hideUnused = true)


    suspend fun allLists(): List<Filter> =
        caldavFilters(showCreate = false, forceExpand = true)
            .filterIsInstance<Filter>()

    suspend fun allFilters(): List<Filter> =
        getAllFilters(showCreate = false, hideUnused = false, forceExpand = true)
            .filterIsInstance<Filter>()

    suspend fun filterPickerItems(): List<FilterListItem> =
            getAllFilters(showCreate = false)

    suspend fun drawerCustomizationItems(): List<FilterListItem> =
            getAllFilters(showBuiltIn = false)

    private fun getDebugFilters(): List<FilterListItem> =
            if (BuildConfig.DEBUG) {
                val collapsed = preferences.getBoolean(R.string.p_collapse_debug, false)
                listOf(
                    NavigationDrawerSubheader(
                        context.getString(R.string.debug),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_debug.toLong(),
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(listOf(
                                BuiltInFilterExposer.getNoListFilter(),
                                BuiltInFilterExposer.getNoTitleFilter(),
                                BuiltInFilterExposer.getMissingListFilter(),
                                BuiltInFilterExposer.getMissingAccountFilter(),
                                BuiltInFilterExposer.getNoCreateDateFilter(),
                                BuiltInFilterExposer.getNoModificationDateFilter(),
                                BuiltInFilterExposer.getDeleted()
                        ))

            } else {
                emptyList()
            }

    private suspend fun addFilters(
        showCreate: Boolean,
        showBuiltIn: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_filters_enabled, true)) {
                emptyList()
            } else {
                val collapsed = !forceExpand && preferences.getBoolean(R.string.p_collapse_filters, false)
                listOf(
                    NavigationDrawerSubheader(
                        context.getString(R.string.filters),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_filters.toLong(),
                        if (showCreate) REQUEST_NEW_FILTER else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plusAllIf(showBuiltIn) {
                            builtInFilterExposer.filters()
                        }
                        .plus(filterDao.getFilters().map(::CustomFilter).sort())
            }

    private suspend fun addTags(
        showCreate: Boolean,
        hideUnused: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_tags_enabled, true)) {
                emptyList()
            } else {
                val collapsed = !forceExpand && preferences.getBoolean(R.string.p_collapse_tags, false)
                listOf(
                    NavigationDrawerSubheader(
                        context.getString(R.string.tags),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_tags.toLong(),
                        if (showCreate) MainActivity.REQUEST_NEW_TAGS else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(tagDataDao.getTagFilters()
                                    .filterIf(hideUnused && preferences.getBoolean(R.string.p_tags_hide_unused, false)) {
                                        it.count > 0
                                    }
                                    .map(TagFilters::toTagFilter)
                                    .sort())
            }

    private suspend fun addPlaces(
        showCreate: Boolean,
        hideUnused: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_places_enabled, true)) {
                emptyList()
            } else {
                val collapsed = !forceExpand && preferences.getBoolean(R.string.p_collapse_locations, false)
                listOf(
                    NavigationDrawerSubheader(
                        context.getString(R.string.places),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_locations.toLong(),
                        if (showCreate) MainActivity.REQUEST_NEW_PLACE else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(locationDao.getPlaceFilters()
                                    .filterIf(hideUnused && preferences.getBoolean(R.string.p_places_hide_unused, false)) {
                                        it.count > 0
                                    }
                                    .map(LocationFilters::toLocationFilter)
                                    .sort())
            }

    private suspend fun getAllFilters(
        showCreate: Boolean = true,
        showBuiltIn: Boolean = true,
        hideUnused: Boolean = false,
        forceExpand: Boolean = false,
    ): List<FilterListItem> =
            if (showBuiltIn) {
                arrayListOf(builtInFilterExposer.myTasksFilter)
            } else {
                ArrayList<FilterListItem>()
            }
                    .asSequence()
                    .plus(addFilters(showCreate, showBuiltIn, forceExpand))
                    .plus(addTags(showCreate, hideUnused, forceExpand))
                    .plus(addPlaces(showCreate, hideUnused, forceExpand))
                    .plus(caldavFilters(showCreate, forceExpand))
                    .toList()
                    .plusAllIf(BuildConfig.DEBUG) { getDebugFilters() }

    private suspend fun googleTaskFilter(
        account: CaldavAccount,
        showCreate: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> {
        val collapsed = !forceExpand && account.isCollapsed
        return listOf(
            NavigationDrawerSubheader(
                account.username,
                account.error?.isNotBlank() ?: false,
                collapsed,
                SubheaderType.GOOGLE_TASKS,
                account.id,
                if (showCreate) MainActivity.REQUEST_NEW_LIST else 0,
            )
        )
            .apply { if (collapsed) return this }
            .plus(
                googleTaskListDao
                    .getGoogleTaskFilters(account.username!!)
                    .map(GoogleTaskFilters::toGtasksFilter)
                    .sort()
            )
    }

    private suspend fun caldavFilters(
        showCreate: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            caldavDao.getAccounts()
                    .ifEmpty { listOf(caldavDao.setupLocalAccount(context)) }
                    .filter { it.accountType != TYPE_LOCAL || preferences.getBoolean(R.string.p_lists_enabled, true) }
                .flatMap {
                    if (it.isGoogleTasks) {
                        googleTaskFilter(it, showCreate, forceExpand)
                    } else {
                        caldavFilter(
                            it,
                            showCreate && it.accountType != TYPE_OPENTASKS && it.accountType != TYPE_ETESYNC,
                            forceExpand,
                        )
                    }
                }

    private suspend fun caldavFilter(
        account: CaldavAccount,
        showCreate: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> {
        val collapsed = !forceExpand && account.isCollapsed
        return listOf(
            NavigationDrawerSubheader(
                if (account.accountType == TYPE_LOCAL) {
                    context.getString(R.string.local_lists)
                } else {
                    account.name
                },
                account.error?.isNotBlank() ?: false,
                collapsed,
                when {
                    account.isTasksOrg -> SubheaderType.TASKS
                    account.isEteSyncAccount -> SubheaderType.ETESYNC
                    else -> SubheaderType.CALDAV
                },
                account.id,
                if (showCreate) MainActivity.REQUEST_NEW_LIST else 0,
            )
        )
            .apply { if (collapsed) return this }
            .plus(caldavDao
                .getCaldavFilters(account.uuid!!)
                .map {
                    CaldavFilter(
                        calendar = it.caldavCalendar,
                        principals = it.principals,
                        count = it.count,
                    )
                }
                .sort())
    }

    companion object {
        const val REQUEST_NEW_FILTER = 101015
        private val COMPARATOR = Comparator<Filter> { f1, f2 ->
            when {
                f1.order == NO_ORDER && f2.order == NO_ORDER ->
                    AlphanumComparator.FILTER.compare(f1, f2)
                f1.order == NO_ORDER -> 1
                f2.order == NO_ORDER -> -1
                f1.order < f2.order -> -1
                f1.order > f2.order -> 1
                else -> AlphanumComparator.FILTER.compare(f1, f2)
            }
        }

        private fun List<Filter>.sort(): List<Filter> =
                if (all { it.order == NO_ORDER }) {
                    sortedWith(AlphanumComparator.FILTER)
                } else {
                    sortedWith(COMPARATOR)
                }

        private suspend fun <T> Collection<T>.plusAllIf(predicate: Boolean, item: suspend () -> Iterable<T>): List<T> =
                plus(if (predicate) item() else emptyList())

        private fun <T> Iterable<T>.filterIf(predicate: Boolean, predicate2: (T) -> Boolean): Iterable<T> =
                if (predicate) filter(predicate2) else this
    }
}
