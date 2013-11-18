package org.tasks.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget;
import com.todoroo.astrid.widget.WidgetConfigActivity;
import com.todoroo.astrid.widget.WidgetUpdateService;

import org.tasks.R;

import static android.content.Intent.*;

public class WidgetHelper {

    static {
        AstridDependencyInjector.initialize();
    }

    private static int flags = FLAG_ACTIVITY_NEW_TASK
            | FLAG_ACTIVITY_MULTIPLE_TASK
            | FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
            | FLAG_ACTIVITY_NO_HISTORY
            | FLAG_ACTIVITY_PREVIOUS_IS_TOP;


    public static void startWidgetService(Context context) {
        Class widgetServiceClass = android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH
                ? WidgetUpdateService.class
                : ScrollableWidgetUpdateService.class;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, widgetServiceClass);
        PendingIntent pendingIntent = PendingIntent.getService(context,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.setInexactRepeating(AlarmManager.RTC, 0,
                Constants.WIDGET_UPDATE_INTERVAL, pendingIntent);
    }

    public static void triggerUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null) {
            return;
        }
        ComponentName thisWidget = new ComponentName(context, TasksWidget.class);
        Intent intent = new Intent(context, TasksWidget.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(thisWidget));
        context.sendBroadcast(intent);
    }

    @Autowired
    TagDataService tagDataService;

    public WidgetHelper() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public RemoteViews createScrollableWidget(Context context, int id) {
        Filter filter = getFilter(context, id);
        Intent rvIntent = new Intent(context, ScrollableWidgetUpdateService.class);
        rvIntent.putExtra(ScrollableWidgetUpdateService.QUERY_ID, getQuery(context, filter, id));
        rvIntent.putExtra(ScrollableWidgetUpdateService.IS_DARK_THEME, ThemeService.isDarkWidgetTheme());
        rvIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        rvIntent.setData(Uri.parse(rvIntent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), ThemeService.isDarkWidgetTheme() ? R.layout.scrollable_widget_dark : R.layout.scrollable_widget_light);
        remoteViews.setTextViewText(R.id.widget_title, filter.title);
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
        String customIntent = Preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_INTENT
                + widgetId);
        if (customIntent != null) {
            String serializedExtras = Preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_EXTRAS
                    + widgetId);
            Bundle extras = AndroidUtilities.bundleFromSerializedString(serializedExtras);
            listIntent.putExtras(extras);
        }
        listIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_WIDGET);
        listIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
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
        Intent intent;
        if (AstridPreferences.useTabletLayout(context)) {
            intent = new Intent(context, TaskListActivity.class);
            if (filter != null && filter instanceof FilterWithCustomIntent) {
                Bundle customExtras = ((FilterWithCustomIntent) filter).customExtras;
                intent.putExtras(customExtras);
            }
        } else {
            intent = new Intent(context, TaskEditActivity.class);
        }
        intent.setFlags(flags);
        intent.putExtra(TaskEditFragment.OVERRIDE_FINISH_ANIM, false);
        return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public PendingIntent getNewTaskIntent(Context context, Filter filter, int id) {
        Intent intent;
        boolean tablet = AstridPreferences.useTabletLayout(context);
        if (tablet) {
            intent = new Intent(context, TaskListActivity.class);
            intent.putExtra(TaskListActivity.OPEN_TASK, 0L);
        } else {
            intent = new Intent(context, TaskEditActivity.class);
        }

        intent.setFlags(flags);
        intent.putExtra(TaskEditFragment.OVERRIDE_FINISH_ANIM, false);
        if (filter != null) {
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            if (filter.valuesForNewTasks != null) {
                String values = AndroidUtilities.contentValuesToSerializedString(filter.valuesForNewTasks);
                values = PermaSql.replacePlaceholders(values);
                intent.putExtra(TaskEditFragment.TOKEN_VALUES, values);
                intent.setAction("E" + id + values);
            }
            if (tablet) {
                if (filter instanceof FilterWithCustomIntent) {
                    Bundle customExtras = ((FilterWithCustomIntent) filter).customExtras;
                    intent.putExtras(customExtras);
                }
            }
        } else {
            intent.setAction("E" + id);
        }

        return PendingIntent.getActivity(context, -id, intent, 0);
    }

    public Filter getFilter(Context context, int widgetId) {

        // base our filter off the inbox filter, replace stuff if we have it
        Filter filter = CoreFilterExposer.buildInboxFilter(context.getResources());
        String sql = Preferences.getStringValue(WidgetConfigActivity.PREF_SQL + widgetId);
        if (sql != null) {
            filter.setSqlQuery(sql);
        }
        String title = Preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);
        if (title != null) {
            filter.title = title;
        }
        String contentValues = Preferences.getStringValue(WidgetConfigActivity.PREF_VALUES + widgetId);
        if (contentValues != null) {
            filter.valuesForNewTasks = AndroidUtilities.contentValuesFromSerializedString(contentValues);
        }

        String customComponent = Preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_INTENT
                + widgetId);
        if (customComponent != null) {
            ComponentName component = ComponentName.unflattenFromString(customComponent);
            filter = new FilterWithCustomIntent(filter.title, filter.title, filter.getSqlQuery(), filter.valuesForNewTasks);
            ((FilterWithCustomIntent) filter).customTaskList = component;
            String serializedExtras = Preferences.getStringValue(WidgetConfigActivity.PREF_CUSTOM_EXTRAS
                    + widgetId);
            ((FilterWithCustomIntent) filter).customExtras = AndroidUtilities.bundleFromSerializedString(serializedExtras);
        }

        // Validate tagData
        long id = Preferences.getLong(WidgetConfigActivity.PREF_TAG_ID + widgetId, 0);
        TagData tagData;
        if (id > 0) {
            tagData = tagDataService.fetchById(id, TagData.ID, TagData.NAME, TagData.TASK_COUNT, TagData.UUID, TagData.PICTURE, TagData.USER_ID, TagData.MEMBER_COUNT);
            if (tagData != null && !tagData.getValue(TagData.NAME).equals(filter.title)) { // Tag has been renamed; rebuild filter
                filter = TagFilterExposer.filterFromTagData(context, tagData);
                Preferences.setString(WidgetConfigActivity.PREF_SQL + widgetId, filter.getSqlQuery());
                Preferences.setString(WidgetConfigActivity.PREF_TITLE + widgetId, filter.title);
                ContentValues newTaskValues = filter.valuesForNewTasks;
                String contentValuesString = null;
                if (newTaskValues != null) {
                    contentValuesString = AndroidUtilities.contentValuesToSerializedString(newTaskValues);
                }
                Preferences.setString(WidgetConfigActivity.PREF_VALUES + widgetId, contentValuesString);
                if (filter != null) {
                    String flattenedExtras = AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent) filter).customExtras);
                    if (flattenedExtras != null) {
                        Preferences.setString(WidgetConfigActivity.PREF_CUSTOM_EXTRAS + widgetId,
                                flattenedExtras);
                    }
                }
            }
        } else {
            tagData = tagDataService.getTagByName(filter.title, TagData.ID);
            if (tagData != null) {
                Preferences.setLong(WidgetConfigActivity.PREF_TAG_ID + widgetId, tagData.getId());
            }
        }

        return filter;
    }

    private String getQuery(Context context, Filter filter, int widgetId) {
        if (SubtasksHelper.isTagFilter(filter)) {
            ((FilterWithCustomIntent) filter).customTaskList = new ComponentName(context, TagViewFragment.class); // In case legacy widget was created with subtasks fragment
        }

        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
        int flags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
        int sort = publicPrefs.getInt(SortHelper.PREF_SORT_SORT, 0);

        String query = SortHelper.adjustQueryForFlagsAndSort(
                filter.getSqlQuery(), flags, sort).replaceAll("LIMIT \\d+", "");

        String tagName = Preferences.getStringValue(WidgetConfigActivity.PREF_TITLE + widgetId);

        return SubtasksHelper.applySubtasksToWidgetFilter(filter, query, tagName, 0);
    }
}
