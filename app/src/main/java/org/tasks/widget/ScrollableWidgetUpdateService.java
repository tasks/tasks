package org.tasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViewsService;

import com.todoroo.astrid.subtasks.SubtasksHelper;

import org.tasks.LocalBroadcastManager;
import org.tasks.data.TaskDao;
import org.tasks.markdown.MarkdownProvider;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.tasklist.HeaderFormatter;
import org.tasks.themes.ColorProvider;
import org.tasks.ui.CheckBoxProvider;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScrollableWidgetUpdateService extends RemoteViewsService {

  @Inject TaskDao taskDao;
  @Inject Preferences preferences;
  @Inject SubtasksHelper subtasksHelper;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject Locale locale;
  @Inject ChipProvider chipProvider;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject MarkdownProvider markdownProvider;
  @Inject HeaderFormatter headerFormatter;

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);

    stopSelf();
  }

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    if (intent == null) {
      return null;
    }

    Bundle extras = intent.getExtras();
    if (extras == null) {
      return null;
    }

    int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
    Context context = getApplicationContext();
    return new ScrollableViewsFactory(
        subtasksHelper,
        preferences,
        context,
        widgetId,
        taskDao,
        defaultFilterProvider,
        new CheckBoxProvider(context, new ColorProvider(context, preferences)),
        locale,
        chipProvider,
        localBroadcastManager,
        markdownProvider.markdown(false),
        headerFormatter
    );
  }
}
