package org.tasks.filters

import android.content.Context
import android.content.Intent
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.core.BuiltInFilterExposer
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.activities.NavigationDrawerCustomization
import org.tasks.activities.TagSettingsActivity
import org.tasks.billing.Inventory
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.*
import org.tasks.data.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.ui.NavigationDrawerFragment
import javax.inject.Inject

class FilterProvider @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val inventory: Inventory,
        private val builtInFilterExposer: BuiltInFilterExposer,
        private val filterDao: FilterDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao,
        private val preferences: Preferences,
        private val locationDao: LocationDao) {

    suspend fun listPickerItems(): List<FilterListItem> =
            googleTaskFilters(false).plus(caldavFilters(false))

    suspend fun navDrawerItems(): List<FilterListItem> =
            getAllFilters().plus(navDrawerFooter)

    suspend fun filterPickerItems(): List<FilterListItem> =
            getAllFilters(showCreate = false)

    suspend fun drawerCustomizationItems(): List<FilterListItem> =
            getAllFilters(showBuiltIn = false)

    private fun getDebugFilters(): List<FilterListItem> =
            if (BuildConfig.DEBUG) {
                val collapsed = preferences.getBoolean(R.string.p_collapse_debug, false)
                listOf(NavigationDrawerSubheader(
                        context.getString(R.string.debug),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_debug.toLong()))
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

    private suspend fun addFilters(showCreate: Boolean, showBuiltIn: Boolean): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_filters_enabled, true)) {
                emptyList()
            } else {
                val collapsed = preferences.getBoolean(R.string.p_collapse_filters, false)
                listOf(
                        NavigationDrawerSubheader(
                                context.getString(R.string.filters),
                                false,
                                collapsed,
                                SubheaderType.PREFERENCE,
                                R.string.p_collapse_filters.toLong()))
                        .apply { if (collapsed) return this }
                        .plusAllIf(showBuiltIn) {
                            builtInFilterExposer.filters()
                        }
                        .plus(filterDao.getFilters().map(::CustomFilter).sort())
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.add_filter),
                                    R.drawable.ic_outline_add_24px,
                                    NavigationDrawerFragment.REQUEST_NEW_FILTER)
                        }
            }

    private suspend fun addTags(showCreate: Boolean): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_tags_enabled, true)) {
                emptyList()
            } else {
                val collapsed = preferences.getBoolean(R.string.p_collapse_tags, false)
                listOf(
                        NavigationDrawerSubheader(
                                context.getString(R.string.tags),
                                false,
                                collapsed,
                                SubheaderType.PREFERENCE,
                                R.string.p_collapse_tags.toLong()))
                        .apply { if (collapsed) return this }
                        .plus(tagDataDao.getTagFilters()
                                    .filterIf(preferences.getBoolean(R.string.p_tags_hide_unused, false)) {
                                        it.count > 0
                                    }
                                    .map(TagFilters::toTagFilter)
                                    .sort())
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.new_tag),
                                    R.drawable.ic_outline_add_24px,
                                    Intent(context, TagSettingsActivity::class.java),
                                    NavigationDrawerFragment.REQUEST_NEW_LIST)
                        }
            }

    private suspend fun addPlaces(showCreate: Boolean): List<FilterListItem> =
            if (!preferences.getBoolean(R.string.p_places_enabled, true)) {
                emptyList()
            } else {
                val collapsed = preferences.getBoolean(R.string.p_collapse_locations, false)
                listOf(
                        NavigationDrawerSubheader(
                                context.getString(R.string.places),
                                false,
                                collapsed,
                                SubheaderType.PREFERENCE,
                                R.string.p_collapse_locations.toLong()))
                        .apply { if (collapsed) return this }
                        .plus(locationDao.getPlaceFilters()
                                    .filterIf(preferences.getBoolean(R.string.p_places_hide_unused, false)) {
                                        it.count > 0
                                    }
                                    .map(LocationFilters::toLocationFilter)
                                    .sort())
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.add_place),
                                    R.drawable.ic_outline_add_24px,
                                    Intent(context, LocationPickerActivity::class.java),
                                    NavigationDrawerFragment.REQUEST_NEW_PLACE)
                        }
            }

    private suspend fun getAllFilters(showCreate: Boolean = true, showBuiltIn: Boolean = true): List<FilterListItem> =
            if (showBuiltIn) {
                arrayListOf(builtInFilterExposer.myTasksFilter)
            } else {
                ArrayList<FilterListItem>()
            }
                    .asSequence()
                    .plus(addFilters(showCreate, showBuiltIn))
                    .plus(addTags(showCreate))
                    .plus(addPlaces(showCreate))
                    .plus(googleTaskFilters(showCreate))
                    .plus(caldavFilters(showCreate))
                    .toList()
                    .plusAllIf(BuildConfig.DEBUG) { getDebugFilters() }

    private val navDrawerFooter: List<FilterListItem>
        get() = listOf(NavigationDrawerSeparator())
                .plusIf(BuildConfig.FLAVOR == "generic" && !inventory.hasTasksSubscription) {
                    NavigationDrawerAction(
                            context.getString(R.string.TLA_menu_donate),
                            R.drawable.ic_outline_attach_money_24px,
                            NavigationDrawerFragment.REQUEST_DONATE)
                }
                .plusIf(!inventory.hasPro) {
                    NavigationDrawerAction(
                            context.getString(R.string.name_your_price),
                            R.drawable.ic_outline_attach_money_24px,
                            NavigationDrawerFragment.REQUEST_PURCHASE)
                }
                .plus(NavigationDrawerAction(
                        context.getString(R.string.manage_lists),
                        R.drawable.ic_outline_edit_24px,
                        Intent(context, NavigationDrawerCustomization::class.java),
                        0))
                .plus(NavigationDrawerAction(
                        context.getString(R.string.TLA_menu_settings),
                        R.drawable.ic_outline_settings_24px,
                        Intent(context, MainPreferences::class.java),
                        NavigationDrawerFragment.REQUEST_SETTINGS))
                .plus(NavigationDrawerAction(
                        context.getString(R.string.help_and_feedback),
                        R.drawable.ic_outline_help_outline_24px,
                        Intent(context, HelpAndFeedback::class.java),
                        0))

    private suspend fun googleTaskFilters(showCreate: Boolean = true): List<FilterListItem> =
            googleTaskListDao.getAccounts().flatMap { googleTaskFilter(it, showCreate) }

    private suspend fun googleTaskFilter(account: GoogleTaskAccount, showCreate: Boolean): List<FilterListItem> =
            listOf(
                    NavigationDrawerSubheader(
                            account.account,
                            account.error?.isNotBlank() ?: false,
                            account.isCollapsed,
                            SubheaderType.GOOGLE_TASKS,
                            account.id))
                    .apply { if (account.isCollapsed) return this }
                    .plus(googleTaskListDao
                                .getGoogleTaskFilters(account.account!!)
                                .map(GoogleTaskFilters::toGtasksFilter)
                                .sort())
                    .plusIf(showCreate) {
                        NavigationDrawerAction(
                                context.getString(R.string.new_list),
                                R.drawable.ic_outline_add_24px,
                                Intent(context, GoogleTaskListSettingsActivity::class.java)
                                        .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, account),
                                NavigationDrawerFragment.REQUEST_NEW_LIST)
                    }

    private suspend fun caldavFilters(showCreate: Boolean = true): List<FilterListItem> =
            caldavDao.getAccounts()
                    .ifEmpty { listOf(caldavDao.setupLocalAccount(context)) }
                    .filter { it.accountType != TYPE_LOCAL || preferences.getBoolean(R.string.p_lists_enabled, true) }
                    .flatMap { caldavFilter(it, showCreate && it.accountType != TYPE_OPENTASKS) }

    private suspend fun caldavFilter(account: CaldavAccount, showCreate: Boolean): List<FilterListItem> =
            listOf(
                    NavigationDrawerSubheader(
                            if (account.accountType == TYPE_LOCAL) {
                                context.getString(R.string.local_lists)
                            } else {
                                account.name
                            },
                            account.error?.isNotBlank() ?: false,
                            account.isCollapsed,
                            when {
                                account.isTasksOrg -> SubheaderType.TASKS
                                account.isEteSyncAccount -> SubheaderType.ETESYNC
                                else -> SubheaderType.CALDAV
                            },
                            account.id))
                    .apply { if (account.isCollapsed) return this }
                    .plus(caldavDao
                                .getCaldavFilters(account.uuid!!)
                                .map(CaldavFilters::toCaldavFilter)
                                .sort())
                    .plusIf(showCreate) {
                        NavigationDrawerAction(
                                context.getString(R.string.new_list),
                                R.drawable.ic_outline_add_24px,
                                Intent(context, account.listSettingsClass())
                                        .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT, account),
                                NavigationDrawerFragment.REQUEST_NEW_LIST)
                    }

    companion object {
        private val COMPARATOR = Comparator<Filter> { f1, f2 ->
            when {
                f1.order == NO_ORDER && f2.order == NO_ORDER -> f1.id.compareTo(f2.id)
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

        private fun <T> Iterable<T>.plusIf(predicate: Boolean, item: () -> T): List<T> =
                if (predicate) plus(item()) else toList()

        private fun <T> Iterable<T>.filterIf(predicate: Boolean, predicate2: (T) -> Boolean): List<T> =
                if (predicate) filter(predicate2) else toList()
    }
}