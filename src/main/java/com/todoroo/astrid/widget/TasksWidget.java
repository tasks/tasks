/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagFilterExposer;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;
import org.tasks.widget.ScrollableWidgetUpdateService;

import javax.inject.Inject;

import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.todoroo.astrid.api.AstridApiConstants.BROADCAST_EVENT_REFRESH;
import static org.tasks.intents.TaskIntents.getEditTaskStack;

public class TasksWidget extends InjectingAppWidgetProvider {

    private static int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP;

    @Inject Broadcaster broadcaster;
    @Inject TagDataDao tagDataDao;
    @Inject Preferences preferences;

    public static final String COMPLETE_TASK = "COMPLETE_TASK";
    public static final String EDIT_TASK = "EDIT_TASK";

    public static final String EXTRA_FILTER = "extra_filter";
    public static final String EXTRA_ID = "extra_id"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        switch(intent.getAction()) {
            case COMPLETE_TASK:
                broadcaster.toggleCompletedState(intent.getLongExtra(EXTRA_ID, 0));
                break;
            case EDIT_TASK:
                long taskId = intent.getLongExtra(EXTRA_ID, 0);
                Filter filter = intent.getParcelableExtra(EXTRA_FILTER);
                getEditTaskStack(context, filter, taskId).startActivities();
                break;
            case BROADCAST_EVENT_REFRESH:
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidget.class));
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view);
                break;
        }
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            super.onUpdate(context, appWidgetManager, appWidgetIds);

            ComponentName thisWidget = new ComponentName(context, TasksWidget.class);
            int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int id : ids) {
                appWidgetManager.updateAppWidget(id, createScrollableWidget(context, id));
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }

    private RemoteViews createScrollableWidget(Context context, int id) {
        Filter filter = getFilter(context, id);
        Intent rvIntent = new Intent(context, ScrollableWidgetUpdateService.class);
        Bundle filterBundle = new Bundle(com.todoroo.astrid.api.Filter.class.getClassLoader());
        filterBundle.putParcelable(ScrollableWidgetUpdateService.FILTER, filter);
        rvIntent.putExtra(ScrollableWidgetUpdateService.FILTER, filterBundle);
        rvIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        rvIntent.setData(Uri.parse(rvIntent.toUri(Intent.URI_INTENT_SCHEME)));
        boolean darkTheme = preferences.useDarkWidgetTheme(id);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), darkTheme ? R.layout.scrollable_widget_dark : R.layout.scrollable_widget_light);
        if (preferences.getBoolean(WidgetConfigActivity.PREF_HIDE_HEADER + id, false)) {
            remoteViews.setViewVisibility(R.id.widget_header, View.GONE);
        }
        if (preferences.getBoolean(WidgetConfigActivity.PREF_WIDGET_TRANSPARENT + id, false)) {
            remoteViews.setInt(R.id.widget_header, "setBackgroundColor", android.R.color.transparent);
            remoteViews.setInt(R.id.list_view, "setBackgroundColor", android.R.color.transparent);
            remoteViews.setInt(R.id.empty_view, "setBackgroundColor", android.R.color.transparent);
        }
        remoteViews.setTextViewText(R.id.widget_title, filter.listingTitle);
        remoteViews.setRemoteAdapter(R.id.list_view, rvIntent);
        remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);
        remoteViews.setOnClickPendingIntent(R.id.widget_title, getOpenListIntent(context, filter, id));
        remoteViews.setOnClickPendingIntent(R.id.widget_button, getNewTaskIntent(context, filter, id));
        remoteViews.setPendingIntentTemplate(R.id.list_view, getPendingIntentTemplate(context));
        return remoteViews;
    }

    private PendingIntent getPendingIntentTemplate(Context context) {
        Intent intent = new Intent(context, TasksWidget.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getOpenListIntent(Context context, Filter filter, int widgetId) {
        Intent intent = TaskIntents.getTaskListIntent(context, filter);
        intent.setFlags(flags);
        return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getNewTaskIntent(Context context, Filter filter, int widgetId) {
        Intent intent = TaskIntents.getNewTaskIntent(context, filter);
        intent.setFlags(flags);
        return PendingIntent.getActivity(context, -widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Filter getFilter(Context context, int widgetId) {
        // base our filter off the inbox filter, replace stuff if we have it
        Filter filter = BuiltInFilterExposer.getMyTasksFilter(context.getResources());
        String sql = preferences.getStringValue(WidgetConfigActivity.PREF_SQL + widgetId);
        if (sql != null) {
            sql = sql.replace("tasks.userId=0", "1"); // TODO: replace dirty hack for missing column
            filter.setSqlQuery(sql);
        }
        String title = preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);
        if (title != null) {
            filter.listingTitle = title;
        }
        String contentValues = preferences.getStringValue(WidgetConfigActivity.PREF_VALUES + widgetId);
        if (contentValues != null) {
            filter.valuesForNewTasks = AndroidUtilities.contentValuesFromSerializedString(contentValues);
        }

        String customComponent = preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_INTENT
                + widgetId);
        if (customComponent != null) {
            ComponentName component = ComponentName.unflattenFromString(customComponent);
            filter = new FilterWithCustomIntent(filter.listingTitle, filter.getSqlQuery(), filter.valuesForNewTasks);
            ((FilterWithCustomIntent) filter).customTaskList = component;
            String serializedExtras = preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_EXTRAS
                    + widgetId);
            ((FilterWithCustomIntent) filter).customExtras = AndroidUtilities.bundleFromSerializedString(serializedExtras);
        }

        // Validate tagData
        long id = preferences.getLong(WidgetConfigActivity.PREF_TAG_ID + widgetId, 0);
        TagData tagData;
        if (id > 0) {
            tagData = tagDataDao.fetch(id, TagData.ID, TagData.NAME, TagData.UUID);
            if (tagData != null && !tagData.getName().equals(filter.listingTitle)) { // Tag has been renamed; rebuild filter
                filter = TagFilterExposer.filterFromTagData(context, tagData);
                preferences.setString(WidgetConfigActivity.PREF_SQL + widgetId, filter.getSqlQuery());
                preferences.setString(WidgetConfigActivity.PREF_TITLE + widgetId, filter.listingTitle);
                ContentValues newTaskValues = filter.valuesForNewTasks;
                String contentValuesString = null;
                if (newTaskValues != null) {
                    contentValuesString = AndroidUtilities.contentValuesToSerializedString(newTaskValues);
                }
                preferences.setString(WidgetConfigActivity.PREF_VALUES + widgetId, contentValuesString);
                String flattenedExtras = AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent) filter).customExtras);
                if (flattenedExtras != null) {
                    preferences.setString(WidgetConfigActivity.PREF_CUSTOM_EXTRAS + widgetId,
                            flattenedExtras);
                }
            }
        } else {
            tagData = tagDataDao.getTagByName(filter.listingTitle, TagData.ID);
            if (tagData != null) {
                preferences.setLong(WidgetConfigActivity.PREF_TAG_ID + widgetId, tagData.getId());
            }
        }

        return filter;
    }
}
