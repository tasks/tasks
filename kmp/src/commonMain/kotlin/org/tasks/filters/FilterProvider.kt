package org.tasks.filters

import org.jetbrains.compose.resources.getString
import org.tasks.data.GoogleTaskFilters
import org.tasks.data.LocationFilters
import org.tasks.data.NO_ORDER
import org.tasks.data.TagFilters
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.setupLocalAccount
import org.tasks.data.toGtasksFilter
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType
import org.tasks.kmp.IS_DEBUG
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.data.toLocationFilter
import org.tasks.data.toTagFilter
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.TasksPreferences.Companion.collapseDebug
import org.tasks.preferences.TasksPreferences.Companion.collapseFilters
import org.tasks.preferences.TasksPreferences.Companion.collapsePlaces
import org.tasks.preferences.TasksPreferences.Companion.collapseTags
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.drawer_filters
import tasks.kmp.generated.resources.drawer_local_lists
import tasks.kmp.generated.resources.drawer_places
import tasks.kmp.generated.resources.drawer_tags

class FilterProvider(
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val googleTaskListDao: GoogleTaskListDao,
    private val caldavDao: CaldavDao,
    private val configuration: DrawerConfiguration,
    private val locationDao: LocationDao,
    private val taskDao: TaskDao,
    private val tasksPreferences: TasksPreferences,
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
            getAllFilters(showBuiltIn = false, showCreate = true)

    private suspend fun getDebugFilters(): List<FilterListItem> =
            if (IS_DEBUG) {
                val collapsed = tasksPreferences.get(collapseDebug, false)
                listOf(
                    NavigationDrawerSubheader(
                        "Debug",
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        collapseDebug.name,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(listOf(
                            DebugFilters.getNoListFilter(),
                            DebugFilters.getNoTitleFilter(),
                            DebugFilters.getMissingListFilter(),
                            DebugFilters.getMissingAccountFilter(),
                            DebugFilters.getNoCreateDateFilter(),
                            DebugFilters.getNoModificationDateFilter(),
                            DebugFilters.getDeleted()
                        ))

            } else {
                emptyList()
            }

    private suspend fun addFilters(
        showCreate: Boolean,
        showBuiltIn: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            if (!configuration.filtersEnabled) {
                emptyList()
            } else {
                val collapsed = !forceExpand && tasksPreferences.get(collapseFilters, false)
                listOf(
                    NavigationDrawerSubheader(
                        getString(Res.string.drawer_filters),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        collapseFilters.name,
                        if (showCreate) REQUEST_NEW_FILTER else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plusAllIf(showBuiltIn) {
                            builtInFilters()
                        }
                        .plus(filterDao.getFilters().map(::CustomFilter).sort())
            }

    private suspend fun addTags(
        showCreate: Boolean,
        hideUnused: Boolean,
        forceExpand: Boolean,
    ): List<FilterListItem> =
            if (!configuration.tagsEnabled) {
                emptyList()
            } else {
                val collapsed = !forceExpand && tasksPreferences.get(collapseTags, false)
                listOf(
                    NavigationDrawerSubheader(
                        getString(Res.string.drawer_tags),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        collapseTags.name,
                        if (showCreate) REQUEST_NEW_TAGS else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(tagDataDao.getTagFilters()
                                    .filterIf(hideUnused && configuration.hideUnusedTags) {
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
            if (!configuration.placesEnabled) {
                emptyList()
            } else {
                val collapsed = !forceExpand && tasksPreferences.get(collapsePlaces, false)
                listOf(
                    NavigationDrawerSubheader(
                        getString(Res.string.drawer_places),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        collapsePlaces.name,
                        if (showCreate) REQUEST_NEW_PLACE else 0,
                    )
                )
                        .apply { if (collapsed) return this }
                        .plus(locationDao.getPlaceFilters()
                                    .filterIf(hideUnused && configuration.hideUnusedPlaces) {
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
                arrayListOf(MyTasksFilter.create())
            } else {
                ArrayList<FilterListItem>()
            }
                    .asSequence()
                    .plus(addFilters(showCreate, showBuiltIn, forceExpand))
                    .plus(addTags(showCreate, hideUnused, forceExpand))
                    .plus(addPlaces(showCreate, hideUnused, forceExpand))
                    .plus(caldavFilters(showCreate, forceExpand))
                    .toList()
                    .plusAllIf(IS_DEBUG) { getDebugFilters() }

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
                account.id.toString(),
                if (showCreate) REQUEST_NEW_LIST else 0,
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
                    .ifEmpty { listOf(caldavDao.setupLocalAccount()) }
                    .filter { it.accountType != TYPE_LOCAL || configuration.localListsEnabled }
                .flatMap {
                    if (it.isGoogleTasks) {
                        googleTaskFilter(it, showCreate, forceExpand)
                    } else {
                        caldavFilter(
                            it,
                            showCreate && it.accountType != TYPE_OPENTASKS,
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
                    getString(Res.string.drawer_local_lists)
                } else {
                    account.name
                },
                account.error?.isNotBlank() ?: false,
                collapsed,
                when {
                    account.isTasksOrg -> SubheaderType.TASKS
                    else -> SubheaderType.CALDAV
                },
                account.id.toString(),
                if (showCreate) REQUEST_NEW_LIST else 0,
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

    private suspend fun builtInFilters(): List<Filter> {
        val filters: MutableList<Filter> = ArrayList()
        if (configuration.todayFilter) {
            filters.add(TodayFilter.create())
        }
        if (configuration.recentlyModifiedFilter) {
            filters.add(RecentlyModifiedFilter.create())
        }
        if (taskDao.snoozedReminders() > 0) {
            filters.add(SnoozedFilter.create())
        }
        if (taskDao.activeTimers() > 0) {
            filters.add(TimerFilter.create())
        }
        if (taskDao.hasNotifications() > 0) {
            filters.add(NotificationsFilter.create())
        }
        return filters
    }

    companion object {
        const val REQUEST_NEW_LIST = 10100
        const val REQUEST_NEW_TAGS = 10101
        const val REQUEST_NEW_PLACE = 10104
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
