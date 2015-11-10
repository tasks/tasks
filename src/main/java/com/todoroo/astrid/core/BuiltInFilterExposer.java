/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.ResourceResolver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class BuiltInFilterExposer {

    private final Preferences preferences;
    private ResourceResolver resourceResolver;
    private final Context context;

    @Inject
    public BuiltInFilterExposer(ResourceResolver resourceResolver, @ForApplication Context context, Preferences preferences) {
        this.resourceResolver = resourceResolver;
        this.context = context;
        this.preferences = preferences;
    }

    public Filter getMyTasksFilter() {
        Filter myTasksFilter = getMyTasksFilter(context.getResources());
        myTasksFilter.icon = resourceResolver.getResource(R.attr.ic_action_inbox);
        return myTasksFilter;
    }

    public List<Filter> getFilters() {
        Resources r = context.getResources();
        // core filters
        List<Filter> filters = new ArrayList<>();

        if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
            Filter todayFilter = getTodayFilter(r);
            todayFilter.icon = resourceResolver.getResource(R.attr.ic_action_calendar_today);
            filters.add(todayFilter);
        }
        if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
            Filter recentlyModifiedFilter = getRecentlyModifiedFilter(r);
            recentlyModifiedFilter.icon = resourceResolver.getResource(R.attr.ic_action_history);
            filters.add(recentlyModifiedFilter);
        }
        if (preferences.getBoolean(R.string.p_show_not_in_list_filter, true)) {
            Filter uncategorizedFilter = getUncategorizedFilter(r);
            uncategorizedFilter.icon = resourceResolver.getResource(R.attr.ic_action_uncategorized);
            filters.add(uncategorizedFilter);
        }
        // transmit filter list
        return filters;
    }

    /**
     * Build inbox filter
     */
    public static Filter getMyTasksFilter(Resources r) {
        return new Filter(r.getString(R.string.BFE_Active),
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeAndVisible(),
                                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(
                                        Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                                                TaskToTagMetadata.TAG_NAME.like("x_%", "x"))))))), //$NON-NLS-1$ //$NON-NLS-2$
                null);
    }

    private static Filter getTodayFilter(Resources r) {
        String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
        ContentValues todayValues = new ContentValues();
        todayValues.put(Task.DUE_DATE.name, PermaSql.VALUE_NOON);
        return new Filter(todayTitle,
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeAndVisible(),
                                Task.DUE_DATE.gt(0),
                                Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
                todayValues);
    }

    private static Filter getRecentlyModifiedFilter(Resources r) {
        return new Filter(r.getString(R.string.BFE_Recent),
                new QueryTemplate().where(
                        Criterion.all).orderBy(
                        Order.desc(Task.MODIFICATION_DATE)).limit(15),
                null);
    }

    private static Filter getTimeRemainingFilter(Resources r)
    {
        String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
        ContentValues todayValues = new ContentValues();
        todayValues.put(Task.ESTIMATED_SECONDS.name, PermaSql.Value_EOD_Hours);
        return new Filter(todayTitle,
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeAndVisible(),
                                Task.DUE_DATE.gt(0),
                                Task.DUE_DATE.lte(PermaSql.Value_EOD_Hours))),
                todayValues);

    }
    private static Filter getUncategorizedFilter(Resources r) {
        return new Filter(r.getString(R.string.tag_FEx_untagged),
                new QueryTemplate().where(Criterion.and(
                        Criterion.not(Task.UUID.in(Query.select(TaskToTagMetadata.TASK_UUID).from(Metadata.TABLE)
                                .where(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0))))),
                        TaskCriteria.isActive(),
                        TaskCriteria.isVisible())),
                null);
    }

    /**
     * Is this the inbox?
     */
    public static boolean isInbox(Context context, Filter filter) {
        return (filter != null && filter.equals(getMyTasksFilter(context.getResources())));
    }

    public static boolean isTodayFilter(Context context, Filter filter) {
        return (filter != null && filter.equals(getTodayFilter(context.getResources())));
    }
}
