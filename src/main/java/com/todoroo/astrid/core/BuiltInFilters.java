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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class BuiltInFilters {

    private final Filter myTasks;
    private final Filter today;
    private final Filter recentlyModified;
    private final Filter uncategorized;

    @Inject
    public BuiltInFilters(@ForApplication Context context) {
        Resources resources = context.getResources();

        myTasks = getMyTasksFilter(resources);
        myTasks.icon = R.drawable.ic_inbox_24dp;

        today = getTodayFilter(resources);
        today.icon = R.drawable.ic_today_24dp;

        recentlyModified = getRecentlyModifiedFilter(resources);
        recentlyModified.icon = R.drawable.ic_history_24dp;

        uncategorized = getUncategorizedFilter(resources);
        uncategorized.icon = R.drawable.ic_label_outline_24dp;
    }

    public Filter getMyTasks() {
        return myTasks;
    }

    public Filter getToday() {
        return today;
    }

    public Filter getRecentlyModified() {
        return recentlyModified;
    }

    public Filter getUncategorized() {
        return uncategorized;
    }

    public boolean isMyTasksFilter(Filter filter) {
        return myTasks.equals(filter);
    }

    public boolean isTodayFilter(Filter filter) {
        return today.equals(filter);
    }

    private static Filter getMyTasksFilter(Resources r) {
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

    private static Filter getUncategorizedFilter(Resources r) {
        return new Filter(r.getString(R.string.tag_FEx_untagged),
                new QueryTemplate().where(Criterion.and(
                        Criterion.not(Task.UUID.in(Query.select(TaskToTagMetadata.TASK_UUID).from(Metadata.TABLE)
                                .where(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0))))),
                        TaskCriteria.isActive(),
                        TaskCriteria.isVisible())),
                null);
    }
}
