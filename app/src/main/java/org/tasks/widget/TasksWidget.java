package org.tasks.widget;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.ApplicationContext;
import org.tasks.injection.InjectingAppWidgetProvider;
import org.tasks.intents.TaskIntents;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeColor;
import timber.log.Timber;

public class TasksWidget extends InjectingAppWidgetProvider {

  private static final int flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP;
  @Inject Preferences preferences;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject Locale locale;
  @Inject TaskDao taskDao;
  @Inject @ApplicationContext Context context;

  @Override
  protected void inject(BroadcastComponent component) {
    component.inject(this);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    for (int id : appWidgetIds) {
      try {
        appWidgetManager.updateAppWidget(id, createScrollableWidget(context, id));
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }

  private RemoteViews createScrollableWidget(Context context, int id) {
    WidgetPreferences widgetPreferences = new WidgetPreferences(context, preferences, id);
    String filterId = widgetPreferences.getFilterId();
    ThemeColor color = new ThemeColor(context, widgetPreferences.getColor());
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
    remoteViews.setInt(R.id.widget, "setLayoutDirection", locale.getDirectionality());
    if (widgetPreferences.showHeader()) {
      remoteViews.setViewVisibility(R.id.widget_header, View.VISIBLE);
      remoteViews.setViewVisibility(
          R.id.widget_change_list, widgetPreferences.showMenu() ? View.VISIBLE : View.GONE);
      int widgetTitlePadding =
          widgetPreferences.showMenu()
              ? 0
              : (int) context.getResources().getDimension(R.dimen.widget_padding);
      remoteViews.setViewPadding(R.id.widget_title, widgetTitlePadding, 0, 0, 0);
      remoteViews.setViewVisibility(
          R.id.widget_reconfigure, widgetPreferences.showSettings() ? View.VISIBLE : View.GONE);
      remoteViews.setInt(R.id.widget_title, "setTextColor", color.getColorOnPrimary());
      remoteViews.setInt(R.id.widget_button, "setColorFilter", color.getColorOnPrimary());
      remoteViews.setInt(R.id.widget_reconfigure, "setColorFilter", color.getColorOnPrimary());
      remoteViews.setInt(R.id.widget_change_list, "setColorFilter", color.getColorOnPrimary());
    } else {
      remoteViews.setViewVisibility(R.id.widget_header, View.GONE);
    }

    remoteViews.setInt(
        R.id.widget_header,
        "setBackgroundColor",
        ColorUtils.setAlphaComponent(color.getPrimaryColor(), widgetPreferences.getHeaderOpacity()));
    int bgColor = getBackgroundColor(widgetPreferences.getThemeIndex());
    remoteViews.setInt(
        R.id.list_view,
        "setBackgroundColor",
        ColorUtils.setAlphaComponent(bgColor, widgetPreferences.getRowOpacity()));
    remoteViews.setInt(
        R.id.empty_view,
        "setBackgroundColor",
        ColorUtils.setAlphaComponent(bgColor, widgetPreferences.getFooterOpacity()));

    Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
    remoteViews.setTextViewText(R.id.widget_title, filter.listingTitle);

    Uri cacheBuster = Uri.parse("tasks://widget/" + System.currentTimeMillis());
    remoteViews.setRemoteAdapter(
        R.id.list_view,
        new Intent(context, ScrollableWidgetUpdateService.class)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .setData(cacheBuster));

    setRipple(
        remoteViews, color, R.id.widget_button, R.id.widget_change_list, R.id.widget_reconfigure);
    remoteViews.setOnClickPendingIntent(R.id.widget_title, getOpenListIntent(context, filter, id));
    remoteViews.setOnClickPendingIntent(R.id.widget_button, getNewTaskIntent(context, filter, id));
    remoteViews.setOnClickPendingIntent(R.id.widget_change_list, getChooseListIntent(context, filter, id));
    remoteViews.setOnClickPendingIntent(
        R.id.widget_reconfigure, getWidgetConfigIntent(context, id));
    if (widgetPreferences.openOnFooterClick()) {
      remoteViews.setOnClickPendingIntent(R.id.empty_view, getOpenListIntent(context, filter, id));
    } else {
      remoteViews.setOnClickPendingIntent(R.id.empty_view, null);
    }
    remoteViews.setPendingIntentTemplate(R.id.list_view, getPendingIntentTemplate(context));
    return remoteViews;
  }

  private void setRipple(RemoteViews rv, ThemeColor color, int... views) {
    int drawableRes =
        color.isDark()
            ? R.drawable.widget_ripple_circle_light
            : R.drawable.widget_ripple_circle_dark;
    for (int view : views) {
      rv.setInt(view, "setBackgroundResource", drawableRes);
    }
  }

  private @ColorInt int getBackgroundColor(int themeIndex) {
    int background;
    if (themeIndex == 1) {
      background = android.R.color.black;
    } else if (themeIndex == 2) {
      background = R.color.md_background_dark;
    } else {
      background = android.R.color.white;
    }
    return context.getColor(background);
  }

  private PendingIntent getPendingIntentTemplate(Context context) {
    return PendingIntent.getActivity(
        context, 0, new Intent(context, WidgetClickActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent getOpenListIntent(Context context, Filter filter, int widgetId) {
    Intent intent = TaskIntents.getTaskListIntent(context, filter);
    intent.setFlags(flags);
    intent.setAction("open_list");
    return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent getNewTaskIntent(Context context, Filter filter, int widgetId) {
    Intent intent = TaskIntents.getNewTaskIntent(context, filter);
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

  private PendingIntent getChooseListIntent(Context context, Filter filter, int widgetId) {
    Intent intent = new Intent(context, FilterSelectionActivity.class);
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, filter);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    intent.setAction("choose_list");
    return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
