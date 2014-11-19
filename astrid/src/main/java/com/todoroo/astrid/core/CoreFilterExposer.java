/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.injection.Injector;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CoreFilterExposer extends InjectingBroadcastReceiver implements AstridFilterExposer {

    @Inject Preferences preferences;
    @Inject @ForApplication Context context;

    private FilterListItem[] prepareFilters() {
        Resources r = context.getResources();
        // core filters
        List<FilterListItem> filters = new ArrayList<>(3);

        filters.add(buildInboxFilter(r));
        if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
            filters.add(getTodayFilter(r));
        }

        // transmit filter list
        return filters.toArray(new FilterListItem[filters.size()]);
    }

    /**
     * Build inbox filter
     */
    public static Filter buildInboxFilter(Resources r) {
        return new Filter(r.getString(R.string.BFE_Active), r.getString(R.string.BFE_Active),
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeVisibleMine(),
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
                todayTitle,
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeVisibleMine(),
                                Task.DUE_DATE.gt(0),
                                Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
                                todayValues);
    }

    /**
     * Is this the inbox?
     */
    public static boolean isInbox(Filter filter) {
        return (filter != null && filter.equals(buildInboxFilter(ContextManager.getContext().getResources())));
    }

    public static boolean isTodayFilter(Filter filter) {
        return (filter != null && filter.equals(getTodayFilter(ContextManager.getContext().getResources())));
    }

    @Override
    public FilterListItem[] getFilters(Injector injector) {
        injector.inject(this);

        return prepareFilters();
    }

}
