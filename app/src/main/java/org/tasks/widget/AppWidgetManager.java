package org.tasks.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationContext;

public class AppWidgetManager {

  private final android.appwidget.AppWidgetManager appWidgetManager;
  private final Context context;

  @Inject
  public AppWidgetManager(@ApplicationContext Context context) {
    this.context = context;
    appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context);
  }

  public void updateWidgets() {
    appWidgetManager.notifyAppWidgetViewDataChanged(getWidgetIds(), R.id.list_view);
  }

  public void reconfigureWidgets(int... appWidgetIds) {
    Intent intent = new Intent(context, TasksWidget.class);
    intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    intent.putExtra(
        android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS,
        appWidgetIds.length == 0 ? getWidgetIds() : appWidgetIds);
    context.sendBroadcast(intent);
    updateWidgets();
  }

  public int[] getWidgetIds() {
    return appWidgetManager.getAppWidgetIds(new ComponentName(context, TasksWidget.class));
  }
}
