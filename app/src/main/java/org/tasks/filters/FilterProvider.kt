package org.tasks.filters

import android.content.Context
import android.content.Intent
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.activities.TagSettingsActivity
import org.tasks.billing.Inventory
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.*
import org.tasks.data.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType
import org.tasks.injection.ForApplication
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.HelpAndFeedback
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.ui.NavigationDrawerFragment
import javax.inject.Inject

class FilterProvider @Inject constructor(
        @param:ForApplication private val context: Context,
        private val inventory: Inventory,
        private val builtInFilterExposer: BuiltInFilterExposer,
        private val taskDao: TaskDao,
        private val filterDao: FilterDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao,
        private val preferences: Preferences,
        private val locationDao: LocationDao) {

    val listPickerItems: List<FilterListItem>
        get() {
            AndroidUtilities.assertNotMainThread()
            return googleTaskFilters(false).plus(caldavFilters(false))
        }

    val navDrawerItems: List<FilterListItem>
        get() {
            AndroidUtilities.assertNotMainThread()
            return arrayListOf(builtInFilterExposer.myTasksFilter)
                    .plus(getAllFilters(true))
                    .plus(navDrawerFooter)
        }

    val filterPickerItems: List<FilterListItem>
        get() {
            AndroidUtilities.assertNotMainThread()
            return arrayListOf(builtInFilterExposer.myTasksFilter)
                    .plus(getAllFilters(false))
        }

    private fun addFilters(showCreate: Boolean): List<FilterListItem> =
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
                        .plus(builtInFilterExposer.filters)
                        .plusIf(taskDao.activeTimers() > 0) { TimerPlugin.createFilter(context) }
                        .plus(filterDao.getFilters().map(::CustomFilter).sortedWith(AlphanumComparator.FILTER))
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.add_filter),
                                    R.drawable.ic_outline_add_24px,
                                    NavigationDrawerFragment.REQUEST_NEW_FILTER)
                        }
            }

    private fun addTags(showCreate: Boolean): List<FilterListItem> =
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
                                    .sortedWith(AlphanumComparator.FILTER))
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.new_tag),
                                    R.drawable.ic_outline_add_24px,
                                    Intent(context, TagSettingsActivity::class.java),
                                    NavigationDrawerFragment.REQUEST_NEW_LIST)
                        }
            }

    private fun addPlaces(showCreate: Boolean): List<FilterListItem> =
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
                                    .sortedWith(AlphanumComparator.FILTER))
                        .plusIf(showCreate) {
                            NavigationDrawerAction(
                                    context.getString(R.string.add_place),
                                    R.drawable.ic_outline_add_24px,
                                    Intent(context, LocationPickerActivity::class.java),
                                    NavigationDrawerFragment.REQUEST_NEW_PLACE)
                        }
            }

    private fun getAllFilters(showCreate: Boolean): List<FilterListItem> =
            addFilters(showCreate)
                    .plus(addTags(showCreate))
                    .plus(addPlaces(showCreate))
                    .plus(googleTaskFilters(showCreate))
                    .plus(caldavFilters(showCreate))

    private val navDrawerFooter: List<FilterListItem>
        get() = listOf(NavigationDrawerSeparator())
                .plusIf(BuildConfig.FLAVOR == "generic") {
                    NavigationDrawerAction(
                            context.getString(R.string.TLA_menu_donate),
                            R.drawable.ic_outline_attach_money_24px,
                            NavigationDrawerFragment.REQUEST_DONATE)
                }
                .plusIf(!inventory.hasPro()) {
                    NavigationDrawerAction(
                            context.getString(R.string.name_your_price),
                            R.drawable.ic_outline_attach_money_24px,
                            NavigationDrawerFragment.REQUEST_PURCHASE)
                }
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

    private fun googleTaskFilters(showCreate: Boolean = true): List<FilterListItem> =
            googleTaskListDao
                    .getAccounts()
                    .flatMap {
                        listOf(
                                NavigationDrawerSubheader(
                                        it.account,
                                        it.error?.isNotBlank() ?: false,
                                        it.isCollapsed,
                                        SubheaderType.GOOGLE_TASKS,
                                        it.id))
                                .plusAllIf(!it.isCollapsed) {
                                    googleTaskListDao
                                            .getGoogleTaskFilters(it.account!!)
                                            .map(GoogleTaskFilters::toGtasksFilter)
                                            .sortedWith(AlphanumComparator.FILTER)
                                }
                                .plusIf(showCreate && !it.isCollapsed) {
                                    NavigationDrawerAction(
                                            context.getString(R.string.new_list),
                                            R.drawable.ic_outline_add_24px,
                                            Intent(context, GoogleTaskListSettingsActivity::class.java)
                                                    .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, it),
                                            NavigationDrawerFragment.REQUEST_NEW_LIST)
                                }
                    }

    private fun caldavFilters(showCreate: Boolean = true): List<FilterListItem> =
            caldavDao.getAccounts()
                    .ifEmpty { listOf(caldavDao.setupLocalAccount(context)) }
                    .filter { it.accountType != TYPE_LOCAL || preferences.getBoolean(R.string.p_lists_enabled, true) }
                    .flatMap {
                        listOf(
                                NavigationDrawerSubheader(
                                        if (it.accountType == TYPE_LOCAL) {
                                            context.getString(R.string.lists)
                                        } else {
                                            it.name
                                        },
                                        it.error?.isNotBlank() ?: false,
                                        it.isCollapsed,
                                        SubheaderType.CALDAV,
                                        it.id))
                                .plusAllIf(!it.isCollapsed) {
                                    caldavDao
                                            .getCaldavFilters(it.uuid!!)
                                            .map(CaldavFilters::toCaldavFilter)
                                            .sortedWith(AlphanumComparator.FILTER)
                                }
                                .plusIf(showCreate && !it.isCollapsed) {
                                    NavigationDrawerAction(
                                            context.getString(R.string.new_list),
                                            R.drawable.ic_outline_add_24px,
                                            Intent(context, it.listSettingsClass())
                                                    .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT, it),
                                            NavigationDrawerFragment.REQUEST_NEW_LIST)
                                }
                    }

    companion object {
        private fun <T> Collection<T>.plusAllIf(predicate: Boolean, item: () -> Iterable<T>): List<T> =
                plus(if (predicate) item.invoke() else emptyList())

        private fun <T> Collection<T>.plusIf(predicate: Boolean, item: () -> T): List<T> =
                if (predicate) plus(item.invoke()) else this.toList()

        private fun <T> Iterable<T>.filterIf(predicate: Boolean, predicate2: (T) -> Boolean): List<T> =
                if (predicate) filter(predicate2) else this.toList()
    }
}