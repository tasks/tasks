package org.tasks.filters

import android.content.Context
import android.content.Intent
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CustomFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.timers.TimerFilterExposer
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.Strings
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
import java.util.*
import javax.inject.Inject

class FilterProvider @Inject constructor(
        @param:ForApplication private val context: Context,
        private val inventory: Inventory,
        private val builtInFilterExposer: BuiltInFilterExposer,
        private val timerFilterExposer: TimerFilterExposer,
        private val filterDao: FilterDao,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao,
        private val preferences: Preferences,
        private val locationDao: LocationDao) {

    val listPickerItems: List<FilterListItem>
        get() {
            AndroidUtilities.assertNotMainThread()
            val items: MutableList<FilterListItem> = ArrayList()
            for ((account, value) in googleTaskFilters) {
                items.addAll(
                        getSubmenu(
                                account.account,
                                !Strings.isNullOrEmpty(account.error),
                                value,
                                true,
                                account.isCollapsed,
                                SubheaderType.GOOGLE_TASKS,
                                account.id))
            }
            for ((account, value) in caldavFilters) {
                items.addAll(
                        getSubmenu(
                                account.name,
                                !Strings.isNullOrEmpty(account.error),
                                value,
                                true,
                                account.isCollapsed,
                                SubheaderType.CALDAV,
                                account.id))
            }
            return items
        }

    private fun addFilters(items: MutableList<FilterListItem>, navigationDrawer: Boolean) {
        if (!preferences.getBoolean(R.string.p_filters_enabled, true)) {
            return
        }
        items.addAll(getSubmenu(R.string.filters, R.string.p_collapse_filters) { filters })
        if (navigationDrawer && !preferences.getBoolean(R.string.p_collapse_filters, false)) {
            items.add(
                    NavigationDrawerAction(
                            context.getString(R.string.add_filter),
                            R.drawable.ic_outline_add_24px,
                            NavigationDrawerFragment.REQUEST_NEW_FILTER))
        }
    }

    private fun addTags(items: MutableList<FilterListItem>, navigationDrawer: Boolean) {
        if (!preferences.getBoolean(R.string.p_tags_enabled, true)) {
            return
        }
        val collapsed = preferences.getBoolean(R.string.p_collapse_tags, false)
        var filters = if (collapsed) emptyList() else tagDataDao.getTagFilters(DateUtilities.now())
        if (preferences.getBoolean(R.string.p_tags_hide_unused, false)) {
            filters = filters.filter { it.count > 0 }
        }
        val tags = filters.map(TagFilters::toTagFilter).sortedWith(AlphanumComparator.FILTER)
        items.addAll(
                getSubmenu(
                        context.getString(R.string.tags),
                        false,
                        tags,
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_tags.toLong()))
        if (navigationDrawer && !collapsed) {
            items.add(
                    NavigationDrawerAction(
                            context.getString(R.string.new_tag),
                            R.drawable.ic_outline_add_24px,
                            Intent(context, TagSettingsActivity::class.java),
                            NavigationDrawerFragment.REQUEST_NEW_LIST))
        }
    }

    private fun addPlaces(items: MutableList<FilterListItem>, navigationDrawer: Boolean) {
        if (!preferences.getBoolean(R.string.p_places_enabled, true)) {
            return
        }
        val collapsed = preferences.getBoolean(R.string.p_collapse_locations, false)
        var filters = if (collapsed) emptyList() else locationDao.getPlaceFilters(DateUtilities.now())
        if (preferences.getBoolean(R.string.p_places_hide_unused, false)) {
            filters = filters.filter { it.count > 0 }
        }
        items.addAll(
                getSubmenu(
                        context.getString(R.string.places),
                        false,
                        filters.map(LocationFilters::toLocationFilter),
                        false,
                        collapsed,
                        SubheaderType.PREFERENCE,
                        R.string.p_collapse_locations.toLong()))
        if (navigationDrawer && !collapsed) {
            items.add(
                    NavigationDrawerAction(
                            context.getString(R.string.add_place),
                            R.drawable.ic_outline_add_24px,
                            Intent(context, LocationPickerActivity::class.java),
                            NavigationDrawerFragment.REQUEST_NEW_PLACE))
        }
    }

    fun getItems(navigationDrawer: Boolean): List<FilterListItem> {
        AndroidUtilities.assertNotMainThread()
        val items: MutableList<FilterListItem> = ArrayList()
        items.add(builtInFilterExposer.myTasksFilter)
        addFilters(items, navigationDrawer)
        addTags(items, navigationDrawer)
        addPlaces(items, navigationDrawer)
        for ((account, value) in googleTaskFilters) {
            items.addAll(
                    getSubmenu(
                            account.account,
                            !Strings.isNullOrEmpty(account.error),
                            value,
                            !navigationDrawer,
                            account.isCollapsed,
                            SubheaderType.GOOGLE_TASKS,
                            account.id))
            if (navigationDrawer && !account.isCollapsed) {
                items.add(
                        NavigationDrawerAction(
                                context.getString(R.string.new_list),
                                R.drawable.ic_outline_add_24px,
                                Intent(context, GoogleTaskListSettingsActivity::class.java)
                                        .putExtra(GoogleTaskListSettingsActivity.EXTRA_ACCOUNT, account),
                                NavigationDrawerFragment.REQUEST_NEW_LIST))
            }
        }
        for ((account, value) in caldavFilters) {
            if (account.accountType == TYPE_LOCAL && !preferences.getBoolean(R.string.p_lists_enabled, true)) {
                continue
            }
            items.addAll(
                    getSubmenu(
                            account.name,
                            !Strings.isNullOrEmpty(account.error),
                            value,
                            !navigationDrawer,
                            account.isCollapsed,
                            SubheaderType.CALDAV,
                            account.id))
            if (navigationDrawer && !account.isCollapsed) {
                items.add(
                        NavigationDrawerAction(
                                context.getString(R.string.new_list),
                                R.drawable.ic_outline_add_24px,
                                Intent(context, account.listSettingsClass())
                                        .putExtra(BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT, account),
                                NavigationDrawerFragment.REQUEST_NEW_LIST))
            }
        }
        if (navigationDrawer) {
            items.add(NavigationDrawerSeparator())
            @Suppress("ConstantConditionIf")
            if (BuildConfig.FLAVOR == "generic") {
                items.add(
                        NavigationDrawerAction(
                                context.getString(R.string.TLA_menu_donate),
                                R.drawable.ic_outline_attach_money_24px,
                                NavigationDrawerFragment.REQUEST_DONATE))
            } else if (!inventory.hasPro()) {
                items.add(
                        NavigationDrawerAction(
                                context.getString(R.string.name_your_price),
                                R.drawable.ic_outline_attach_money_24px,
                                NavigationDrawerFragment.REQUEST_PURCHASE))
            }
            items.add(
                    NavigationDrawerAction(
                            context.getString(R.string.TLA_menu_settings),
                            R.drawable.ic_outline_settings_24px,
                            Intent(context, MainPreferences::class.java),
                            NavigationDrawerFragment.REQUEST_SETTINGS))
            items.add(
                    NavigationDrawerAction(
                            context.getString(R.string.help_and_feedback),
                            R.drawable.ic_outline_help_outline_24px,
                            Intent(context, HelpAndFeedback::class.java),
                            0))
        }
        return items
    }

    private val filters: List<Filter>
        get() {
            val filters = ArrayList(builtInFilterExposer.filters)
            val filter = timerFilterExposer.filters
            if (filter != null) {
                filters.add(filter)
            }
            filters.addAll(filterDao.getFilters()
                    .map(::CustomFilter)
                    .sortedWith(AlphanumComparator.FILTER))
            return filters
        }

    private val googleTaskFilters: Set<Map.Entry<GoogleTaskAccount, List<Filter>>>
        get() {
            val accounts = googleTaskListDao.getAccounts()
            val filters = LinkedHashMap<GoogleTaskAccount, List<Filter>>()
            for (account in accounts) {
                filters[account] = if (account.isCollapsed) {
                    emptyList()
                } else {
                    googleTaskListDao
                            .getGoogleTaskFilters(account.account!!, DateUtilities.now())
                            .map(GoogleTaskFilters::toGtasksFilter)
                            .sortedWith(AlphanumComparator.FILTER)
                }
            }
            return filters.entries
        }

    private val caldavFilters: Set<Map.Entry<CaldavAccount, List<Filter>>>
        get() {
            val accounts = caldavDao.getAccounts().ifEmpty {
                listOf(caldavDao.setupLocalAccount(context))
            }
            val filters = LinkedHashMap<CaldavAccount, List<Filter>>()
            for (account in accounts) {
                if (account.accountType == TYPE_LOCAL) {
                    account.name = context.getString(R.string.lists)
                }
                filters[account] = if (account.isCollapsed) {
                    emptyList()
                } else {
                    caldavDao
                            .getCaldavFilters(account.uuid!!, DateUtilities.now())
                            .map(CaldavFilters::toCaldavFilter)
                            .sortedWith(AlphanumComparator.FILTER)
                }
            }
            return filters.entries
        }

    private fun getSubmenu(title: Int, prefId: Int, getFilters: () -> List<Filter>): List<FilterListItem> {
        val collapsed = preferences.getBoolean(prefId, false)
        val subheader = NavigationDrawerSubheader(context.getString(title), false, collapsed, SubheaderType.PREFERENCE, prefId.toLong())
        return listOf(subheader).plus(if (collapsed) emptyList() else getFilters.invoke())
    }

    private fun getSubmenu(
            title: String?,
            error: Boolean,
            filters: List<Filter>,
            hideIfEmpty: Boolean,
            collapsed: Boolean,
            type: SubheaderType,
            id: Long): List<FilterListItem> {
        return if (hideIfEmpty && filters.isEmpty() && !collapsed) {
            listOf()
        } else {
            listOf(NavigationDrawerSubheader(title, error, collapsed, type, id)).plus(filters)
        }
    }
}