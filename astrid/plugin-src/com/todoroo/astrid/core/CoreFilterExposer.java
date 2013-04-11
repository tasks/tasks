/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.WaitingOnMeFragment;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.WaitingOnMe;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

/**
 * Exposes Astrid's built in filters to the {@link FilterListFragment}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CoreFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();
        ContextManager.setContext(context);

        FilterListItem[] list = prepareFilters(r);
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Resources r) {
        // core filters
        List<FilterListItem> filters = new ArrayList<FilterListItem>(3);

        filters.add(buildInboxFilter(r));
        if (Preferences.getBoolean(R.string.p_show_today_filter, true))
            filters.add(getTodayFilter(r));

        if (Preferences.getBoolean(R.string.p_show_waiting_on_me_filter, true) &&
                PluginServices.getWaitingOnMeDao().count(Query.select(WaitingOnMe.ID).where(Criterion.and(WaitingOnMe.DELETED_AT.eq(0),
                        Criterion.or(WaitingOnMe.ACKNOWLEDGED.isNull(), WaitingOnMe.ACKNOWLEDGED.neq(1))))) > 0)
            filters.add(getWaitingOnMeFilter(r));

        // transmit filter list
        return filters.toArray(new FilterListItem[filters.size()]);
    }

    /**
     * Build inbox filter
     * @return
     */
    public static Filter buildInboxFilter(Resources r) {
        Filter inbox = new Filter(r.getString(R.string.BFE_Active), r.getString(R.string.BFE_Active),
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeVisibleMine(),
                                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(
                                        Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                                                TaskToTagMetadata.TAG_NAME.like("x_%", "x"))))))), //$NON-NLS-1$ //$NON-NLS-2$
                                                null);
        int themeFlags = ThemeService.getFilterThemeFlags();
        inbox.listingIcon = ((BitmapDrawable)r.getDrawable(
                ThemeService.getDrawable(R.drawable.filter_inbox, themeFlags))).getBitmap();
        return inbox;
    }

    public static Filter getTodayFilter(Resources r) {
        int themeFlags = ThemeService.getFilterThemeFlags();
        String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
        ContentValues todayValues = new ContentValues();
        todayValues.put(Task.DUE_DATE.name, PermaSql.VALUE_NOON);
        Filter todayFilter = new Filter(todayTitle,
                todayTitle,
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeVisibleMine(),
                                Task.DUE_DATE.gt(0),
                                Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
                                todayValues);
        todayFilter.listingIcon = ((BitmapDrawable)r.getDrawable(
                ThemeService.getDrawable(R.drawable.filter_calendar, themeFlags))).getBitmap();
        return todayFilter;
    }

    public static Filter getWaitingOnMeFilter(Resources r) {
         FilterWithCustomIntent waitingOnMe = new FilterWithCustomIntent(r.getString(R.string.BFE_waiting_on_me), r.getString(R.string.BFE_waiting_on_me),
                 new QueryTemplate().join(Join.inner(WaitingOnMe.TABLE, Task.UUID.eq(WaitingOnMe.TASK_UUID))).where(
                         Criterion.and(WaitingOnMe.DELETED_AT.eq(0),
                                 Criterion.or(WaitingOnMe.ACKNOWLEDGED.isNull(), WaitingOnMe.ACKNOWLEDGED.neq(1))))
                                 .groupBy(Task.UUID), null);
         waitingOnMe.customTaskList = new ComponentName(ContextManager.getContext(), WaitingOnMeFragment.class);
         int themeFlags = ThemeService.getFilterThemeFlags();
         waitingOnMe.listingIcon = ((BitmapDrawable) r.getDrawable(
                 ThemeService.getDrawable(R.drawable.waiting_on_me, themeFlags))).getBitmap();
         return waitingOnMe;

    }

    /**
     * Is this the inbox?
     * @param filter
     * @return
     */
    public static boolean isInbox(Filter filter) {
        return (filter != null && filter.equals(buildInboxFilter(ContextManager.getContext().getResources())));
    }

    public static boolean isTodayFilter(Filter filter) {
        return (filter != null && filter.equals(getTodayFilter(ContextManager.getContext().getResources())));
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null || ContextManager.getContext().getResources() == null)
            return null;

        Resources r = ContextManager.getContext().getResources();
        return prepareFilters(r);
    }

}
