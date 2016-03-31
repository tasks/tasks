package org.tasks.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import org.tasks.preferences.Theme;
import org.tasks.preferences.ThemeManager;

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
    @Inject ThemeManager themeManager;

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
        Theme theme = themeManager.getWidgetTheme(id);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
        if (preferences.getBoolean(WidgetConfigActivity.PREF_HIDE_HEADER + id, false)) {
            remoteViews.setViewVisibility(R.id.widget_header, View.GONE);
        }
        int opacity = preferences.getInt(WidgetConfigActivity.PREF_WIDGET_OPACITY + id, WidgetConfigActivity.DEFAULT_OPACITY);
        remoteViews.setImageViewBitmap(R.id.widget_background,
                getSolidBackground(theme.getContentBackground()));
        remoteViews.setImageViewBitmap(R.id.widget_header_background,
                getSolidBackground(theme.getPrimaryColor()));
        if (opacity < 100) {
            remoteViews.setInt(R.id.widget_background, "setAlpha", opacity);
            remoteViews.setInt(R.id.widget_header_background, "setAlpha", opacity);
        }
        if (!theme.isDark()) {
            remoteViews.setInt(R.id.widget_header_separator, "setVisibility", View.GONE);
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

    private static Bitmap getSolidBackground(int bgColor) {
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); // Create a Bitmap
        new Canvas(bitmap).drawColor(bgColor); //Set the color
        return bitmap;
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
