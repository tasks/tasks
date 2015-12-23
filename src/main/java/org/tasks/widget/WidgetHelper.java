package org.tasks.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.widget.TasksWidget;
import com.todoroo.astrid.widget.WidgetConfigActivity;

import org.tasks.R;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.AlarmManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@Singleton
public class WidgetHelper {

    public static int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP;

    private final TagDataDao tagDataDao;
    private final Preferences preferences;
    private AlarmManager alarmManager;

    @Inject
    public WidgetHelper(TagDataDao tagDataDao, Preferences preferences, AlarmManager alarmManager) {
        this.tagDataDao = tagDataDao;
        this.preferences = preferences;
        this.alarmManager = alarmManager;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public RemoteViews createScrollableWidget(Context context, int id) {
        Intent intent = new Intent(context, ScrollableWidgetUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setInexactRepeating(TimeUnit.MINUTES.toMillis(30), pendingIntent);

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
        PendingIntent listIntent = getListIntent(context, filter, id);
        if (listIntent != null) {
            remoteViews.setOnClickPendingIntent(R.id.widget_title, listIntent);
        }
        PendingIntent newTaskIntent = getNewTaskIntent(context, filter, id);
        if (newTaskIntent != null) {
            remoteViews.setOnClickPendingIntent(R.id.widget_button, newTaskIntent);
        }
        PendingIntent editTaskIntent = getEditTaskIntent(context, filter, id);
        if (editTaskIntent != null) {
            remoteViews.setPendingIntentTemplate(R.id.list_view, editTaskIntent);
        }
        return remoteViews;
    }

    public PendingIntent getListIntent(Context context, Filter filter, int widgetId) {
        Intent listIntent = new Intent(context, TaskListActivity.class);
        String customIntent = preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_INTENT
                + widgetId);
        if (customIntent != null) {
            String serializedExtras = preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_EXTRAS
                    + widgetId);
            Bundle extras = AndroidUtilities.bundleFromSerializedString(serializedExtras);
            listIntent.putExtras(extras);
        }
        listIntent.setFlags(flags);
        if (filter != null) {
            listIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            listIntent.setAction("L" + widgetId + filter.getSqlQuery());
        } else {
            listIntent.setAction("L" + widgetId);
        }
        if (filter instanceof FilterWithCustomIntent) {
            listIntent.putExtras(((FilterWithCustomIntent) filter).customExtras);
        }

        return PendingIntent.getActivity(context, widgetId,
                listIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getEditTaskIntent(Context context, Filter filter, int widgetId) {
        Intent intent = new Intent(context, TasksWidget.class);
        if (context.getResources().getBoolean(R.bool.two_pane_layout)) {
            if (filter != null && filter instanceof FilterWithCustomIntent) {
                Bundle customExtras = ((FilterWithCustomIntent) filter).customExtras;
                intent.putExtras(customExtras);
            }
        }
        return PendingIntent.getBroadcast(context, -widgetId, intent, 0);
    }

    public PendingIntent getNewTaskIntent(Context context, Filter filter, int id) {
        Intent intent = TaskIntents.getNewTaskIntent(context, filter);
        intent.setFlags(flags);
        return PendingIntent.getActivity(context, -id, intent, 0);
    }

    public Filter getFilter(Context context, int widgetId) {

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
                if (filter != null) {
                    String flattenedExtras = AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent) filter).customExtras);
                    if (flattenedExtras != null) {
                        preferences.setString(WidgetConfigActivity.PREF_CUSTOM_EXTRAS + widgetId,
                                flattenedExtras);
                    }
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
