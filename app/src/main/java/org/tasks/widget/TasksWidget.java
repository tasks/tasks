package org.tasks.widget;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static org.tasks.intents.TaskIntents.getEditTaskIntent;

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
import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.intents.TaskIntents;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.themes.WidgetTheme;
import timber.log.Timber;

public class TasksWidget extends InjectingAppWidgetProvider {

  public static final String COMPLETE_TASK = "COMPLETE_TASK";
  public static final String EDIT_TASK = "EDIT_TASK";
  public static final String EXTRA_FILTER_ID = "extra_filter_id";
  public static final String EXTRA_ID = "extra_id"; // $NON-NLS-1$
  private static final int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP;
  @Inject Preferences preferences;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject ThemeCache themeCache;
  @Inject Locale locale;
  @Inject @ForApplication Context context;

  private static Bitmap getSolidBackground(int bgColor) {
    Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); // Create a Bitmap
    new Canvas(bitmap).drawColor(bgColor); // Set the color
    return bitmap;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    String action = intent.getAction();

    if (Strings.isNullOrEmpty(action)) {
      return;
    }

    switch (action) {
      case COMPLETE_TASK:
        Intent completionIntent = new Intent(context, CompleteTaskReceiver.class);
        completionIntent.putExtra(CompleteTaskReceiver.TASK_ID, intent.getLongExtra(EXTRA_ID, 0));
        completionIntent.putExtra(CompleteTaskReceiver.TOGGLE_STATE, true);
        context.sendBroadcast(completionIntent);
        break;
      case EDIT_TASK:
        long taskId = intent.getLongExtra(EXTRA_ID, 0);
        String filterId = intent.getStringExtra(EXTRA_FILTER_ID);
        Intent editTaskIntent = getEditTaskIntent(context, filterId, taskId);
        editTaskIntent.setFlags(flags);
        context.startActivity(editTaskIntent);
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
      Timber.e(e);
    }
  }

  private RemoteViews createScrollableWidget(Context context, int id) {
    WidgetPreferences widgetPreferences = new WidgetPreferences(context, preferences, id);
    String filterId = widgetPreferences.getFilterId();
    Intent rvIntent = new Intent(context, ScrollableWidgetUpdateService.class);
    rvIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
    rvIntent.setData(Uri.parse(rvIntent.toUri(Intent.URI_INTENT_SCHEME)));
    WidgetTheme theme = themeCache.getWidgetTheme(widgetPreferences.getThemeIndex());
    ThemeColor color = themeCache.getThemeColor(widgetPreferences.getColorIndex());
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
    if (atLeastJellybeanMR1()) {
      remoteViews.setInt(R.id.widget, "setLayoutDirection", locale.getDirectionality());
    }
    if (widgetPreferences.showHeader()) {
      remoteViews.setViewVisibility(R.id.widget_header, View.VISIBLE);
      remoteViews.setViewVisibility(
          R.id.widget_reconfigure, widgetPreferences.showSettings() ? View.VISIBLE : View.GONE);
      remoteViews.setInt(R.id.widget_title, "setTextColor", color.getActionBarTint());
      remoteViews.setInt(R.id.widget_button, "setColorFilter", color.getActionBarTint());
      remoteViews.setInt(R.id.widget_reconfigure, "setColorFilter", color.getActionBarTint());
    } else {
      remoteViews.setViewVisibility(R.id.widget_header, View.GONE);
    }
    int opacityPercentage = widgetPreferences.getOpacity();
    int opacity = (int) ((opacityPercentage / 100.0) * 255.0);
    remoteViews.setImageViewBitmap(
        R.id.widget_background, getSolidBackground(theme.getBackgroundColor()));
    remoteViews.setImageViewBitmap(
        R.id.widget_header_background, getSolidBackground(color.getPrimaryColor()));
    remoteViews.setInt(R.id.widget_background, "setAlpha", opacity);
    remoteViews.setInt(R.id.widget_header_background, "setAlpha", opacity);

    Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
    remoteViews.setTextViewText(R.id.widget_title, filter.listingTitle);
    remoteViews.setRemoteAdapter(R.id.list_view, rvIntent);
    remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);
    remoteViews.setOnClickPendingIntent(
        R.id.widget_title, getOpenListIntent(context, filterId, id));
    remoteViews.setOnClickPendingIntent(
        R.id.widget_button, getNewTaskIntent(context, filterId, id));
    remoteViews.setOnClickPendingIntent(
        R.id.widget_reconfigure, getWidgetConfigIntent(context, id));
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
    intent.setAction("open_list");
    return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent getNewTaskIntent(Context context, String filterId, int widgetId) {
    Intent intent = TaskIntents.getNewTaskIntent(context, filterId);
    intent.setFlags(flags);
    intent.setAction("new_task");
    return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent getWidgetConfigIntent(Context context, final int widgetId) {
    Intent intent = new Intent(context, WidgetConfigActivity.class);
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    intent.setAction("widget_settings");
    return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
