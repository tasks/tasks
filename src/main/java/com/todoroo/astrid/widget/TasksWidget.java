/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.astrid.api.Filter;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.widget.ScrollableWidgetUpdateService;

import javax.inject.Inject;

import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.todoroo.astrid.api.AstridApiConstants.BROADCAST_EVENT_REFRESH;
import static org.tasks.intents.TaskIntents.getEditTaskIntent;

public class TasksWidget extends InjectingAppWidgetProvider {

    private static int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP;

    @Inject Broadcaster broadcaster;
    @Inject Preferences preferences;
    @Inject DefaultFilterProvider defaultFilterProvider;

    public static final String COMPLETE_TASK = "COMPLETE_TASK";
    public static final String EDIT_TASK = "EDIT_TASK";

    public static final String EXTRA_FILTER_ID = "extra_filter_id";
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
                String filterId = intent.getStringExtra(EXTRA_FILTER_ID);
                Intent editTaskIntent = getEditTaskIntent(context, filterId, taskId);
                editTaskIntent.setFlags(flags);
                context.startActivity(editTaskIntent);
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
        String filterId = preferences.getStringValue(WidgetConfigActivity.PREF_WIDGET_ID + id);
        Intent rvIntent = new Intent(context, ScrollableWidgetUpdateService.class);
        rvIntent.putExtra(ScrollableWidgetUpdateService.FILTER_ID, filterId);
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
        Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
        remoteViews.setTextViewText(R.id.widget_title, filter.listingTitle);
        remoteViews.setRemoteAdapter(R.id.list_view, rvIntent);
        remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);
        remoteViews.setOnClickPendingIntent(R.id.widget_title, getOpenListIntent(context, filterId, id));
        remoteViews.setOnClickPendingIntent(R.id.widget_button, getNewTaskIntent(context, filterId, id));
        remoteViews.setPendingIntentTemplate(R.id.list_view, getPendingIntentTemplate(context));
        return remoteViews;
    }

    private PendingIntent getPendingIntentTemplate(Context context) {
        Intent intent = new Intent(context, TasksWidget.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getOpenListIntent(Context context, String filterId, int widgetId) {
        Intent intent = TaskIntents.getTaskListByIdIntent(context, filterId);
        intent.setFlags(flags);
        return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getNewTaskIntent(Context context, String filterId, int widgetId) {
        Intent intent = TaskIntents.getNewTaskIntent(context, filterId);
        intent.setFlags(flags);
        return PendingIntent.getActivity(context, -widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
